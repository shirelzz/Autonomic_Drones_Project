package com.dji.sdk.sample.demo.accurateLandingController;

import android.widget.TextView;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public class AccuracyLog {

    private DateFormat df = new SimpleDateFormat("dd/MM/yyyy , HH:mm:ss");
    private DecimalFormat dcF = new DecimalFormat("##.####");

    TextView log;

    public AccuracyLog(TextView log) {
        this.log = log;
    }


}
