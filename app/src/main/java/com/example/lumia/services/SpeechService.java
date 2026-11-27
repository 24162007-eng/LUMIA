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

    public static final int PRIORITY_NORMAL = 0;   // kết quả SEE, OCR, dẫn đường
    public static final int PRIORITY_ALERT  = 1;   // cảnh báo vật cản

    private static final long ALERT_COOLDOWN_MS = 2000;   // chống lặp cảnh báo

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
            Intent i = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            appContext.startActivity(i);
            return;
        }
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) { speakingNow = true; }
            @Override public void onDone(String id)  { speakingNow = false; currentPriority = PRIORITY_NORMAL; }
            @Override public void onError(String id) { speakingNow = false; currentPriority = PRIORITY_NORMAL; }
        });
        ready = true;
        if (pendingText != null) {
            speak(pendingText, pendingPriority);
            pendingText = null;
        }
    }

    /** Đọc thông tin thường (SEE, OCR, dẫn đường). Giữ nguyên chữ ký cũ. */
    public void speak(String text) {
        speak(text, PRIORITY_NORMAL);
    }

    /** Đọc với mức ưu tiên. Cảnh báo dùng PRIORITY_ALERT. */
    public synchronized void speak(String text, int priority) {
        if (text == null || text.trim().isEmpty()) return;
        if (!ready) {
            pendingText = text;
            pendingPriority = priority;
            lastText = text;
            return;
        }

        // 1. Đang đọc cảnh báo thì thông tin thường không được cắt ngang
        if (speakingNow && currentPriority == PRIORITY_ALERT && priority < PRIORITY_ALERT) {
            return;
        }

        // 2. Chống lặp cảnh báo liên tục
        if (priority == PRIORITY_ALERT) {
            long now = System.currentTimeMillis();
            if (now - lastAlertTime < ALERT_COOLDOWN_MS) return;
            lastAlertTime = now;
        }

        // 3. Không đọc lại đúng câu đang đọc
        if (speakingNow && text.equals(lastText) && priority == currentPriority) return;

        currentPriority = priority;
        speakingNow = true;
        lastText = text;
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lumia-" + System.currentTimeMillis());
    }

    public void stop() {
        if (tts != null) tts.stop();
        speakingNow = false;
        currentPriority = PRIORITY_NORMAL;
    }

    /** Đọc lại câu gần nhất, luôn được phép vì do người dùng yêu cầu. */
    public void repeatLast() {
        if (lastText.isEmpty()) return;
        String t = lastText;
        stop();
        speak(t, PRIORITY_NORMAL);
    }

    public boolean isSpeaking() {
        return speakingNow;
    }

    public void shutdown() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        instance = null;
    }
}