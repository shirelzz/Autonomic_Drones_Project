package com.dji.sdk.sample.demo.accurateLandingController;

import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import com.dji.sdk.sample.demo.speechToText.SpeechToText;

public class HandleSpeechToText {
    private final SpeechToText speechToText;
    private final Runnable goToFunc,
            startRecording, stopRecording,
            stopButton, takeOff, edgeDetect, upButton, downButton, landButton, movementDetectedStart, movementDetectedEnd;
    protected ImageView audioIcon;

    public HandleSpeechToText(Context context,
                              ImageView audioIcon,
                              Runnable stopButton,
                              Runnable takeOff,
                              Runnable goToFunc,
                              Runnable edgeDetect,
                              Runnable upButton,
                              Runnable downButton,
                              Runnable landButton,
                              Runnable startRecording,
                              Runnable stopRecording,
                              Runnable movementDetectedStart,
                              Runnable movementDetectedEnd
    ) {
        speechToText = new SpeechToText(context, this::performActionOnResults, null, this::updateStartListening);
        speechToText.startListening();
        this.audioIcon = audioIcon;
        this.startRecording = startRecording;
        this.stopRecording = stopRecording;
        this.stopButton = stopButton;
        this.takeOff = takeOff;
        this.goToFunc = goToFunc;
        this.edgeDetect = edgeDetect;
        this.upButton = upButton;
        this.downButton = downButton;
        this.landButton = landButton;
        this.movementDetectedStart = movementDetectedStart;
        this.movementDetectedEnd = movementDetectedEnd;
    }

    public void updateStartListening() {
        audioIcon.setVisibility(View.VISIBLE);
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    public void performActionOnResults(String text) {
        showToast(text);
        text = text.toLowerCase();

        // Movement Commands
        if (containsAny(text, "go to")) {
            showToast("go to");
            this.goToFunc.run();
        } else if (containsAny(text, "track me", "talk me")) {
            showToast("track me");
        } else if (containsAny(text, "move up", "up", "app")) {
            showToast("Up");
            this.upButton.run();
        } else if (containsAny(text, "move down", "down", "done")) {
            showToast("down");
            this.downButton.run();
        }

        // Recording Commands
        else if (containsAny(text, "start recording", "start record", "doctor clothing")) {
            this.startRecording.run();
        } else if (containsAny(text, "stop recording", "stop record")) {
            this.stopRecording.run();
        }

        // Stop Commands
        else if (containsAny(text, "stop", "scope", "dope", "panic", "funny", "abort",
                "a bird", "about", "a boat")) {
            showToast("Stop");
            this.stopButton.run();
        }

        // Follow Commands
        else if (containsAny(text, "follow me", "photo me")) {
            showToast("follow me");
        } else if (containsAny(text, "follow phone", "auto phone me", "photo phone")) {
            showToast("follow phone");
        }

        // Algorithm Commands
        else if (containsAny(text, "edge detection", "algorithm", "energy detection",
                "exit jackson", "ag detection", "edge action", "detection",
                "angie deduction", "and the deduction")) {
            showToast("Edge algo");
            this.edgeDetect.run();
        }

        // Drone Commands
        else if (containsAny(text, "take off", "deco")) {
            this.takeOff.run();
        } else if (containsAny(text, "land", "lend")) {
            this.landButton.run();
        }

        // Camera Commands
        else if (containsAny(text, "camera up", "camera app")) {
            showToast("camera up");
        } else if (containsAny(text, "camera down")) {
            showToast("camera down");
        }
    }

    // Helper method to check if the text contains any of the keywords
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
