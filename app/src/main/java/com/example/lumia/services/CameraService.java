
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
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

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

    private final ImageLabeler imageLabeler;
    private final ObjectDetector objectDetector;

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

    public interface OnDetectionResultsListener {

        void onDetectionResults(
                List<String> labels,
                List<String> objects
        );

        void onError(String error);
    }

    public CameraService(Context context) {

        this.context = context.getApplicationContext();

        cameraExecutor = Executors.newSingleThreadExecutor();

        // Khởi tạo Image Labeling
        imageLabeler = ImageLabeling.getClient(
                ImageLabelerOptions.DEFAULT_OPTIONS
        );

        // Khởi tạo Object Detection
        ObjectDetectorOptions options =
                new ObjectDetectorOptions.Builder()
                        .setDetectorMode(
                                ObjectDetectorOptions.STREAM_MODE
                        )
                        .enableClassification()
                        .build();

        objectDetector = ObjectDetection.getClient(options);
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

    // Phân tích một khung hình bằng cả hai mô hình
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

        InputImage inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.getImageInfo().getRotationDegrees()
        );

        // Image Labeling
        Task<List<ImageLabel>> labelingTask =
                imageLabeler.process(inputImage);

        // Object Detection
        Task<List<DetectedObject>> detectionTask =
                objectDetector.process(inputImage);

        // Chờ cả hai mô hình hoàn thành
        Tasks.whenAllComplete(labelingTask, detectionTask)
                .addOnCompleteListener(task -> {

                    try {

                        if (released) {
                            return;
                        }

                        List<String> labelResults =
                                new ArrayList<>();

                        List<String> objectResults =
                                new ArrayList<>();

                        // Lấy kết quả Image Labeling
                        if (labelingTask.isSuccessful()) {

                            List<ImageLabel> labels =
                                    labelingTask.getResult();

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

                        // Lấy kết quả Object Detection
                        if (detectionTask.isSuccessful()) {

                            List<DetectedObject> detectedObjects =
                                    detectionTask.getResult();

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

                        // Kiểm tra lỗi của hai mô hình
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

                        } else {

                            String currentResult =
                                    labelResults.toString()
                                            + objectResults.toString();

                            // Chỉ gửi kết quả khi nội dung thay đổi
                            if (!currentResult.equals(lastResult)) {

                                lastResult = currentResult;

                                if (listener != null) {

                                    listener.onDetectionResults(
                                            labelResults,
                                            objectResults
                                    );
                                }
                            }
                        }

                    } finally {

                        // Đóng frame sau khi cả hai mô hình hoàn tất
                        imageProxy.close();

                        isProcessing.set(false);
                    }
                });
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

        cameraExecutor.shutdown();
    }
}