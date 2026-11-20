package com.example.lumia;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.lumia.services.SpeechService;

import com.example.lumia.services.SpeechService;
import com.example.lumia.services.VoiceCommandService;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_AUDIO = 1;
    private VoiceCommandService voice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_main);
        SpeechService speech = SpeechService.getInstance(this);
        findViewById(R.id.btnTest).setOnClickListener(v ->
                speech.speak("Xin chào, đây là Lumia"));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SpeechService speech = SpeechService.getInstance(this);
        voice = new VoiceCommandService(this,
                heard -> speech.speak("Mình chưa hiểu lệnh: " + heard));

        findViewById(R.id.btnMic).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                voice.startListening();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (code == REQ_AUDIO && res.length > 0
                && res[0] == PackageManager.PERMISSION_GRANTED) {
            voice.startListening();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        voice.destroy();
    }
}