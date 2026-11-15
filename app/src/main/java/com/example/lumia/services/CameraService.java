
package com.example.lumia.services;

import android.content.Context;
import android.graphics.Rect;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

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

import androidx.camera.core.ExperimentalGetImage;

public class CameraService {

    private final Context context;
    private final ExecutorService cameraExecutor;

    // Image Labeling
    private final ImageLabeler imageLabeler;

    // Object Detection
    private final ObjectDetector objectDetector;

    private final AtomicBoolean isProcessing =
            new AtomicBoolean(false);

    private ProcessCameraProvider cameraProvider;

    private OnDetectionResultsListener listener;

    private long lastDetectionTime = 0;
    private String lastResult = "";

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
    @ExperimentalGetImage
    public void startCamera(
            LifecycleOwner lifecycleOwner,
            PreviewView previewView,
            OnDetectionResultsListener listener
    ) {

        this.listener = listener;

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {

            try {

                cameraProvider = cameraProviderFuture.get();

                // Camera Preview
                Preview preview = new Preview.Builder()
                        .build();

                preview.setSurfaceProvider(
                        previewView.getSurfaceProvider()
                );

                // Phân tích hình ảnh liên tục
                ImageAnalysis imageAnalysis =
                        new ImageAnalysis.Builder()
                                .setBackpressureStrategy(
                                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                                )
                                .build();

                imageAnalysis.setAnalyzer(
                        cameraExecutor,
                        this::analyzeImage
                );

                CameraSelector cameraSelector =
                        CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();

                cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                );

            } catch (ExecutionException | InterruptedException e) {

                if (this.listener != null) {
                    this.listener.onError(
                            "Không thể khởi động camera: "
                                    + e.getMessage()
                    );
                }

                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }

            } catch (Exception e) {

                if (this.listener != null) {
                    this.listener.onError(
                            "Lỗi camera: " + e.getMessage()
                    );
                }
            }

        }, ContextCompat.getMainExecutor(context));
    }

    // Phân tích một khung hình bằng cả hai mô hình
    @ExperimentalGetImage
    private void analyzeImage(ImageProxy imageProxy) {

        if (!isProcessing.compareAndSet(false, true)) {
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

        android.media.Image mediaImage = imageProxy.getImage();

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

        // Mô hình 1: Image Labeling
        Task<List<ImageLabel>> labelingTask =
                imageLabeler.process(inputImage);

        // Mô hình 2: Object Detection
        Task<List<DetectedObject>> detectionTask =
                objectDetector.process(inputImage);

        // Chờ cả hai mô hình hoàn thành
        Tasks.whenAllComplete(labelingTask, detectionTask)
                .addOnCompleteListener(task -> {

                    try {

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

                                // Lấy nhãn phân loại
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

                        // Gửi kết quả về Activity
                        String currentResult =
                                labelResults.toString()
                                        + objectResults.toString();

                        if (!currentResult.equals(lastResult)) {

                            lastResult = currentResult;

                            if (listener != null) {
                                if (labelingTask.isSuccessful()
                                        && detectionTask.isSuccessful()) {

                                    listener.onDetectionResults(
                                            labelResults,
                                            objectResults
                                    );

                                } else if (!labelingTask.isSuccessful()) {

                                    listener.onError(
                                            "Image Labeling lỗi: "
                                                    + labelingTask.getException()
                                    );

                                } else {

                                    listener.onError(
                                            "Object Detection lỗi: "
                                                    + detectionTask.getException()
                                    );
                                }
                            }
                        }

                    } finally {

                        // Chỉ đóng ảnh sau khi cả hai mô hình hoàn thành
                        imageProxy.close();

                        isProcessing.set(false);
                    }
                });
    }

    // Dừng camera
    public void stopCamera() {

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }
    }

    // Giải phóng tài nguyên
    public void release() {

        stopCamera();

        imageLabeler.close();

        objectDetector.close();

        cameraExecutor.shutdown();

        listener = null;
    }
}
