
package com.example.lumia.services;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import com.example.lumia.AlertActivity;
import com.example.lumia.CommunityActivity;
import com.example.lumia.NavigationActivity;
import com.example.lumia.SettingsActivity;
import com.example.lumia.activities.SeeActivity;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Locale;

public class VoiceCommandService {

    public interface Callback {
        void onUnknown(String heard);
    }

    private final Context context;
    private final SpeechService speech;
    private final Callback callback;

    private SpeechRecognizer recognizer;

    public VoiceCommandService(Context context, Callback callback) {

        this.context = context.getApplicationContext();

        // Khởi tạo SpeechService bằng constructor
        this.speech = new SpeechService(context);

        this.callback = callback;
    }

    // Bắt đầu nhận diện giọng nói
    // Hàm này cần được gọi trên Main Thread
    public void startListening() {

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            speech.speak("Thiết bị không hỗ trợ nhận diện giọng nói");
            return;
        }

        // Dừng giọng đọc để tránh microphone thu lại giọng của ứng dụng
        speech.stop();

        if (recognizer == null) {

            recognizer = SpeechRecognizer.createSpeechRecognizer(context);

            recognizer.setRecognitionListener(listener);
        }

        Intent intent = new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "vi-VN"
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                5
        );

        recognizer.startListening(intent);
    }

    // Hủy dịch vụ nhận diện giọng nói
    public void destroy() {

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.destroy();
            recognizer = null;
        }

        if (speech != null) {
            speech.close();
        }
    }

    // Chuẩn hóa tiếng Việt có dấu thành không dấu
    private static String normalize(String input) {

        String normalized = Normalizer.normalize(
                input.toLowerCase(Locale.forLanguageTag("vi-VN")),
                Normalizer.Form.NFD
        );

        return normalized
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd');
    }

    // Kiểm tra từ khóa
    private static boolean has(String text, String... keywords) {

        for (String keyword : keywords) {

            if (text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    // Xử lý lệnh người dùng
    private boolean handle(String heard) {

        String text = " " + normalize(heard) + " ";

        // Đọc lại nội dung gần nhất
        if (has(text, "doc lai", "lap lai")) {
            speech.repeatLast();
            return true;
        }

        // Dừng giọng đọc
        if (has(text, " dung ", " im ", "im lang")) {
            speech.stop();
            return true;
        }

        // Mở nhận diện xung quanh
        if (has(text, "nhin", "camera", "xung quanh")) {
            open(SeeActivity.class);
            return true;
        }

        // Mở cảnh báo vật cản
        if (has(text, "canh bao", "vat can")) {
            open(AlertActivity.class);
            return true;
        }

        // Mở dẫn đường
        if (has(text, "dan duong", "chi duong")) {
            open(NavigationActivity.class);
            return true;
        }

        // Mở cộng đồng
        if (has(text, "cong dong", "bao cao")) {
            open(CommunityActivity.class);
            return true;
        }

        // Mở cài đặt
        if (has(text, "cai dat")) {
            open(SettingsActivity.class);
            return true;
        }

        return false;
    }

    // Mở màn hình tương ứng
    private void open(Class<?> activityClass) {

        try {

            speech.speak("Đang mở");

            Intent intent = new Intent(
                    context,
                    activityClass
            );

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);

        } catch (Exception e) {

            speech.speak("Chức năng này chưa sẵn sàng");
        }
    }

    // Lắng nghe kết quả nhận diện
    private final RecognitionListener listener = new RecognitionListener() {

        @Override
        public void onResults(Bundle results) {

            ArrayList<String> list = results.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION
            );

            if (list == null || list.isEmpty()) {

                speech.speak("Mình chưa nghe rõ, bạn nói lại nhé");
                return;
            }

            // Thử xử lý tối đa 5 kết quả nhận diện
            for (String result : list) {

                if (handle(result)) {
                    return;
                }
            }

            // Không hiểu lệnh
            if (callback != null) {
                callback.onUnknown(list.get(0));
            }
        }

        @Override
        public void onError(int error) {

            if (error == SpeechRecognizer.ERROR_NO_MATCH
                    || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {

                speech.speak("Mình chưa nghe rõ, bạn nói lại nhé");

            } else {

                speech.speak("Lỗi nhận diện giọng nói");
            }
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
        }

        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }
    };
}
