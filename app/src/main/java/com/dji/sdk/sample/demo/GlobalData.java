package com.dji.sdk.sample.demo;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class GlobalData {
    private static Bundle savedInstanceBundle;
    private static AppCompatActivity appCompatActivity;

    public static Bundle getSavedInstanceBundle() {
        return savedInstanceBundle;
    }

    public static void setSavedInstanceBundle(Bundle bundle) {
        savedInstanceBundle = bundle;
    }

    public static AppCompatActivity getAppCompatActivity() {
        return appCompatActivity;
    }

    public static void setAppCompatActivity(AppCompatActivity appCompatActivity) {
        GlobalData.appCompatActivity = appCompatActivity;
    }
}
