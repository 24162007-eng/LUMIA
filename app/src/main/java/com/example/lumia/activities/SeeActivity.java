
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

        // Anh xa giao dien
        previewView = findViewById(R.id.cameraPreview);
        tvDetectionResult = findViewById(R.id.tvDetectionResult);

        // Khoi tao CameraService
        cameraService = new CameraService(this);

        // Dang ky yeu cau quyen camera
        requestPermission = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startCamera();
                    } else {
                        Toast.makeText(
                                this,
                                "Can cap quyen camera de su dung",
                                Toast.LENGTH_LONG
                        ).show();

                        finish();
                    }
                }
        );

        // Nut quay lai
        findViewById(R.id.btnCloseCamera)
                .setOnClickListener(v -> finish());

        // Kiem tra quyen camera
        checkCameraPermission();
    }

    // Kiem tra quyen camera
    private void checkCameraPermission() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {

            startCamera();

        } else {
            requestPermission.launch(
                    Manifest.permission.CAMERA
            );
        }
    }

    // Khoi dong camera va nhan dien lien tuc
    private void startCamera() {

        tvDetectionResult.setText("Đang nhận diện...");

        cameraService.startCamera(
                this,
                previewView,
                new CameraService.OnLabelsDetectedListener() {

                    @Override
                    public void onLabelsDetected(List<String> labels) {

                        runOnUiThread(() -> {

                            if (labels == null || labels.isEmpty()) {
                                tvDetectionResult.setText(
                                        "Chưa nhận diện được vật thể."
                                );
                                return;
                            }

                            StringBuilder result = new StringBuilder();

                            result.append("Phát hiện:\n");

                            for (String label : labels) {
                                result.append("• ")
                                        .append(label)
                                        .append("\n");
                            }

                            tvDetectionResult.setText(
                                    result.toString()
                            );
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
