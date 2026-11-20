package com.example.lumia.services;

import android.content.Context;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import java.util.Locale;

public class SpeechService implements TextToSpeech.OnInitListener {
    private static final String TAG = "SpeechService";
    private static SpeechService instance;

    private final Context appContext;
    private TextToSpeech tts;
    private boolean ready = false;
    private String lastText = "";
    private String pendingText = null;   // câu gọi speak() trước khi TTS khởi tạo xong

    // Singleton: mọi module dùng chung một TextToSpeech
    public static synchronized SpeechService getInstance(Context context) {
        if (instance == null) {
            instance = new SpeechService(context.getApplicationContext());
        }
        return instance;
    }

    private SpeechService(Context context) {
        this.appContext = context;
        tts = new TextToSpeech(context, this);
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) {
            Log.e(TAG, "Khởi tạo TTS thất bại");
            return;
        }
        int result = tts.setLanguage(new Locale("vi", "VN"));
        if (result == TextToSpeech.LANG_MISSING_DATA
                || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(TAG, "Thiếu dữ liệu giọng tiếng Việt");
            // Mở màn hình tải dữ liệu giọng đọc
            Intent i = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            appContext.startActivity(i);
            return;
        }
        ready = true;
        if (pendingText != null) {
            speak(pendingText);
            pendingText = null;
        }
    }

    public void speak(String text) {
        if (text == null || text.trim().isEmpty()) return;
        lastText = text;
        if (!ready) {            // chưa sẵn sàng thì giữ lại câu cuối
            pendingText = text;
            return;
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lumia-" + System.currentTimeMillis());
    }

    public void stop() {
        if (tts != null) tts.stop();
    }

    public void repeatLast() {
        if (!lastText.isEmpty()) speak(lastText);
    }

    public boolean isSpeaking() {
        return tts != null && tts.isSpeaking();
    }

    public void shutdown() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        instance = null;
    }
}