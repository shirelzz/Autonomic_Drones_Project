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
    protected EditText lat;
    protected EditText lon;
//    protected EditText alt;

    protected PresentMap presentMap;
    private boolean onGoToMode = false;

    private FlightCommands flightCommands;
    private GimbalController gimbalController;
    private float pitch = 0.5f, yaw = 0.02f, roll = 0.01f, max_i = 1, throttle = -0.6f;//t fot vertical throttle


    public ALRemoteControllerView(Context context) {
        super(context);
        ctx = context;
        this.receivedVideo = new ReceivedVideo();
        init(context);
    }

    // Constructor to handle null context or default constructor
//    public ALRemoteControllerView(Context context, AttributeSet attrs) {
//        super(context, attrs);
//        if (context != null) {
//            ctx = context;
//            this.receivedVideo = new ReceivedVideo();
//            init(context);
//        } else {
//            // Handle the case of null context (log an error, throw an exception, or provide a default behavior)
//        }
//    }

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

//        presentMap = new PresentMap(savedInstanceState, context, (Activity) getContext());

        HandleSpeechToText handleSpeechToText = new HandleSpeechToText(context, audioIcon, button1, button2, button3, this::goToFunc);
        gimbalController = new GimbalController(flightControlMethods);

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
        accuracyLog.updateData(dataFromDrone.getAll());
//        if (!onGoToMode) {
//            imgView.setVisibility(View.VISIBLE);
        droneIMG = mVideoSurface.getBitmap();
        imgView.setImageBitmap(droneIMG);
//            presentMap.getMapView().setVisibility(View.INVISIBLE);
//        } else {
//            presentMap.getMapView().setVisibility(View.VISIBLE);
//            imgView.setVisibility(View.INVISIBLE);
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
        //        }
    }

    private void goToFunc() {
        onGoToMode = !onGoToMode;
        if (onGoToMode) {
            goTo_btn.setBackgroundColor(Color.GREEN);
            button3.setBackgroundColor(Color.WHITE);
            button1.setBackgroundColor(Color.WHITE);
            button2.setBackgroundColor(Color.WHITE);
        } else {
            goTo_btn.setBackgroundColor(Color.WHITE);
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn1:
                double lat_ = Double.parseDouble(lat.getText().toString());
                double lon_ = Double.parseDouble(lon.getText().toString());
//                float alt_ = Double.parseDouble(alt.getText().toString());
                float alt_ = (float) dataFromDrone.getGPS().getAltitude();

                LocationCoordinate2D targetLoc = new LocationCoordinate2D(lat_, lon_);
                MissionControlWrapper fmm = new MissionControlWrapper(targetLoc,
                        alt_ + 1.0F,
                        flightControlMethods.getFlightController());
//                fmm.startGoToMission();
                ToastUtils.showToast("active go-to mission");

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
