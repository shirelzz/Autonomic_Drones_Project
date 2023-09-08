package com.dji.sdk.sample.demo.kcgremotecontroller;

public class VLD_PID {
    private double P, I,D,max_i,integral,last_error;
    private boolean first_run;
    public VLD_PID(double p, double i, double d, double max_i) {
        this.P = p;
        this.I = i;
        this.D = d;
        this.max_i = max_i;
        first_run=true;
    }
    public double update(double error,double dt){
        if(first_run){
            last_error = error; first_run = false; }
        integral += I*error*dt;
        double diff = (error-last_error)/dt;
        double const_integral = constrain(integral,max_i,-max_i);
        double control_out = P*error + D*diff + const_integral;
        last_error = error;
        return control_out;
    }

    public double update(double error,double dt,double max_val){
        assert max_val >= 0;
        return constrain(update(error,dt),max_val,-max_val);
    }

    private double constrain(double val, double max,double min){
        if(val > max) return max;
        if(val < min) return min;
        return val;
    }

    public void setPID(double p, double i, double d, double max_i){
        this.P = p;
        this.I = i;
        this.D = d;
        this.max_i = max_i;
    }

    public double getP() {
        return P;
    }

    public void setP(double p) {
        this.P = p;
    }

    public double getI() {
        return I;
    }

    public void setI(double i) {
        this.I = i;
    }

    public double getD() {
        return D;
    }

    public void setD(double d) {
        this.D = d;
    }

    public double getMax_i() {
        return max_i;
    }

    public void setMax_i(double max_i) {
        assert max_i >= 0;
        this.max_i = max_i;
    }

    public void reset(){
        integral = 0;
        first_run = true;
    }
}
