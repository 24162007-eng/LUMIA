
package com.example.lumia.services;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public class SpeechService {

    private TextToSpeech engine;
    private boolean ready = false;
    private String lastText;
    private String pendingText;

    public SpeechService(Context context) {

        engine = new TextToSpeech(
                context.getApplicationContext(),
                status -> {

                    if (status == TextToSpeech.SUCCESS && engine != null) {

                        int languageResult = engine.setLanguage(
                                Locale.forLanguageTag("vi-VN")
                        );

                        ready = languageResult >= 0;

                        // Phát nội dung đã yêu cầu trước khi TTS sẵn sàng
                        if (ready && pendingText != null) {
                            String text = pendingText;
                            pendingText = null;
                            speakNow(text);
                        }
                    }
                }
        );
    }

    // Đọc một đoạn văn bản
    public boolean speak(String text) {

        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        lastText = text;

        // Nếu TTS chưa khởi tạo xong thì lưu lại để đọc sau
        if (engine == null) {
            return false;
        }

        if (!ready) {
            pendingText = text;
            return true;
        }

        return speakNow(text);
    }

    private boolean speakNow(String text) {

        if (engine == null || !ready) {
            return false;
        }

        int result = engine.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "lumia-speech"
        );

        return result == TextToSpeech.SUCCESS;
    }

    // Đọc lại nội dung gần nhất
    public boolean repeatLast() {

        if (lastText == null || lastText.trim().isEmpty()) {
            return false;
        }

        return speak(lastText);
    }

    // Điều chỉnh tốc độ đọc
    public void setRate(float rate) {

        if (engine != null && ready) {
            engine.setSpeechRate(rate);
        }
    }

    // Dừng đọc
    public void stop() {

        if (engine != null) {
            engine.stop();
        }
    }

    // Giải phóng tài nguyên
    public void close() {

        ready = false;
        pendingText = null;

        if (engine != null) {
            engine.stop();
            engine.shutdown();
            engine = null;
        }
    }
}
