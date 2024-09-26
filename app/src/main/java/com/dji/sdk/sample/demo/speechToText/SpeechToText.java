package com.dji.sdk.sample.demo.speechToText;

import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.util.Consumer;

import java.util.ArrayList;
import java.util.Locale;

public class SpeechToText implements RecognitionListener {

    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private final String[] heyRexi;
    private boolean listeningForTrigger = false;
    private final String LOG_TAG = "VoiceRecognition";
    private final Context context;
    private final Consumer<String> onResultsCallback;
    private final Consumer<String> writeOnScreenCallback;
    Runnable updateStartListening;


    public SpeechToText(Context context, Consumer<String> onResultsCallback, @Nullable Consumer<String> writeOnScreenCallback
            , Runnable updateStartListening) {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        this.context = context;
        speechRecognizer.setRecognitionListener(this);
        heyRexi = new String[]{"hey rexi", "hey rexy", "hey roxi", "hey roxy", "hey lexy",
                "hey lexi", "hey lexie", "hi rexi", "hi rexy", "hi roxi", "hi roxy", "hi lexy",
                "hi lexi", "hi lexie"};
        setRecogniserIntent();
        this.onResultsCallback = onResultsCallback;
        this.writeOnScreenCallback = writeOnScreenCallback;
        this.updateStartListening = updateStartListening;
    }

    private void setRecogniserIntent() {
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                "en");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
//        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "he");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
    }

    public void startListening() {
        speechRecognizer.startListening(recognizerIntent);
    }

    public void stopListening() {
        speechRecognizer.stopListening();
    }

    public void destroyListening() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

    }

    public boolean resetSpeechRecognizer() {

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
//        Log.i(LOG_TAG, "isRecognitionAvailable: " + SpeechRecognizer.isRecognitionAvailable(context));
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer.setRecognitionListener(this);
            return true;
        }
        return false;
    }

    @Override
    public void onBeginningOfSpeech() {
//        Log.i(LOG_TAG, "onBeginningOfSpeech");
//        progressBar.setIndeterminate(false);
//        progressBar.setMax(10);
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
//        Log.i(LOG_TAG, "onBufferReceived: " + Arrays.toString(buffer));
    }

    @Override
    public void onEndOfSpeech() {
//        Log.i(LOG_TAG, "onEndOfSpeech");
//        progressBar.setIndeterminate(true);
        stopListening();
    }

    @Override
    public void onResults(Bundle results) {
        ArrayList<String> matches = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
//        showToast(String.valueOf(results));

        StringBuilder text = new StringBuilder();
        assert matches != null;
        for (String result : matches)
            text.append(result).append("\n");
        if (writeOnScreenCallback != null) {

            writeOnScreenCallback.accept(text.toString());
//        returnedText.setText(text.toString());
        }

        if (!listeningForTrigger) {
            boolean containsKeyword = false;

            for (String keyword : heyRexi) {
                if (text.toString().toLowerCase().contains(keyword)) {
                    containsKeyword = true;
                    break; // Exit the loop if a match is found
                }
            }
            if (containsKeyword) {
                listeningForTrigger = true;
                updateStartListening.run();
                startListening();
//                returnedText.setText("I am listening now!!!!!!!!");

            }
        } else {
            // If listening for trigger, continue listening for speech
//            returnedText.setText(text.toString());
            if (onResultsCallback != null) {
                onResultsCallback.accept(text.toString().toLowerCase()); // Invoke the runnable
            }
            startListening();

        }
        startListening();
    }

    @Override
    public void onError(int errorCode) {
        String errorMessage = getErrorText(errorCode);
//        Log.i(LOG_TAG, "FAILED " + errorMessage);
//        returnedError.setText(errorMessage);
        if (writeOnScreenCallback != null) {
            writeOnScreenCallback.accept("Error:" + errorMessage);
        }
        // rest voice recogniser
        resetSpeechRecognizer();
        startListening();
    }

    @Override
    public void onEvent(int arg0, Bundle arg1) {
//        Log.i(LOG_TAG, "onEvent");
    }

    @Override
    public void onPartialResults(Bundle arg0) {
//        Log.i(LOG_TAG, "onPartialResults");
    }

    @Override
    public void onReadyForSpeech(Bundle arg0) {
//        Log.i(LOG_TAG, "onReadyForSpeech");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
//        Log.i(LOG_TAG, "onRmsChanged: " + rmsdB);
//        progressBar.setProgress((int) rmsdB);
    }


    public String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }
}
