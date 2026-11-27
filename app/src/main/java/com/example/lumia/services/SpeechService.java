
package com.example.lumia.services;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;

public class SpeechService implements TextToSpeech.OnInitListener {

    private static final String TAG = "SpeechService";

    public static final int PRIORITY_NORMAL = 0;
    public static final int PRIORITY_ALERT = 1;

    private static final long ALERT_COOLDOWN_MS = 2000;

    private static SpeechService instance;

    private final Context appContext;
    private final Handler main = new Handler(Looper.getMainLooper());

    private TextToSpeech tts;
    private boolean ready = false;

    private String lastText = "";
    private String pendingText = null;
    private int pendingPriority = PRIORITY_NORMAL;

    private volatile int currentPriority = PRIORITY_NORMAL;
    private volatile boolean speakingNow = false;

    private long lastAlertTime = 0;

    // Singleton
    public static synchronized SpeechService getInstance(Context context) {
        if (instance == null) {
            instance = new SpeechService(context.getApplicationContext());
        }
        return instance;
    }

    // Khởi tạo TTS
    public SpeechService(Context context) {
        appContext = context.getApplicationContext();

        tts = new TextToSpeech(appContext, this);
    }

    @Override
    public void onInit(int status) {

        if (status != TextToSpeech.SUCCESS || tts == null) {
            Log.e(TAG, "Khởi tạo TTS thất bại");
            return;
        }

        int result = tts.setLanguage(new Locale("vi", "VN"));

        if (result == TextToSpeech.LANG_MISSING_DATA
                || result == TextToSpeech.LANG_NOT_SUPPORTED) {

            Log.e(TAG, "Thiếu dữ liệu giọng tiếng Việt");

            Intent intent = new Intent(
                    TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
            );

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            appContext.startActivity(intent);
            return;
        }

        tts.setOnUtteranceProgressListener(
                new UtteranceProgressListener() {

                    @Override
                    public void onStart(String id) {
                        speakingNow = true;
                    }

                    @Override
                    public void onDone(String id) {
                        speakingNow = false;
                        currentPriority = PRIORITY_NORMAL;
                    }

                    @Override
                    public void onError(String id) {
                        speakingNow = false;
                        currentPriority = PRIORITY_NORMAL;
                    }
                }
        );

        ready = true;

        // Đọc nội dung đã yêu cầu trước khi TTS sẵn sàng
        if (pendingText != null) {
            String text = pendingText;
            int priority = pendingPriority;

            pendingText = null;
            pendingPriority = PRIORITY_NORMAL;

            speak(text, priority);
        }
    }

    // Đọc văn bản thông thường
    public boolean speak(String text) {

        return speak(text, PRIORITY_NORMAL);
    }

    // Đọc văn bản có mức độ ưu tiên
    public synchronized boolean speak(String text, int priority) {

        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        lastText = text;

        // TTS chưa khởi tạo xong
        if (!ready || tts == null) {
            pendingText = text;
            pendingPriority = priority;
            return true;
        }

        // Không cắt ngang cảnh báo để đọc thông tin thường
        if (speakingNow
                && currentPriority == PRIORITY_ALERT
                && priority < PRIORITY_ALERT) {
            return false;
        }

        // Chống lặp cảnh báo liên tục
        if (priority == PRIORITY_ALERT) {

            long now = System.currentTimeMillis();

            if (now - lastAlertTime < ALERT_COOLDOWN_MS) {
                return false;
            }

            lastAlertTime = now;
        }

        // Không đọc lại cùng nội dung cùng mức ưu tiên
        if (speakingNow
                && text.equals(lastText)
                && priority == currentPriority) {
            return false;
        }

        currentPriority = priority;
        speakingNow = true;

        lastText = text;

        int result = tts.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "lumia-" + System.currentTimeMillis()
        );

        if (result != TextToSpeech.SUCCESS) {
            speakingNow = false;
            currentPriority = PRIORITY_NORMAL;
            return false;
        }

        return true;
    }

    // Đọc lại nội dung gần nhất
    public boolean repeatLast() {

        if (lastText == null || lastText.trim().isEmpty()) {
            return false;
        }

        String text = lastText;

        stop();

        return speak(text, PRIORITY_NORMAL);
    }

    // Kiểm tra đang đọc hay không
    public boolean isSpeaking() {
        return speakingNow;
    }

    // Điều chỉnh tốc độ đọc
    public void setRate(float rate) {

        if (tts != null && ready) {
            tts.setSpeechRate(rate);
        }
    }

    // Dừng đọc
    public void stop() {

        if (tts != null) {
            tts.stop();
        }

        speakingNow = false;
        currentPriority = PRIORITY_NORMAL;
    }

    // Giải phóng tài nguyên
    public void close() {

        ready = false;
        pendingText = null;
        speakingNow = false;
        currentPriority = PRIORITY_NORMAL;

        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }
}
