package com.dji.sdk.sample.demo.accurateLandingController;

import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.internal.view.PresentableView;

import java.util.Arrays;

import dji.common.flightcontroller.flightassistant.FillLightMode;
import dji.midware.data.model.P3.Ha;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;

import android.widget.Button;


/**
 * Class for mobile remote controller.
 */
public class ALRemoteControllerView extends RelativeLayout
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, View.OnFocusChangeListener,
        PresentableView, TextureView.SurfaceTextureListener {

    static String TAG = "Accurate landing";
    protected ImageView audioIcon;
    private Context ctx;
    private Button button1, button2, button3;
    private Bitmap droneIMG;
    protected ImageView imgView;
    protected TextureView mVideoSurface = null;
    protected TextView dataLog;
    private ReceivedVideo receivedVideo;
    private AccuracyLog accuracyLog;
    private DataFromDrone dataFromDrone;
    private GoToUsingVS goToUsingVS;
    private FlightControlMethods flightControlMethods;
    private DroneFeatures droneFeatures;
    protected TextView dist;


    private FlightCommands flightCommands;
    private GimbalController gimbalController;


    public ALRemoteControllerView(Context context) {
        super(context);
        ctx = context;
        this.receivedVideo = new ReceivedVideo();
        init(context);
    }

    private void init(Context context) {

        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.view_accurate_landing, this, true);
        initUI();
        accuracyLog = new AccuracyLog(dataLog);
        dataFromDrone = new DataFromDrone();
        flightCommands = new FlightCommands();
        goToUsingVS = new GoToUsingVS();
        flightControlMethods = new FlightControlMethods();
        droneFeatures = new DroneFeatures(flightControlMethods);
        HandleSpeechToText handleSpeechToText = new HandleSpeechToText(context, audioIcon, button1, button2, button3);
        gimbalController = new GimbalController(flightControlMethods);
//        gimbalController.rotateGimbalToDegree(-30);
//        Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                gimbalController.rotateGimbalToDegree(90);
//            }
//        }, 5000);

//        droneFeatures.setDownwardLight(FillLightMode.ON);
    }

    private void initUI() {
        mVideoSurface = findViewById(R.id.video_previewer_surface);
        imgView = findViewById(R.id.imgView);
        button1 = findViewById(R.id.btn1);
        button2 = findViewById(R.id.btn2);
        button3 = findViewById(R.id.btn3);
        audioIcon = findViewById(R.id.audioIcon);
        dataLog = findViewById(R.id.dataLog);
        dist = findViewById(R.id.dist);

        if (mVideoSurface != null) {
            mVideoSurface.setSurfaceTextureListener(this);
        }
        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        if (receivedVideo.getMCodecManager() == null) {
            showToast("" + width + "," + height);
            receivedVideo.setMCodecManager(new DJICodecManager(ctx, surfaceTexture, width, height));

        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
        Log.e(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
        Log.e(TAG, "onSurfaceTextureDestroyed");
        if (receivedVideo.getMCodecManager() != null) {
            receivedVideo.getMCodecManager().cleanSurface();
            receivedVideo.setMCodecManager(null);
        }
        return false;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
        droneIMG = mVideoSurface.getBitmap();
        imgView.setImageBitmap(droneIMG);
        accuracyLog.updateData(dataFromDrone.getAll());

        // need to provide relevant values
        GPSLocation gpsLocation = goToUsingVS.getDestGpsLocation();
        double[] pos;
        if (gpsLocation == null) {
            double lat = dataFromDrone.getGPS().getLatitude() + 0.001;
            double lon = dataFromDrone.getGPS().getLongitude() + 0.000001;
            double alt = dataFromDrone.getGPS().getAltitude();

            pos = new double[]{lat, lon, alt};
            goToUsingVS.setTargetGpsLocation(pos);
        } else {
            pos = gpsLocation.getAll();
        }
        goToUsingVS.setCurrentGpsLocation(dataFromDrone.getGPS());

        dist.setText(Arrays.toString(flightCommands.calcDistFrom(pos, dataFromDrone)) + " [" + Arrays.toString(goToUsingVS.calculateMovement()));

    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn1:
                button1.setBackgroundColor(Color.GREEN);
                button2.setBackgroundColor(Color.WHITE);
                button3.setBackgroundColor(Color.WHITE);
                break;
            case R.id.btn2:
                button2.setBackgroundColor(Color.GREEN);
                button1.setBackgroundColor(Color.WHITE);
                button3.setBackgroundColor(Color.WHITE);
                break;
            case R.id.btn3:
                button3.setBackgroundColor(Color.GREEN);
                button1.setBackgroundColor(Color.WHITE);
                button2.setBackgroundColor(Color.WHITE);
                break;
            default:
                break;
        }


    }

    @Override
    public void onFocusChange(View view, boolean b) {

    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

    }

    @Override
    public int getDescription() {
        return 0;
    }

    @NonNull
    @Override
    public String getHint() {
        return null;
    }
}
