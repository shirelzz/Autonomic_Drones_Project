package com.dji.sdk.sample.demo.accurateLandingController;

public class VLD_PID {
    // PID coefficients
    private double P, I, D;
    // Maximum allowable integral value
    private double max_i;
    // Integral term accumulator
    private double integral;
    // Last error value (for derivative calculation)
    private double last_error;
    // Flag to check if it's the first run
    private boolean first_run;

    // Constructor to initialize PID coefficients and maximum integral value
    public VLD_PID(double p, double i, double d, double max_i) {
        this.P = p;
        this.I = i;
        this.D = d;
        this.max_i = max_i;
        first_run = true;
    }

    // Method to update the control output based on the current error and time difference
    public double update(double error, double dt) {
        // If it's the first run, initialize last_error
        if (first_run) {
            last_error = error;
            first_run = false;
        }
        // Update the integral term
        integral += I * error * dt;
        // Calculate the derivative term
        double diff = (error - last_error) / dt;
        // Constrain the integral term within the limits
        double const_integral = constrain(integral, max_i, -max_i);
        // Calculate the control output
        double control_out = P * error + D * diff + const_integral;
        // Update last_error for the next cycle
        last_error = error;
        return control_out;
    }

    // Overloaded update method with an additional parameter for maximum control output
    public double update(double error, double dt, double max_val) {
        assert max_val >= 0;
        return constrain(update(error, dt), max_val, -max_val);
    }

    // Method to constrain a value within specified bounds
    private double constrain(double val, double max, double min) {
        if (val > max) return max;
        if (val < min) return min;
        return val;
    }

    // Method to set new PID coefficients and maximum integral value
    public void setPID(double p, double i, double d, double max_i) {
        this.P = p;
        this.I = i;
        this.D = d;
        this.max_i = max_i;
    }

    // Getter and setter methods for PID coefficients and max integral value
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

    // Method to reset the integral term and the first_run flag
    public void reset() {
        integral = 0;
        first_run = true;
    }
}
