package com.example.lumia;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.example.lumia.services.CameraService;

import java.util.List;

public class SeeActivity extends FeatureActivity {
    private CameraService cameraService;
    private Button startButton;
    private TextView result;

    private final ActivityResultLauncher<String> cameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startRecognition();
                } else {
                    result.setText(R.string.permission_denied);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showFeature(R.layout.activity_see);
        result = findViewById(R.id.tvDetectionResult);
        startButton = findViewById(R.id.btnCapture);
        startButton.setOnClickListener(view -> requestCamera());
        showPendingMessage(R.id.btnReadText);
        showPendingMessage(R.id.btnRepeatRecognition);
    }

    private void requestCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startRecognition();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.permission_camera_reason)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.permission_continue,
                            (dialog, which) -> cameraPermission.launch(Manifest.permission.CAMERA))
                    .show();
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    private void startRecognition() {
        releaseCamera();
        result.setText(R.string.see_scanning);
        startButton.setEnabled(false);
        cameraService = new CameraService(this);
        cameraService.startCamera(this, findViewById(R.id.cameraPreview),
                new CameraService.OnLabelsDetectedListener() {
                    @Override
                    public void onLabelsDetected(List<String> labels) {
                        if (labels.isEmpty()) {
                            result.setText(R.string.see_no_result);
                        } else {
                            result.setText(getString(R.string.see_detected,
                                    TextUtils.join("\n", labels)));
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.w("SeeActivity", error);
                        releaseCamera();
                        result.setText(R.string.see_camera_error);
                        startButton.setText(R.string.see_retry);
                        startButton.setEnabled(true);
                    }
                });
    }

    private void releaseCamera() {
        if (cameraService != null) {
            cameraService.release();
            cameraService = null;
        }
    }

    @Override
    protected void onDestroy() {
        releaseCamera();
        super.onDestroy();
    }
}
