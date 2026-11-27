
package com.example.lumia;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.lumia.services.SpeechService;
import com.example.lumia.services.VoiceCommandService;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_AUDIO = 1;

    private VoiceCommandService voice;
    private SpeechService speech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hiển thị giao diện Edge-to-Edge
        WindowCompat.enableEdgeToEdge(getWindow());

        WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        ).setAppearanceLightStatusBars(true);

        setContentView(R.layout.activity_main);

        // Xử lý khoảng cách với thanh trạng thái
        // và thanh điều hướng
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (view, insets) -> {

                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    view.setPadding(
                            bars.left,
                            bars.top,
                            bars.right,
                            bars.bottom
                    );

                    return insets;
                }
        );

        // Khởi tạo dịch vụ giọng nói
        speech = new SpeechService(this);

        voice = new VoiceCommandService(
                this,
                heard -> speech.speak(
                        "Mình chưa hiểu lệnh: " + heard
                )
        );

        // Nút Cài đặt
        findViewById(R.id.btnSettings).setOnClickListener(view ->
                startActivity(
                        new Intent(this, SettingsActivity.class)
                )
        );

        // Nút Nhận diện xung quanh
        findViewById(R.id.btnSee).setOnClickListener(view ->
                startActivity(
                        new Intent(
                                this,
                                com.example.lumia.activities.SeeActivity.class
                        )
                )
        );

        // Nút Đọc văn bản
        showPendingMessage(R.id.btnOcr);

        // Nút Phát hiện vật cản
        findViewById(R.id.btnAlert).setOnClickListener(view ->
                startActivity(
                        new Intent(this, AlertActivity.class)
                )
        );

        // Nút Dẫn đường
        findViewById(R.id.btnGo).setOnClickListener(view ->
                startActivity(
                        new Intent(this, NavigationActivity.class)
                )
        );

        // Nút Báo cáo vật cản
        showPendingMessage(R.id.btnReport);

        // Nút Cộng đồng
        findViewById(R.id.btnCommunity).setOnClickListener(view ->
                startActivity(
                        new Intent(this, CommunityActivity.class)
                )
        );

        // Nút Nói lệnh
        findViewById(R.id.btnVoice).setOnClickListener(view -> {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED) {

                voice.startListening();

            } else {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.RECORD_AUDIO
                        },
                        REQ_AUDIO
                );
            }
        });
    }

    // Hiển thị thông báo cho tính năng chưa hoàn thiện
    private void showPendingMessage(int buttonId) {

        findViewById(buttonId).setOnClickListener(view ->
                Toast.makeText(
                        this,
                        R.string.feature_pending,
                        Toast.LENGTH_SHORT
                ).show()
        );
    }

    // Xử lý kết quả cấp quyền microphone
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == REQ_AUDIO
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            if (voice != null) {
                voice.startListening();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (voice != null) {
            voice.destroy();
            voice = null;
        }

        if (speech != null) {
            speech.close();
            speech = null;
        }
    }
}
