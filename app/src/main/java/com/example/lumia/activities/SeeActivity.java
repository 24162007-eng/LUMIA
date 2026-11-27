
package com.example.lumia.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.lumia.R;
import com.example.lumia.services.CameraService;
import com.example.lumia.services.SpeechService;

import java.util.List;

public class SeeActivity extends AppCompatActivity {

    private PreviewView previewView;
    private TextView tvDetectionResult;

    private CameraService cameraService;
    private SpeechService speechService;

    private ActivityResultLauncher<String> requestPermission;

    // Lưu văn bản OCR mới nhất
    private String recognizedText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_see);

        // Ánh xạ giao diện
        previewView = findViewById(R.id.cameraPreview);
        tvDetectionResult = findViewById(R.id.tvDetectionResult);

        // Khởi tạo các service
        cameraService = new CameraService(this);
        speechService = new SpeechService(this);

        // Nút đọc văn bản OCR
        findViewById(R.id.btnReadText).setOnClickListener(v -> {

            if (recognizedText == null || recognizedText.trim().isEmpty()) {
                Toast.makeText(
                        this,
                        "Chưa nhận diện được văn bản để đọc.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            boolean success = speechService.speak(recognizedText);

            if (!success) {
                Toast.makeText(
                        this,
                        "Không thể khởi tạo giọng đọc. Hãy kiểm tra cài đặt TTS.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        // Nút nghe lại nội dung đã đọc
        findViewById(R.id.btnRepeatRecognition).setOnClickListener(v -> {

            boolean success = speechService.repeatLast();

            if (!success) {
                Toast.makeText(
                        this,
                        "Chưa có nội dung để đọc lại.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        // Xử lý yêu cầu quyền camera
        requestPermission = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startCamera();
                    } else {
                        Toast.makeText(
                                this,
                                "Cần cấp quyền camera để sử dụng",
                                Toast.LENGTH_LONG
                        ).show();

                        finish();
                    }
                }
        );

        // Nút đóng camera
        findViewById(R.id.btnCloseCamera)
                .setOnClickListener(v -> finish());

        checkCameraPermission();
    }

    // Kiểm tra quyền camera
    private void checkCameraPermission() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {

            startCamera();

        } else {
            requestPermission.launch(Manifest.permission.CAMERA);
        }
    }

    // Khởi động camera và cả ba mô hình AI
    private void startCamera() {

        tvDetectionResult.setText(
                "Đang khởi động camera và các mô hình AI..."
        );

        cameraService.startCamera(
                this,
                previewView,
                new CameraService.OnDetectionResultsListener() {

                    @Override
                    public void onDetectionResults(
                            List<String> labels,
                            List<String> objects,
                            String recognizedTextResult
                    ) {

                        runOnUiThread(() -> {

                            // Lưu kết quả OCR mới nhất
                            recognizedText = recognizedTextResult == null
                                    ? ""
                                    : recognizedTextResult.trim();

                            StringBuilder result = new StringBuilder();

                            // IMAGE LABELING
                            result.append("IMAGE LABELING\n");

                            if (labels == null || labels.isEmpty()) {
                                result.append(
                                        "Chưa nhận diện được nội dung.\n"
                                );
                            } else {
                                for (String label : labels) {
                                    result.append("• ")
                                            .append(label)
                                            .append("\n");
                                }
                            }

                            // OBJECT DETECTION
                            result.append("\nOBJECT DETECTION\n");

                            if (objects == null || objects.isEmpty()) {
                                result.append(
                                        "Chưa phát hiện đối tượng.\n"
                                );
                            } else {
                                for (String object : objects) {
                                    result.append("• ")
                                            .append(object)
                                            .append("\n");
                                }
                            }

                            // TEXT RECOGNITION
                            result.append("\nTEXT RECOGNITION (OCR)\n");

                            if (recognizedText.isEmpty()) {
                                result.append(
                                        "Chưa phát hiện được văn bản.\n"
                                );
                            } else {
                                result.append(recognizedText)
                                        .append("\n");
                            }

                            tvDetectionResult.setText(result.toString());
                        });
                    }

                    @Override
                    public void onError(String error) {

                        runOnUiThread(() ->
                                tvDetectionResult.setText(error)
                        );
                    }
                }
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (cameraService != null) {
            cameraService.release();
            cameraService = null;
        }

        if (speechService != null) {
            speechService.close();
            speechService = null;
        }
    }
}
