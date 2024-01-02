package com.dji.sdk.sample.demo;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

public class GlobalData {
    private static Bundle savedInstanceBundle;
    private static AppCompatActivity appCompatActivity;

    private static FragmentManager supportFragmentManager;
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

    public static FragmentManager getSupportFragmentManager() {
        return supportFragmentManager;
    }

    public static void setSupportFragmentManager(FragmentManager supportFragmentManager) {
        GlobalData.supportFragmentManager = supportFragmentManager;
    }
}
