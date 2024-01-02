package com.dji.sdk.sample.demo.accurateLandingController;

import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.demo.GlobalData;
import com.dji.sdk.sample.internal.utils.ToastUtils;
import com.dji.sdk.sample.internal.view.PresentableView;

import java.util.Arrays;

import dji.sdk.codec.DJICodecManager;

import android.widget.Button;

import dji.common.model.LocationCoordinate2D;


/**
 * Class for mobile remote controller.
 */
public class ALRemoteControllerView extends RelativeLayout
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, View.OnFocusChangeListener,
        PresentableView, TextureView.SurfaceTextureListener {

    static String TAG = "Accurate landing";
    protected ImageView audioIcon;
    private Context ctx;
    private Button button1, button2, button3, goTo_btn;
    private Button y_minus_btn, y_plus_btn, r_minus_btn, r_plus_btn, p_minus_btn, p_plus_btn, t_minus_btn, t_plus_btn;
    private Button g_minus_btn_up, g_plus_btn_up, g_minus_btn_side, g_plus_btn_side;
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
    protected EditText gimbal;
    protected EditText lat;
    protected EditText lon;
    protected PresentMap presentMap;
    private boolean onGoToMode = false;

    private FlightCommands flightCommands;
    private GimbalController gimbalController;
    private float pitch = 0.2f, yaw = 0.2f, roll = 0.2f, max_i = 1, throttle = 0.2f;


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
        goToUsingVS = new GoToUsingVS(dataFromDrone);
        flightControlMethods = new FlightControlMethods();
        droneFeatures = new DroneFeatures(flightControlMethods);
        Bundle savedInstanceState = GlobalData.getSavedInstanceBundle();
        HandleSpeechToText handleSpeechToText = new HandleSpeechToText(context, audioIcon, button1, button2, button3, this::goToFunc);
        gimbalController = new GimbalController(flightControlMethods);

        presentMap = new PresentMap(dataFromDrone);

//        FragmentMap yourFragment = new FragmentMap();
//        FragmentManager fragmentManager = GlobalData.getAppCompatActivity().getSupportFragmentManager();
//
//        // Get FragmentManager and start a FragmentTransaction
//        fragmentManager.beginTransaction()
//                .replace(R.id.mapView, yourFragment) // Replace fragment_container with your actual container ID
//                .commit();

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
        goTo_btn = findViewById(R.id.goTo_btn);
        audioIcon = findViewById(R.id.audioIcon);
        dataLog = findViewById(R.id.dataLog);
        dist = findViewById(R.id.dist);
        lat = findViewById(R.id.latEditText);
        lon = findViewById(R.id.lonEditText);
//        alt = findViewById(R.id.altEditText);

        y_minus_btn = findViewById(R.id.y_minus_btn);
        y_plus_btn = findViewById(R.id.y_plus_btn);
        r_minus_btn = findViewById(R.id.r_minus_btn);
        r_plus_btn = findViewById(R.id.r_plus_btn);
        p_minus_btn = findViewById(R.id.p_minus_btn);
        p_plus_btn = findViewById(R.id.p_plus_btn);
        t_minus_btn = findViewById(R.id.t_minus_btn);
        t_plus_btn = findViewById(R.id.t_plus_btn);
        g_minus_btn_up = findViewById(R.id.gimbal_pitch_update);
//        g_plus_btn_up = findViewById(R.id.g_plus_up_update);
//        g_minus_btn_side = findViewById(R.id.g_minus_side_update);
//        g_plus_btn_side = findViewById(R.id.g_plus_side_update);

        gimbal = findViewById(R.id.Gimbal);

        if (mVideoSurface != null) {
            mVideoSurface.setSurfaceTextureListener(this);
        }
        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);
        goTo_btn.setOnClickListener(this);
        y_minus_btn.setOnClickListener(this);
        y_plus_btn.setOnClickListener(this);
        r_minus_btn.setOnClickListener(this);
        r_plus_btn.setOnClickListener(this);
        p_minus_btn.setOnClickListener(this);
        p_plus_btn.setOnClickListener(this);
        t_minus_btn.setOnClickListener(this);
        t_plus_btn.setOnClickListener(this);

        g_minus_btn_up.setOnClickListener(this);
//        g_plus_btn_up.setOnClickListener(this);
//        g_minus_btn_side.setOnClickListener(this);
//        g_plus_btn_side.setOnClickListener(this);

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
        accuracyLog.closeLog();
        return false;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
        accuracyLog.updateData(dataFromDrone.getAll());

        droneIMG = mVideoSurface.getBitmap();
        imgView.setImageBitmap(droneIMG);

        if (!onGoToMode) {
//            mVideoSurface.setVisibility(View.VISIBLE);
            imgView.setVisibility(View.VISIBLE);
            presentMap.MapVisibility(false);
        } else {
//            mVideoSurface.setVisibility(View.INVISIBLE);
            imgView.setVisibility(View.INVISIBLE);
            presentMap.MapVisibility(true);
            goToUsingVS.setCurrentGpsLocation(dataFromDrone.getGPS());
            dist.setText(Arrays.toString(flightCommands.calcDistFrom(goToUsingVS.getDestGpsLocation().getAll(), dataFromDrone)) + " [" + Arrays.toString(goToUsingVS.calculateMovement()));
        }
    }

    @SuppressLint("SetTextI18n")
    private void goToFunc() {
        onGoToMode = !onGoToMode;
        if (onGoToMode) {
            goTo_btn.setBackgroundColor(Color.GREEN);
            button3.setBackgroundColor(Color.WHITE);
            button1.setBackgroundColor(Color.WHITE);
            button2.setBackgroundColor(Color.WHITE);
            GPSLocation gpsLocation = goToUsingVS.getDestGpsLocation();
            double[] pos;
            if (gpsLocation == null) {
                double curr_lat = dataFromDrone.getGPS().getLatitude() + 0.001;
                double curr_lon = dataFromDrone.getGPS().getLongitude() + 0.000001;
                double curr_alt = dataFromDrone.getGPS().getAltitude();

                pos = new double[]{curr_lat, curr_lon, curr_alt};
                goToUsingVS.setTargetGpsLocation(pos);
            } else {
                pos = gpsLocation.getAll();
            }
            goToUsingVS.setCurrentGpsLocation(dataFromDrone.getGPS());

            dist.setText(Arrays.toString(flightCommands.calcDistFrom(pos, dataFromDrone)) + " [" + Arrays.toString(goToUsingVS.calculateMovement()));
        } else {
            goTo_btn.setBackgroundColor(Color.WHITE);
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn1:
                //TODO:  need to check if the lon and lat are in the correct format
                double lon_ = Double.parseDouble(lon.getText().toString());
                double lat_ = Double.parseDouble(lat.getText().toString());
//                float alt_ = Double.parseDouble(alt.getText().toString());
                float alt_ = (float) dataFromDrone.getGPS().getAltitude();

                onGoToMode = false;

                LocationCoordinate2D targetLoc = new LocationCoordinate2D(lon_, lat_);
                MissionControlWrapper fmm = new MissionControlWrapper(targetLoc,
                        alt_ + 1.0F,
                        flightControlMethods.getFlightController());
                fmm.startGoToMission();
                ToastUtils.showToast("active go-to mission");
                button1.setBackgroundColor(Color.GREEN);
                button2.setBackgroundColor(Color.WHITE);
                button3.setBackgroundColor(Color.WHITE);
                goTo_btn.setBackgroundColor(Color.WHITE);

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
            case R.id.goTo_btn:
                this.goToFunc();
                break;
            //-------- set Throttle ----------
            case R.id.t_minus_btn:
                try {
                    flightControlMethods.goThrottle(-1 * throttle);
                } catch (NumberFormatException e) {
                    showToast("not float");
                }
                break;
            case R.id.t_plus_btn:
                try {
                    flightControlMethods.goThrottle(throttle);
                } catch (NumberFormatException e) {
                    showToast("not float");
                }
                break;
            //-------- set Pitch ----------
            case R.id.p_minus_btn:
                try {
                    flightControlMethods.goPitch(-1 * pitch);
                } catch (NumberFormatException e) {
                    showToast("not float");
                }
                break;
            case R.id.p_plus_btn:
                try {
                    flightControlMethods.goPitch(pitch);
                } catch (NumberFormatException e) {
                    showToast("not float");
                }
                break;

            //-------- set Yaw ----------
            case R.id.y_minus_btn:
                try {
                    flightControlMethods.goYaw(-1 * yaw);
                } catch (NumberFormatException e) {
                    showToast("not float");
                }
                break;
            case R.id.y_plus_btn:
                try {
                    flightControlMethods.goYaw(yaw);
                } catch (NumberFormatException e) {
                    showToast("not float");
                }
                break;

            //-------- set Roll ----------
            case R.id.r_minus_btn:
                try {
                    flightControlMethods.goRoll(-1 * roll);
                } catch (NumberFormatException e) {
                    showToast("not float");
                }
                break;
            case R.id.r_plus_btn:
                try {
                    flightControlMethods.goRoll(roll);
                } catch (NumberFormatException e) {
                    showToast("not float");
                }
                break;
            case R.id.gimbal_pitch_update:
//                ToastUtils.setResultToToast(String.valueOf(Float.parseFloat(gimbal.getText().toString())));

                gimbalController.rotateGimbalToDegree(Float.parseFloat(gimbal.getText().toString()));


//            case R.id.g_plus_up_update:
//                gimbalController.rotateGimbalToDegree((float) (-5.0));
//            case R.id.g_minus_side_update:
//                gimbalController.rotateGimbalToDegree((float) (dataFromDrone.getGimbalPitch() + 1));
//            case R.id.g_plus_side_update:
//                gimbalController.rotateGimbalToDegree((float) (dataFromDrone.getGimbalPitch() + 1));

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
