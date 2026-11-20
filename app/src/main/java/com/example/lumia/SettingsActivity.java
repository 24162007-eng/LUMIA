package com.example.lumia;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

public class SettingsActivity extends FeatureActivity {
    private SpeechService speechService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showFeature(R.layout.activity_settings);

        SharedPreferences preferences = getSharedPreferences("lumia_settings", MODE_PRIVATE);

        SeekBar readingSpeed = findViewById(R.id.seekReadingSpeed);
        readingSpeed.setProgress(preferences.getInt("reading_speed", 50));
        readingSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    preferences.edit().putInt("reading_speed", progress).apply();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        saveSwitch(R.id.switchVibration, preferences, "vibration", true);
        saveSwitch(R.id.switchSound, preferences, "sound", true);
        saveSwitch(R.id.switchHighContrast, preferences, "high_contrast", false);

        speechService = new SpeechService(this);
        findViewById(R.id.btnVoicePreview).setOnClickListener(view -> {
            float rate = 0.5f + readingSpeed.getProgress() / 100f;
            speechService.setRate(rate);

            if (!speechService.speak(getString(R.string.settings_voice_sample))) {
                Toast.makeText(this, R.string.settings_voice_unavailable, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveSwitch(
            int viewId, SharedPreferences preferences, String key, boolean defaultValue
    ) {
        SwitchCompat toggle = findViewById(viewId);
        toggle.setChecked(preferences.getBoolean(key, defaultValue));
        toggle.setOnCheckedChangeListener((button, checked) ->
                preferences.edit().putBoolean(key, checked).apply()
        );
    }

    @Override
    protected void onStop() {
        speechService.stop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        speechService.close();
        super.onDestroy();
    }
}
