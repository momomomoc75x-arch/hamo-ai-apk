package com.hamo.ai;

import android.app.Activity;
import android.os.Bundle;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import java.util.ArrayList;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private WebView webView;
    private SpeechRecognizer recognizer;
    private static final int MIC_PERMISSION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new VoiceBridge(), "AndroidVoice");

        webView.loadUrl("file:///android_asset/index.html");
    }

    public class VoiceBridge {
        @JavascriptInterface
        public void startListening() {
            runOnUiThread(() -> {
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MIC_PERMISSION
                    );
                    return;
                }
                startRecognition();
            });
        }
    }

    private void startRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            sendToPage("window.onSpeechError('خدمة التعرف على الصوت غير متاحة على الجهاز')");
            return;
        }

        if (recognizer != null) {
            recognizer.destroy();
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(this);

        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                sendToPage("window.onSpeechStatus('اسمعك الآن...')");
            }

            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {
                sendToPage("window.onSpeechStatus('جاري فهم كلامك...')");
            }

            @Override public void onError(int error) {
                sendToPage("window.onSpeechError('لم أستطع سماع الكلام، حاول مرة أخرى')");
            }

            @Override public void onResults(Bundle results) {
                ArrayList<String> matches =
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                if (matches != null && !matches.isEmpty()) {
                    sendToPage("window.onSpeechResult(" +
                        JSONObject.quote(matches.get(0)) + ")");
                }
            }

            @Override public void onPartialResults(Bundle results) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-EG");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-EG");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        recognizer.startListening(intent);
    }

    private void sendToPage(String javascript) {
        runOnUiThread(() -> webView.evaluateJavascript(javascript, null));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MIC_PERMISSION) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecognition();
            } else {
                sendToPage("window.onSpeechError('يجب السماح باستخدام الميكروفون')");
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
