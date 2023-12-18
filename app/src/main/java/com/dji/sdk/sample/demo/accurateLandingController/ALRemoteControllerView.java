package com.dji.sdk.sample.demo.accurateLandingController;

import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;

import android.app.Service;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
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

import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;


/**
 * Class for mobile remote controller.
 */
public class ALRemoteControllerView extends RelativeLayout
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, View.OnFocusChangeListener,
        PresentableView, TextureView.SurfaceTextureListener {

    static String TAG = "Accurate landing";
    private Context ctx;
    private Bitmap droneIMG;
    protected ImageView imgView;
    protected TextureView mVideoSurface = null;
    protected TextView dataLog;
    private ReceivedVideo receivedVideo;
    private AccuracyLog accuracyLog;
    private DataFromDrone dataFromDrone;

    protected TextView dist;

    private FlightCommands flightCommands;
    private FlightController flightController;


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
    }

    private void initUI() {
        mVideoSurface = findViewById(R.id.video_previewer_surface);
        imgView = findViewById(R.id.imgView);
        dataLog = findViewById(R.id.dataTv);
        dist = findViewById(R.id.dist);

        if (mVideoSurface != null) {
            mVideoSurface.setSurfaceTextureListener(this);
        }
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

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        droneIMG = mVideoSurface.getBitmap();
        imgView.setImageBitmap(droneIMG);
        accuracyLog.updateData(dataFromDrone.getAll());

        // need to provide relevant values
        double lat = 32;
        double lon = 35;
        double alt = 0.1;

        double[] closePos = dataFromDrone.getGPS();
        lat = closePos[0] + 2;
        lon = closePos[1] + 2;
        alt = closePos[2] + 0.1;
        double[] pos = {lat, lon, alt};

        dist.setText(Arrays.toString(flightCommands.calcDistFrom(pos, dataFromDrone)));
    }

    @Override
    public void onClick(View view) {

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
