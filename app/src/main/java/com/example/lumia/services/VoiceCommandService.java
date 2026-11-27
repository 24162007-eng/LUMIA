package com.example.lumia.services;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import java.text.Normalizer;
import java.util.ArrayList;

public class VoiceCommandService {
    public interface Callback { void onUnknown(String heard); }

    // Chỉnh lại nếu Activity của nhóm nằm trong package con
    private static final String PKG = "com.example.lumia.";

    private final Context context;
    private final SpeechService speech;
    private final Callback callback;
    private SpeechRecognizer recognizer;

    public VoiceCommandService(Context context, Callback callback) {
        this.context = context.getApplicationContext();
        this.speech = SpeechService.getInstance(context);
        this.callback = callback;
    }

    // Gọi trên main thread
    public void startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            speech.speak("Thiết bị không hỗ trợ nhận diện giọng nói");
            return;
        }
        speech.stop();                       // tránh mic thu giọng đọc của app
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context);
            recognizer.setRecognitionListener(listener);
        }
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN");
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        recognizer.startListening(i);
    }

    public void destroy() {
        if (recognizer != null) { recognizer.destroy(); recognizer = null; }
    }

    // "Cảnh báo" -> "canh bao"
    private static String normalize(String s) {
        String n = Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD);
        return n.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").replace('đ', 'd');
    }

    private static boolean has(String t, String... keys) {
        for (String k : keys) if (t.contains(k)) return true;
        return false;
    }

    private boolean handle(String heard) {
        String t = " " + normalize(heard) + " ";   // đệm khoảng trắng để khớp nguyên từ
        if (has(t, "doc lai", "lap lai"))                      { speech.repeatLast(); return true; }
        if (has(t, " dung ", " im ", "im lang"))               { speech.stop(); return true; }
        if (has(t, "nhin", "camera", "xung quanh"))            { open("SeeActivity"); return true; }
        if (has(t, "canh bao", "vat can"))                     { open("AlertActivity"); return true; }
        if (has(t, "dan duong", "chi duong"))                  { open("NavigationActivity"); return true; }
        if (has(t, "cong dong", "bao cao"))                    { open("CommunityActivity"); return true; }
        if (has(t, "cai dat"))                                 { open("SettingsActivity"); return true; }
        return false;
    }

    private void open(String activityName) {
        try {
            Class<?> cls = Class.forName(PKG + activityName);
            speech.speak("Đang mở");
            Intent i = new Intent(context, cls);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        } catch (ClassNotFoundException e) {
            speech.speak("Chức năng này chưa sẵn sàng");
        }
    }

    private final RecognitionListener listener = new RecognitionListener() {
        @Override public void onResults(Bundle results) {
            ArrayList<String> list =
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (list == null || list.isEmpty()) {
                speech.speak("Mình chưa nghe rõ, bạn nói lại nhé");
                return;
            }
            for (String s : list) if (handle(s)) return;   // thử cả 5 kết quả
            callback.onUnknown(list.get(0));
        }
        @Override public void onError(int error) {
            android.util.Log.e("VoiceCmd", "SpeechRecognizer error = " + error);
            if (error == SpeechRecognizer.ERROR_NO_MATCH
                    || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                speech.speak("Mình chưa nghe rõ, bạn nói lại nhé");
            } else {
                speech.speak("Lỗi nhận diện giọng nói");
            }
        }
        @Override public void onReadyForSpeech(Bundle b) {}
        @Override public void onBeginningOfSpeech() {}
        @Override public void onRmsChanged(float v) {}
        @Override public void onBufferReceived(byte[] b) {}
        @Override public void onEndOfSpeech() {}
        @Override public void onPartialResults(Bundle b) {}
        @Override public void onEvent(int t, Bundle b) {}
    };
}