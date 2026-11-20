
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

        WindowCompat.enableEdgeToEdge(getWindow());
        WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        ).setAppearanceLightStatusBars(true);

        setContentView(R.layout.activity_main);

        // Xử lý khoảng cách với thanh trạng thái và thanh điều hướng
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (view, insets) -> {
                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );
                    view.setPadding(
                            bars.left, bars.top, bars.right, bars.bottom
                    );
                    return insets;
                }
        );

        // Khởi tạo chức năng giọng nói
        speech = SpeechService.getInstance(this);

        voice = new VoiceCommandService(
                this,
                heard -> speech.speak("Mình chưa hiểu lệnh: " + heard)
        );

        // Điều hướng các màn hình
        findViewById(R.id.btnSettings).setOnClickListener(view ->
                startActivity(new Intent(this, SettingsActivity.class))
        );

        findViewById(R.id.btnSee).setOnClickListener(view ->
                startActivity(new Intent(this, SeeActivity.class))
        );

        showPendingMessage(R.id.btnOcr);

        findViewById(R.id.btnAlert).setOnClickListener(view ->
                startActivity(new Intent(this, AlertActivity.class))
        );

        findViewById(R.id.btnGo).setOnClickListener(view ->
                startActivity(new Intent(this, NavigationActivity.class))
        );

        showPendingMessage(R.id.btnReport);

        findViewById(R.id.btnCommunity).setOnClickListener(view ->
                startActivity(new Intent(this, CommunityActivity.class))
        );

        // Nút Voice: kiểm tra quyền microphone và bắt đầu nghe
        findViewById(R.id.btnVoice).setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED) {
                voice.startListening();
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        REQ_AUDIO
                );
            }
        });
    }

    private void showPendingMessage(int buttonId) {
        findViewById(buttonId).setOnClickListener(view ->
                Toast.makeText(
                        this,
                        R.string.feature_pending,
                        Toast.LENGTH_SHORT
                ).show()
        );
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults
        );

        if (requestCode == REQ_AUDIO
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            voice.startListening();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (voice != null) {
            voice.destroy();
        }
    }
}
