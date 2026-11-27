package com.example.lumia;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.lumia.services.AlertObjectInfo;
import com.example.lumia.services.CameraService;
import com.example.lumia.services.SpeechService;

import java.util.List;

@SuppressLint("UnsafeOptInUsageError")
public class AlertActivity extends FeatureActivity {

    private PreviewView previewView;
    private TextView tvCameraStatus;
    private TextView tvAlertResult;

    private CameraService cameraService;
    private SpeechService speechService;

    private ActivityResultLauncher<String> cameraPermissionLauncher;

    private boolean alertRunning = false;

    private String pendingZone = "";
    private int stableCount = 0;

    private static final int STABLE_FRAMES = 2;
    private static final long ALERT_COOLDOWN = 3500;
    private static final float CONFIDENCE_THRESHOLD = 0.50f;

    private long lastAlertTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showFeature(R.layout.activity_alert);

        previewView = findViewById(R.id.alertCameraPreview);
        tvCameraStatus = findViewById(R.id.alert_camera_hint);
        tvAlertResult = findViewById(R.id.alertResultText);

        cameraService = new CameraService(this);
        speechService = new SpeechService(this);

        cameraPermissionLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.RequestPermission(),
                        granted -> {
                            if (granted) {
                                startAlert();
                            } else {
                                Toast.makeText(
                                        this,
                                        "Cần quyền camera để sử dụng ALERT",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );

        findViewById(R.id.btnStartAlert)
                .setOnClickListener(v -> checkCameraPermission());

        findViewById(R.id.btnStopAlert)
                .setOnClickListener(v -> stopAlert());

        tvCameraStatus.setText("Camera chưa khởi động");
        tvAlertResult.setText("Ví dụ: Có xe đạp phía trước");
    }

    private void checkCameraPermission() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {

            startAlert();

        } else {

            cameraPermissionLauncher.launch(
                    Manifest.permission.CAMERA
            );
        }
    }

    private void startAlert() {

        if (alertRunning) {
            return;
        }

        alertRunning = true;

        pendingZone = "";
        stableCount = 0;
        lastAlertTime = 0;

        tvCameraStatus.setText("Đang khởi động camera...");
        tvAlertResult.setText("Đang nhận diện vật cản...");

        cameraService.startAlertCamera(
                this,
                previewView,
                new CameraService.OnAlertDetectionListener() {

                    @Override
                    public void onAlertObjectsDetected(
                            List<AlertObjectInfo> objects
                    ) {

                        runOnUiThread(() -> {

                            if (!alertRunning) {
                                return;
                            }

                            processObjects(objects);
                        });
                    }

                    @Override
                    public void onError(String error) {

                        runOnUiThread(() ->
                                tvCameraStatus.setText(error)
                        );
                    }
                }
        );

        tvCameraStatus.setText("Camera đang hoạt động");
    }

    private void processObjects(List<AlertObjectInfo> objects) {

        if (objects == null || objects.isEmpty()) {

            pendingZone = "";
            stableCount = 0;

            tvCameraStatus.setText("Đang tìm vật cản...");
            return;
        }

        AlertObjectInfo selectedObject = null;

        for (AlertObjectInfo object : objects) {

            if (object == null) {
                continue;
            }

            if (object.getConfidence() >= CONFIDENCE_THRESHOLD) {

                if (selectedObject == null
                        || object.getConfidence()
                        > selectedObject.getConfidence()) {

                    selectedObject = object;
                }
            }
        }

        if (selectedObject == null) {

            pendingZone = "";
            stableCount = 0;

            tvCameraStatus.setText("Chưa xác định được vật cản");
            return;
        }

        Rect box = selectedObject.getBoundingBox();

        int frameWidth = previewView.getWidth();

        if (frameWidth <= 0) {
            return;
        }

        float centerX = (box.left + box.right) / 2f;

        String zone;

        if (centerX < frameWidth / 3f) {
            zone = "LEFT";
        } else if (centerX < frameWidth * 2f / 3f) {
            zone = "CENTER";
        } else {
            zone = "RIGHT";
        }

        if (zone.equals(pendingZone)) {
            stableCount++;
        } else {
            pendingZone = zone;
            stableCount = 1;
        }

        String objectName = selectedObject.getLabel();

        tvCameraStatus.setText("Phát hiện: " + objectName);

        if (stableCount < STABLE_FRAMES) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        if (currentTime - lastAlertTime < ALERT_COOLDOWN) {
            return;
        }

        issueAlert(zone, objectName);

        lastAlertTime = currentTime;
    }

    private void issueAlert(String zone, String objectName) {

        String message;

        switch (zone) {

            case "LEFT":
                message = "Chú ý, có " + objectName + " bên trái";
                break;

            case "CENTER":
                message = "Cẩn thận, có " + objectName + " phía trước";
                break;

            case "RIGHT":
                message = "Chú ý, có " + objectName + " bên phải";
                break;

            default:
                return;
        }

        tvAlertResult.setText(message);

        speechService.speak(
                message,
                SpeechService.PRIORITY_ALERT
        );
    }

    private void stopAlert() {

        alertRunning = false;

        pendingZone = "";
        stableCount = 0;

        if (cameraService != null) {
            cameraService.stopCamera();
        }

        if (speechService != null) {
            speechService.stop();
        }

        tvCameraStatus.setText("Đã dừng camera");
        tvAlertResult.setText("Cảnh báo đã dừng");
    }

    @Override
    protected void onDestroy() {

        alertRunning = false;

        if (cameraService != null) {
            cameraService.release();
            cameraService = null;
        }

        if (speechService != null) {
            speechService.close();
            speechService = null;
        }

        super.onDestroy();
    }
}