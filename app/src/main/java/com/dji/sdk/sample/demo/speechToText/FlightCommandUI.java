package com.dji.sdk.sample.demo.speechToText;


import com.dji.sdk.sample.demo.kcgremotecontroller.KcgLog;

public class FlightCommandUI {

    private enum states {Floor, Takeoff, Land, Forward, Backward, Yaw_R, Yaw_L,Right, Left,
        Up, Down, Emergency, Hover}

    private states state = states.Floor;
    private KcgLog log;
    private FlightCommandsAPI FPVcontrol;
    private float pitch, roll, yaw, throttle;

    public FlightCommandUI(KcgLog init_log){
        this.log = init_log;
        this.FPVcontrol = new FlightCommandsAPI(init_log);
        this.pitch = (float)0.5;
        this.roll = (float)0.2;
        this.yaw = (float)5;
        this.throttle = (float)0.1;
    }

    public void takeoff(){
        if (state == states.Floor) {
            state = states.Takeoff;
            log.setMode("Takeoff");
            FPVcontrol.takeoff();
            state = states.Hover;
            log.setMode("Hover");
        }
    }

    public void land(){
        if (state == states.Hover) {
            state = states.Land;
            log.setMode("Land");
            FPVcontrol.land();
            state = states.Floor;
            log.setMode("Floor");
        }
    }

    public void forward(){
        if (state != states.Floor){
            state = states.Forward;
            log.setMode("Forward");
            FPVcontrol.set_pitch(this.pitch, "Forward");
        }
    }

    public void backward(){
        if (state != states.Floor){
            state = states.Backward;
            log.setMode("Backward");
            FPVcontrol.set_pitch(-this.pitch, "Backward");
        }
    }

    public void turn_left(){
        if (state != states.Floor){
            state = states.Left;
            log.setMode("Left");
            FPVcontrol.set_roll(-this.roll, "Left");
        }
    }

    public void turn_right(){
        if (state != states.Floor){
            state = states.Right;
            log.setMode("Right");
            FPVcontrol.set_roll(this.roll, "Right");
        }
    }

    public void yaw_left(){
        if (state != states.Floor){
            state = states.Yaw_L;
            log.setMode("Yaw Left");
            FPVcontrol.set_yaw(-this.yaw, "Yaw Left");
        }
    }

    public void yaw_right(){
        if (state != states.Floor){
            state = states.Yaw_R;
            log.setMode("Yaw Right");
            FPVcontrol.set_yaw(this.yaw, "Yaw Right");
        }
    }

    public void up(){
        if (state != states.Floor){
            state = states.Up;
            log.setMode("Up");
            FPVcontrol.set_throttle(this.throttle, "Up");
        }
    }

    public void down(){
        if (state != states.Floor){
            state = states.Down;
            log.setMode("Down");
            FPVcontrol.set_throttle(-this.throttle, "Down");
        }
    }

    public void stop(){
        state = states.Hover;
        log.setMode("Hover");
        FPVcontrol.stayOnPlace();
    }

    public void speedUp(){
        if (this.pitch < 1.0){
            this.pitch += 0.1;
        }
        if (this.roll < 0.5){
            this.roll += 0.1;
        }
        if (this.yaw < 25){
            this.yaw += 5;
        }
        if (this.throttle < 0.5){
            this.throttle += 0.1;
        }
    }

    public void slowDown(){
        if (this.pitch > 0.3){
            this.pitch -= 0.1;
        }
        if (this.roll > 0.2){
            this.roll -= 0.1;
        }
        if (this.yaw > 5){
            this.yaw -= 5;
        }
        if (this.throttle > 0.1){
            this.throttle -= 0.1;
        }
    }
}
