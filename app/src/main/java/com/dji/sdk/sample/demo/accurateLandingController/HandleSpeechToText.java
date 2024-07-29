package com.dji.sdk.sample.demo.accurateLandingController;

import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import com.dji.sdk.sample.demo.speechToText.SpeechToText;

public class HandleSpeechToText {
    private final SpeechToText speechToText;
    ;
    private final Runnable goToFunc, goToFMM_btn, stopButton, followPhone_btn, edgeDetect;
    protected ImageView audioIcon;

    //context, audioIcon, goToFMM_btn, stopButton, followPhone_btn, this::goToFunc
    public HandleSpeechToText(Context context, ImageView audioIcon, Runnable goToFMM_btn, Runnable stopButton,
                              Runnable followPhone_btn, Runnable goToFunc, Runnable edgeDetect
    ) {
        speechToText = new SpeechToText(context, this::performActionOnResults, null, this::updateStartListening);
        speechToText.startListening();
        this.audioIcon = audioIcon;
        this.goToFMM_btn = goToFMM_btn;
        this.stopButton = stopButton;
        this.followPhone_btn = followPhone_btn;
        this.goToFunc = goToFunc;
        this.edgeDetect = edgeDetect;
    }

    public void updateStartListening() {
        audioIcon.setVisibility(View.VISIBLE);
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    public void performActionOnResults(String text) {
        showToast(text);
        boolean b = text.contains("edge detection") || text.contains("ag detection") || text.contains("angie deduction");
        showToast("" + b);
        if (text.contains("go to")) {
            showToast("go to");
            this.goToFunc.run();
        } else if (text.contains("track me") || text.contains("talk me")) {
            showToast("track me");
        } else if (text.contains("higher") || text.contains("tier")) {
            showToast("higher");
        } else if (text.contains("lower")) {
            showToast("lower");
        } else if (text.contains("stop") || text.contains("scope") || text.contains("dope") ||
                text.contains("panic") || text.contains("funny") ||
                text.contains("abort") || text.contains("a bird") || text.contains("about") || text.contains("a boat")) {//stop button
            showToast("Stop");
            this.stopButton.run();
        } else if (text.contains("follow me") || text.contains("photo me")) {//follow me button
            this.goToFMM_btn.run();
        } else if (text.contains("follow phone") || text.contains("auto phone me") || text.contains("photo phone")) {//follow phone button
            this.followPhone_btn.run();
        } else if (text.contains("edge detection") || text.contains("exit jackson") || text.contains("ag detection") || text.contains("edge action") || text.contains("detection") || text.contains("angie deduction")) {//edge detection button
            showToast("Edge algo");

            this.edgeDetect.run();

        } else if (text.contains("land") || text.contains("lend")) {//land button

        }

//            else if(text.contains(stay)){ //our algorithm (pause) replace with pose){

//                pauseBtnFunc();
//                }
//            else if(text.contains(stop)){

//                stopBtnFunc();
//            }
//            else if(text.contains(hover)){ //Asaf algorith
//    }

//            if(text.contains(over)){

//                hoverLandBtnFunc(R.id.hover_btn);
//            }
        else if (text.contains("camera up") || text.contains("camera app")) {
            showToast("camera up");
        } else if (text.contains("camera down")) {
            showToast("camera down");
        }

//           else if(text.contains(turn right)){

//                UI_commands.turn_right();
//
//           }
//           else if(text.contains(turn left)){

//                UI_commands.turn_left();
//
//            }
//            else if(text.contains(backward)){

//                UI_commands.backward();
//
//            }
//            else if(text.contains(forward)){

//                UI_commands.forward();
//
//            }
//            else if(text.contains(follow me)){

//                followMeMissionFunc();
//    }

    }
}
