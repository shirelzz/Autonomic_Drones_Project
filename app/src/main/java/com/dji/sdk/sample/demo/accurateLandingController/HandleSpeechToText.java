package com.dji.sdk.sample.demo.accurateLandingController;

import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.demo.speechToText.SpeechToText;

public class HandleSpeechToText {
    private SpeechToText speechToText;
    protected ImageView audioIcon;
    protected Button button1, button2, button3;


    public HandleSpeechToText(Context context, ImageView audioIcon, Button button1, Button button2, Button button3
    ) {
        speechToText = new SpeechToText(context, this::performActionOnResults, null, this::updateStartListening);
        speechToText.startListening();
        this.audioIcon = audioIcon;
        this.button1 = button1;
        this.button2 = button2;
        this.button3 = button3;
    }

    public void updateStartListening() {
        audioIcon.setVisibility(View.VISIBLE);
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    public void performActionOnResults(String text) {
        showToast(text);
        if (text.contains("abort") || text.contains("a bird") || text.contains("about") || text.contains("a boat")) {
            showToast("abort");
        }
//            if(text.contains(landing"){

//                hoverLandBtnFunc(R.id.land_btn);
//    }
        //            if(text.contains(take off"){

//                UI_commands.takeoff();
//               }
        else if (text.contains("go to")) {
            showToast("go to");
        } else if (text.contains("track me") || text.contains("talk me")) {
            showToast("track me");
        } else if (text.contains("panic") || text.contains("funny")) {
            showToast("panic");
        } else if (text.contains("higher") || text.contains("tier")) {
            showToast("higher");
        } else if (text.contains("lower")) {
            showToast("lower");
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

        else if (text.contains("button one") || text.contains("button 1")) {
            button1.setBackgroundColor(Color.GREEN);
            button2.setBackgroundColor(Color.WHITE);
            button3.setBackgroundColor(Color.WHITE);
        }
        else if (text.contains("button two") || text.contains("button 2") || text.contains("button too")) {

            button2.setBackgroundColor(Color.GREEN);
            button1.setBackgroundColor(Color.WHITE);
            button3.setBackgroundColor(Color.WHITE);
        }
        else if (text.contains("button three") || text.contains("button 3")) {

            button3.setBackgroundColor(Color.GREEN);
            button1.setBackgroundColor(Color.WHITE);
            button2.setBackgroundColor(Color.WHITE);

        }
    }
}
