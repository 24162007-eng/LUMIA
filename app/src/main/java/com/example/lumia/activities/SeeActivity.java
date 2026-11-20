
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

import java.util.List;

public class SeeActivity extends AppCompatActivity {

    private PreviewView previewView;
    private TextView tvDetectionResult;

    private CameraService cameraService;

    private ActivityResultLauncher<String> requestPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_see);

        // Ánh xạ giao diện
        previewView = findViewById(R.id.cameraPreview);
        tvDetectionResult = findViewById(R.id.tvDetectionResult);

        // Khởi tạo CameraService
        cameraService = new CameraService(this);

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
        findViewById(R.id.btnCloseCamera).setOnClickListener(v -> finish());

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

    // Khởi động camera và nhận diện
    private void startCamera() {

        tvDetectionResult.setText("Đang nhận diện...");

        cameraService.startCamera(
                this,
                previewView,
                new CameraService.OnDetectionResultsListener() {

                    @Override
                    public void onDetectionResults(
                            List<String> labels,
                            List<String> objects
                    ) {

                        runOnUiThread(() -> {

                            StringBuilder result = new StringBuilder();

                            // Kết quả Image Labeling
                            result.append("IMAGE LABELING\n");

                            if (labels == null || labels.isEmpty()) {
                                result.append("Chưa nhận diện được nội dung.\n");
                            } else {
                                for (String label : labels) {
                                    result.append("• ")
                                            .append(label)
                                            .append("\n");
                                }
                            }

                            // Kết quả Object Detection
                            result.append("\nOBJECT DETECTION\n");

                            if (objects == null || objects.isEmpty()) {
                                result.append("Chưa phát hiện đối tượng.\n");
                            } else {
                                for (String object : objects) {
                                    result.append("• ")
                                            .append(object)
                                            .append("\n");
                                }
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
    }
}
