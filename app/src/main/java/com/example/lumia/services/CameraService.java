
package com.example.lumia.services;

import android.content.Context;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraService {

    private final Context context;
    private final ExecutorService cameraExecutor;
    private final ImageLabeler imageLabeler;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    private ProcessCameraProvider cameraProvider;

    private OnLabelsDetectedListener listener;

    private long lastDetectionTime = 0;
    private String lastResult = "";

    private static final long DETECTION_INTERVAL = 700;

    public interface OnLabelsDetectedListener {
        void onLabelsDetected(List<String> labels);
        void onError(String error);
    }

    public CameraService(Context context) {
        this.context = context.getApplicationContext();

        cameraExecutor = Executors.newSingleThreadExecutor();

        imageLabeler = ImageLabeling.getClient(
                ImageLabelerOptions.DEFAULT_OPTIONS
        );
    }

    // Khởi động camera
    public void startCamera(
            LifecycleOwner lifecycleOwner,
            PreviewView previewView,
            OnLabelsDetectedListener listener
    ) {
        this.listener = listener;

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                // Hiển thị camera preview
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

                // Kết nối camera với Activity
                cameraProvider.unbindAll();

                cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                );

            } catch (ExecutionException | InterruptedException e) {
                if (listener != null) {
                    listener.onError(
                            "Không thể khởi động camera: " + e.getMessage()
                    );
                }

                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(
                            "Lỗi camera: " + e.getMessage()
                    );
                }
            }

        }, ContextCompat.getMainExecutor(context));
    }

    // Phân tích từng khung hình camera
    private void analyzeImage(ImageProxy imageProxy) {

        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close();
            return;
        }

        android.media.Image mediaImage = imageProxy.getImage();

        if (mediaImage == null) {
            imageProxy.close();
            isProcessing.set(false);
            return;
        }

        InputImage inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.getImageInfo().getRotationDegrees()
        );

        imageLabeler.process(inputImage)

                .addOnSuccessListener(labels -> {

                    long currentTime = System.currentTimeMillis();

                    // Chỉ cập nhật kết quả sau khoảng nghỉ
                    if (currentTime - lastDetectionTime
                            < DETECTION_INTERVAL) {
                        return;
                    }

                    List<String> results = new ArrayList<>();

                    for (ImageLabel label : labels) {

                        // Chỉ lấy kết quả có độ tin cậy từ 60%
                        if (label.getConfidence() >= 0.60f) {

                            String result =
                                    label.getText() + " - "
                                            + Math.round(
                                            label.getConfidence() * 100
                                    ) + "%";

                            results.add(result);
                        }

                        // Tối đa 3 nhãn
                        if (results.size() >= 3) {
                            break;
                        }
                    }

                    if (!results.isEmpty()) {

                        String currentResult = results.toString();

                        // Chỉ gửi kết quả khi nội dung thay đổi
                        if (!currentResult.equals(lastResult)) {

                            lastResult = currentResult;
                            lastDetectionTime = currentTime;

                            if (listener != null) {
                                listener.onLabelsDetected(results);
                            }
                        }
                    }
                })

                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onError(
                                "Không thể nhận diện: " + e.getMessage()
                        );
                    }
                })

                .addOnCompleteListener(task -> {
                    imageProxy.close();
                    isProcessing.set(false);
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

        cameraExecutor.shutdown();

        listener = null;
    }
}
