package com.example.lumia;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public class SpeechService {

    private TextToSpeech engine;
    private boolean ready;
    private String lastText;

    public SpeechService(Context context) {
        Handler mainThread = new Handler(Looper.getMainLooper());

        engine = new TextToSpeech(context.getApplicationContext(), status ->
                mainThread.post(() -> {
                    if (engine != null && status == TextToSpeech.SUCCESS) {
                        ready = engine.setLanguage(Locale.forLanguageTag("vi-VN")) >= 0;
                    }
                })
        );
    }

    public boolean speak(String text) {
        if (!ready) {
            return false;
        }

        int result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lumia-speech");
        if (result == TextToSpeech.SUCCESS) {
            lastText = text;
            return true;
        }
        return false;
    }

    public void setRate(float rate) {
        if (ready) {
            engine.setSpeechRate(rate);
        }
    }

    public void repeatLast() {
        if (lastText != null) {
            speak(lastText);
        }
    }

    public void stop() {
        if (engine != null) {
            engine.stop();
        }
    }

    public void close() {
        ready = false;
        if (engine != null) {
            engine.stop();
            engine.shutdown();
            engine = null;
        }
    }
}