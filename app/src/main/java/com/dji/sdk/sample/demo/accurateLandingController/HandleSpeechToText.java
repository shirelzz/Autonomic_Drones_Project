package com.dji.sdk.sample.demo.accurateLandingController;

import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import com.dji.sdk.sample.demo.speechToText.SpeechToText;

public class HandleSpeechToText {
    private final SpeechToText speechToText;
    private final Runnable goToFunc, goToFMM_btn, stopButton, takeOff, edgeDetect, upButton, downButton, landButton;
    protected ImageView audioIcon;

    //context, audioIcon, goToFMM_btn, stopButton, takeOff, this::goToFunc
    public HandleSpeechToText(Context context, ImageView audioIcon, Runnable goToFMM_btn, Runnable stopButton,
                              Runnable takeOff, Runnable goToFunc, Runnable edgeDetect, Runnable upButton, Runnable downButton, Runnable landButton
    ) {
        speechToText = new SpeechToText(context, this::performActionOnResults, null, this::updateStartListening);
        speechToText.startListening();
        this.audioIcon = audioIcon;
        this.goToFMM_btn = goToFMM_btn;
        this.stopButton = stopButton;
        this.takeOff = takeOff;
        this.goToFunc = goToFunc;
        this.edgeDetect = edgeDetect;
        this.upButton = upButton;
        this.downButton = downButton;
        this.landButton = landButton;
    }

    public void updateStartListening() {
        audioIcon.setVisibility(View.VISIBLE);
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    public void performActionOnResults(String text) {
        showToast(text);

        if (text.contains("go to")) {
            showToast("go to");
            this.goToFunc.run();
        } else if (text.contains("track me") || text.contains("talk me")) {
            showToast("track me");
        } else if (text.contains("up") || text.contains("app")) { //todo: check more words
            showToast("Up");
            this.upButton.run();
        } else if (text.contains("down")) { //todo: check more words
            showToast("down");
            this.downButton.run();
        } else if (text.contains("stop") || text.contains("scope") || text.contains("dope") ||
                text.contains("panic") || text.contains("funny") ||
                text.contains("abort") || text.contains("a bird") || text.contains("about") || text.contains("a boat")) {//stop button
            showToast("Stop");
            this.stopButton.run();
        } else if (text.contains("follow me") || text.contains("photo me")) {//follow me button
            this.goToFMM_btn.run();
        } else if (text.contains("follow phone") || text.contains("auto phone me") || text.contains("photo phone")) {//follow phone button
            showToast("follow phone");
        } else if (text.contains("edge detection") || text.contains("exit jackson") || text.contains("ag detection") || text.contains("edge action") || text.contains("detection") || text.contains("angie deduction")) {//edge detection button
            showToast("Edge algo");
            this.edgeDetect.run();

        } else if (text.contains("take off")||text.contains("deco")) {//take off command //todo: check more words
            this.takeOff.run();
        } else if (text.contains("land") || text.contains("lend")) {//land button
            this.landButton.run();
        }

        else if (text.contains("camera up") || text.contains("camera app")) {
            showToast("camera up");
        } else if (text.contains("camera down")) {
            showToast("camera down");
        }
    }
}
