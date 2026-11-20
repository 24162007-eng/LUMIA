
package com.example.lumia.services;

import android.content.Context;
import android.graphics.Rect;
import android.media.Image;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.common.util.concurrent.ListenableFuture;

import com.google.mlkit.vision.common.InputImage;

// Image Labeling
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

// Object Detection
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

// Text Recognition (OCR)
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@ExperimentalGetImage
public class CameraService {

    private final Context context;
    private final ExecutorService cameraExecutor;

    // Ba mô hình AI
    private final ImageLabeler imageLabeler;
    private final ObjectDetector objectDetector;
    private final TextRecognizer textRecognizer;

    private final AtomicBoolean isProcessing =
            new AtomicBoolean(false);

    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis imageAnalysis;
    private Preview preview;

    private OnDetectionResultsListener listener;

    private long lastDetectionTime = 0;
    private String lastResult = "";

    private boolean released = false;

    private static final long DETECTION_INTERVAL = 700;

    // Interface trả kết quả của cả ba mô hình
    public interface OnDetectionResultsListener {

        void onDetectionResults(
                List<String> labels,
                List<String> objects,
                String recognizedText
        );

        void onError(String error);
    }

    // Constructor
    public CameraService(Context context) {

        this.context = context.getApplicationContext();

        cameraExecutor = Executors.newSingleThreadExecutor();

        // 1. Khởi tạo Image Labeling
        imageLabeler = ImageLabeling.getClient(
                ImageLabelerOptions.DEFAULT_OPTIONS
        );

        // 2. Khởi tạo Object Detection
        ObjectDetectorOptions options =
                new ObjectDetectorOptions.Builder()
                        .setDetectorMode(
                                ObjectDetectorOptions.STREAM_MODE
                        )
                        .enableClassification()
                        .build();

        objectDetector = ObjectDetection.getClient(options);

        // 3. Khởi tạo Text Recognition (OCR)
        textRecognizer = TextRecognition.getClient(
                TextRecognizerOptions.DEFAULT_OPTIONS
        );
    }

    // Khởi động camera
    public void startCamera(
            LifecycleOwner lifecycleOwner,
            PreviewView previewView,
            OnDetectionResultsListener listener
    ) {

        if (released) {
            return;
        }

        this.listener = listener;

        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(context);

        future.addListener(() -> {

            if (released || this.listener != listener) {
                return;
            }

            try {

                cameraProvider = future.get();

                // Camera Preview
                preview = new Preview.Builder().build();

                preview.setSurfaceProvider(
                        previewView.getSurfaceProvider()
                );

                // Phân tích hình ảnh
                imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(
                                ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                        )
                        .build();

                imageAnalysis.setAnalyzer(
                        cameraExecutor,
                        this::analyzeImage
                );

                CameraSelector selector =
                        CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();

                cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        imageAnalysis
                );

            } catch (ExecutionException | InterruptedException e) {

                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }

                notifyError(
                        "Không thể khởi động camera: " + e.getMessage()
                );

            } catch (Exception e) {

                notifyError("Lỗi camera: " + e.getMessage());
            }

        }, ContextCompat.getMainExecutor(context));
    }

    // Phân tích một khung hình bằng cả ba mô hình
    private void analyzeImage(ImageProxy imageProxy) {

        if (released ||
                !isProcessing.compareAndSet(false, true)) {

            imageProxy.close();
            return;
        }

        long currentTime = System.currentTimeMillis();

        // Giới hạn tần suất nhận diện
        if (currentTime - lastDetectionTime < DETECTION_INTERVAL) {

            imageProxy.close();
            isProcessing.set(false);
            return;
        }

        Image mediaImage = imageProxy.getImage();

        if (mediaImage == null) {

            imageProxy.close();
            isProcessing.set(false);
            return;
        }

        lastDetectionTime = currentTime;

        try {

            // Chuyển hình ảnh camera thành InputImage
            InputImage inputImage = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.getImageInfo().getRotationDegrees()
            );

            // Mô hình 1: Image Labeling
            Task<List<ImageLabel>> labelingTask =
                    imageLabeler.process(inputImage);

            // Mô hình 2: Object Detection
            Task<List<DetectedObject>> detectionTask =
                    objectDetector.process(inputImage);

            // Mô hình 3: Text Recognition (OCR)
            Task<Text> textTask =
                    textRecognizer.process(inputImage);

            // Chờ cả ba mô hình hoàn thành
            Tasks.whenAllComplete(
                    labelingTask,
                    detectionTask,
                    textTask
            ).addOnCompleteListener(task -> {

                try {

                    if (released) {
                        return;
                    }

                    List<String> labelResults =
                            new ArrayList<>();

                    List<String> objectResults =
                            new ArrayList<>();

                    String recognizedText = "";

                    // =================================
                    // 1. IMAGE LABELING
                    // =================================

                    if (labelingTask.isSuccessful()) {

                        List<ImageLabel> labels =
                                labelingTask.getResult();

                        if (labels != null) {

                            for (ImageLabel label : labels) {

                                if (label.getConfidence() >= 0.60f) {

                                    String result =
                                            label.getText() + " - "
                                                    + Math.round(
                                                    label.getConfidence() * 100
                                            ) + "%";

                                    labelResults.add(result);
                                }

                                if (labelResults.size() >= 3) {
                                    break;
                                }
                            }
                        }
                    }

                    // =================================
                    // 2. OBJECT DETECTION
                    // =================================

                    if (detectionTask.isSuccessful()) {

                        List<DetectedObject> detectedObjects =
                                detectionTask.getResult();

                        if (detectedObjects != null) {

                            for (DetectedObject object : detectedObjects) {

                                Rect box = object.getBoundingBox();

                                String objectName = "Chưa phân loại";
                                String confidence = "";

                                if (!object.getLabels().isEmpty()) {

                                    DetectedObject.Label label =
                                            object.getLabels().get(0);

                                    objectName = label.getText();

                                    confidence = " - "
                                            + Math.round(
                                            label.getConfidence() * 100
                                    ) + "%";
                                }

                                String result =
                                        objectName + confidence
                                                + " | Vị trí: ("
                                                + box.left + ", "
                                                + box.top + ") - ("
                                                + box.right + ", "
                                                + box.bottom + ")";

                                objectResults.add(result);
                            }
                        }
                    }

                    // =================================
                    // 3. TEXT RECOGNITION (OCR)
                    // =================================

                    if (textTask.isSuccessful()) {

                        Text textResult = textTask.getResult();

                        if (textResult != null) {

                            recognizedText =
                                    textResult.getText().trim();
                        }
                    }

                    // =================================
                    // KIỂM TRA LỖI
                    // =================================

                    if (!labelingTask.isSuccessful()) {

                        notifyError(
                                "Image Labeling lỗi: "
                                        + labelingTask.getException()
                        );

                    } else if (!detectionTask.isSuccessful()) {

                        notifyError(
                                "Object Detection lỗi: "
                                        + detectionTask.getException()
                        );

                    } else if (!textTask.isSuccessful()) {

                        notifyError(
                                "Text Recognition lỗi: "
                                        + textTask.getException()
                        );

                    } else {

                        // Gộp kết quả của ba mô hình
                        String currentResult =
                                labelResults.toString()
                                        + objectResults.toString()
                                        + recognizedText;

                        // Chỉ gửi kết quả khi nội dung thay đổi
                        if (!currentResult.equals(lastResult)) {

                            lastResult = currentResult;

                            if (listener != null) {

                                listener.onDetectionResults(
                                        labelResults,
                                        objectResults,
                                        recognizedText
                                );
                            }
                        }
                    }

                } catch (Exception e) {

                    notifyError(
                            "Lỗi xử lý kết quả nhận diện: "
                                    + e.getMessage()
                    );

                } finally {

                    // Đóng frame sau khi ba mô hình hoàn tất
                    imageProxy.close();

                    isProcessing.set(false);
                }
            });

        } catch (Exception e) {

            imageProxy.close();
            isProcessing.set(false);

            notifyError(
                    "Không thể phân tích hình ảnh: "
                            + e.getMessage()
            );
        }
    }

    // Gửi lỗi về Activity
    private void notifyError(String error) {

        if (listener != null && !released) {
            listener.onError(error);
        }
    }

    // Dừng camera
    public void stopCamera() {

        if (imageAnalysis != null) {
            imageAnalysis.clearAnalyzer();
            imageAnalysis = null;
        }

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }

        preview = null;
        listener = null;
    }

    // Giải phóng tài nguyên
    public void release() {

        if (released) {
            return;
        }

        released = true;

        stopCamera();

        imageLabeler.close();
        objectDetector.close();
        textRecognizer.close();

        cameraExecutor.shutdown();
    }
}
