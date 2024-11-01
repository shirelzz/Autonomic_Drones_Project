package com.dji.sdk.sample.demo.accurateLandingController;

import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.internal.view.PresentableView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.Arrays;
import java.util.Locale;

import dji.sdk.codec.DJICodecManager;


/**
 * Class for mobile remote controller.
 */
public class ALRemoteControllerView extends RelativeLayout
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, View.OnFocusChangeListener,
        PresentableView, TextureView.SurfaceTextureListener {

    private static final int MOVEMENT_DETECTION_INTERVAL = 1000;  // Check for movement every 1 second
    static String TAG = "Accurate landing";

    static {
        if (!OpenCVLoader.initDebug()) {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        }
    }

    protected ImageView audioIcon, recIcon;
    protected ImageView imgView;
    protected TextureView mVideoSurface = null;
    protected TextView dataLog, dist;
    protected EditText gimbal;
    protected PresentMap presentMap;
    private boolean isRecording = false;
    private RecordVideo recordVideo;
    private Context ctx;
    private Button edgeDetect, guard_btn, stopButton, land_btn, recordBtn;
    private Button y_minus_btn, y_plus_btn, r_minus_btn, r_plus_btn, p_minus_btn, p_plus_btn,
            t_minus_btn, t_plus_btn, g_minus_btn_up;
    private Bitmap droneIMG;
    private ReceivedVideo receivedVideo;
    private AccuracyLog accuracyLog;

    private DataFromDrone dataFromDrone;
    private GoToUsingVS goToUsingVS;
    private FlightControlMethods flightControlMethods;
    private MissionControlWrapper missionControlWrapper;
    private AndroidGPS androidGPS;
    private DroneFeatures droneFeatures;
    //    private RecordingVideo recordingVideo;
    private boolean onGoToMode = false, onGoToFMMMode = false, onFollowPhoneMode = false,
            edgeDetectionMode = false;
    private FlightCommands flightCommands;
    private GimbalController gimbalController;
    private ControllerImageDetection controllerImageDetection;
    private MovementDetector movementDetector;
    private float pitch = 0.2f, yaw = 0.5f, roll = 0.2f, throttle = 0.2f; //0.2f;
    // depthmap.py video display
    private ImageView imageView;
    private YoloDetector yoloDetector;
    private boolean isMovementDetectionRunning = false;
    private Handler movementDetectionHandler = new Handler();
    private TextToSpeech textToSpeech;
    // Runnable to check for movement periodically
    private Runnable movementDetectionRunnable = new Runnable() {
        @Override
        public void run() {
            if (droneIMG != null && isMovementDetectionRunning) {
                // Run movement detection on the current image/frame
                movementDetector.detectMovement(convertBitmapToMat(droneIMG));

                // Schedule the next movement detection check after the specified interval
                movementDetectionHandler.postDelayed(this, MOVEMENT_DETECTION_INTERVAL);
            }
        }
    };

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
        initTextToSpeech(context);
        initYoloDetector(context);

        accuracyLog = new AccuracyLog(dataLog, dist, this.getContext());

        dataFromDrone = new DataFromDrone();
        flightCommands = new FlightCommands();
        goToUsingVS = new GoToUsingVS(dataFromDrone);
        flightControlMethods = new FlightControlMethods();
        droneFeatures = new DroneFeatures(flightControlMethods);
        recordVideo = new RecordVideo(recIcon);

        HandleSpeechToText handleSpeechToText = new HandleSpeechToText(
                context,
                audioIcon,
                this::stopBtnFunc,
                this.flightControlMethods::takeOff,
                this::goToFunc,
                this::accurateLanding,
                this::upButton,
                this::downButton,
                this::landBtnFunc,
                this.recordVideo::startRecording,
                this.recordVideo::stopRecording,
                this::toggleMovementDetectionStart,
                this::toggleMovementDetectionEnd
        );
        gimbalController = new GimbalController(flightControlMethods);
        controllerImageDetection = new ControllerImageDetection(dataFromDrone, flightControlMethods, ctx, imageView, gimbalController, yoloDetector, this::toggleMovementDetectionStart, edgeDetect);
        movementDetector = new MovementDetector(yoloDetector, textToSpeech);
        presentMap = new PresentMap(dataFromDrone, goToUsingVS);
        missionControlWrapper = new MissionControlWrapper(flightControlMethods.getFlightController(), dataFromDrone, dist);
        androidGPS = new AndroidGPS(context);
        gimbalController.rotateGimbalToDegree(-45);
    }

    private void initTextToSpeech(Context context) {
        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.US);  // Set language to US English
                Log.d("TTS", "TextToSpeech initialized successfully");
            } else {
                Log.e("TTS", "TextToSpeech initialization failed");
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // Stop TTS when the view is destroyed
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        // Stop movement detection runnable if it's running
        movementDetectionHandler.removeCallbacks(movementDetectionRunnable);

        // Additional cleanup
        recordVideo.onEnd();
    }

    private void initYoloDetector(Context context) {
        YoloDetector yoloDetector = new YoloDetector(context, "yolov3-tiny.cfg", "yolov3-tiny.weights");
        this.yoloDetector = yoloDetector;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initUI() {
        mVideoSurface = findViewById(R.id.video_previewer_surface);
        imageView = findViewById(R.id.imageView); // depth map python output view
        dataLog = findViewById(R.id.dataLog);

        imgView = findViewById(R.id.imgView);
        if (imgView != null) {
            imgView.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // Get the x and y coordinates of the touch event
                    float touchX = event.getX();
                    float touchY = event.getY();

                    // Get the Bitmap from the ImageView
                    Bitmap bitmap = ((BitmapDrawable) imgView.getDrawable()).getBitmap();

                    // Get the image dimensions and ImageView dimensions
                    int imgWidth = imgView.getWidth();
                    int imgHeight = imgView.getHeight();
                    int bmpWidth = bitmap.getWidth();
                    int bmpHeight = bitmap.getHeight();

                    // Calculate the scale ratio between the Bitmap and ImageView
                    float scaleX = (float) bmpWidth / imgWidth;
                    float scaleY = (float) bmpHeight / imgHeight;

                    // Convert touch coordinates to bitmap pixel coordinates
                    int pixelX = (int) (touchX * scaleX);
                    int pixelY = (int) (touchY * scaleY);

                    // Ensure the coordinates are within the bitmap bounds
                    if (pixelX >= 0 && pixelX < bmpWidth && pixelY >= 0 && pixelY < bmpHeight) {
                        // Get the pixel color at the touch point
                        int pixelColor = bitmap.getPixel(pixelX, pixelY);
                        controllerImageDetection.setClickedPoint(new Point(pixelX, pixelY));
                        // You can now do something with the pixel color or coordinates
                        Log.d("Pixel Info", "Pixel at (" + pixelX + ", " + pixelY + ") has color: " + pixelColor);
                    }
                }
                return true;
            });
        }

        stopButton = findViewById(R.id.stop_btn);
        guard_btn = findViewById(R.id.guardian);
        edgeDetect = findViewById(R.id.EdgeDetect);
        land_btn = findViewById(R.id.land_btn);
        audioIcon = findViewById(R.id.audioIcon);
        recordBtn = findViewById(R.id.recordBtn);

        y_minus_btn = findViewById(R.id.y_minus_btn);
        y_plus_btn = findViewById(R.id.y_plus_btn);
        r_minus_btn = findViewById(R.id.r_minus_btn);
        r_plus_btn = findViewById(R.id.r_plus_btn);
        p_minus_btn = findViewById(R.id.p_minus_btn);
        p_plus_btn = findViewById(R.id.p_plus_btn);
        t_minus_btn = findViewById(R.id.t_minus_btn);
        t_plus_btn = findViewById(R.id.t_plus_btn);
        g_minus_btn_up = findViewById(R.id.gimbal_pitch_update);
        recIcon = findViewById(R.id.recIcon);

        gimbal = findViewById(R.id.Gimbal);

        if (mVideoSurface != null) {
            mVideoSurface.setSurfaceTextureListener(this);
        }
        recordBtn.setOnClickListener(this);
        stopButton.setOnClickListener(this);
        guard_btn.setOnClickListener(this);
        edgeDetect.setOnClickListener(this);
        land_btn.setOnClickListener(this);
        y_minus_btn.setOnClickListener(this);
        y_plus_btn.setOnClickListener(this);
        r_minus_btn.setOnClickListener(this);
        r_plus_btn.setOnClickListener(this);
        p_minus_btn.setOnClickListener(this);
        p_plus_btn.setOnClickListener(this);
        t_minus_btn.setOnClickListener(this);
        t_plus_btn.setOnClickListener(this);
        g_minus_btn_up.setOnClickListener(this);
    }

    public void setRecIconVisibility() {
        if (!isRecording) {
            isRecording = true;
            recordVideo.startRecording();

        } else {
            isRecording = false;
            recordVideo.stopRecording();
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
        accuracyLog.closeLog();
        return false;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

        droneIMG = mVideoSurface.getBitmap();
        imgView.setImageBitmap(droneIMG);

        double[] currentPos = dataFromDrone.getCurrentPosition();
        controllerImageDetection.setCurrentImage(droneIMG, currentPos);

        if (controllerImageDetection.isEdgeDetectionMode() && gimbalController.isFinishRotate()) {
            controllerImageDetection.setBitmapFrame(droneIMG);
        }
        accuracyLog.updateData(dataFromDrone.getAll(), controllerImageDetection.getControlStatus(), controllerImageDetection.isEdgeDetectionMode(), isMovementDetectionRunning, movementDetector.getPrevAlertMessage(), flightControlMethods.isStartLanding());
        if (!onGoToMode && !onGoToFMMMode) {
            imgView.setVisibility(View.VISIBLE);
            presentMap.MapVisibility(false);
        } else {
            imgView.setVisibility(View.INVISIBLE);
            presentMap.MapVisibility(true);
        }
        if (onGoToMode) {
            goToUsingVS.setCurrentGpsLocation(dataFromDrone.getGPS());
            if (goToUsingVS.getDestGpsLocation() != null) {
                double[] pos = goToUsingVS.getDestGpsLocation().getAll();
                dist.setText(Arrays.toString(flightCommands.calcDistFrom(pos, dataFromDrone)) +
                        " [" + Arrays.toString(goToUsingVS.calculateMovement()));
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void goToFunc() {
        onGoToMode = !onGoToMode;
        onGoToFMMMode = false;
        if (onGoToMode) {
            stopButton.setBackgroundColor(Color.RED);
            GPSLocation gpsLocation = goToUsingVS.getDestGpsLocation();

            goToUsingVS.setCurrentGpsLocation(dataFromDrone.getGPS());
            if (gpsLocation != null) {
                double[] pos = gpsLocation.getAll();
                dist.setText(Arrays.toString(flightCommands.calcDistFrom(pos, dataFromDrone)) + " [" + Arrays.toString(goToUsingVS.calculateMovement()));
            }
        } else {
            imgView.setVisibility(View.VISIBLE);
            presentMap.MapVisibility(false);
        }
    }

    public void stopBtnFunc() {
        controllerImageDetection.stopEdgeDetection();
        gimbalController.rotateGimbalToDegree(-45);
        controllerImageDetection.stopObjectDetectionAlgo();
        toggleMovementDetectionEnd();

        goToUsingVS.setTargetGpsLocation((GPSLocation) null);
        ControlCommand command = flightControlMethods.stayOnPlace();
        flightControlMethods.sendVirtualStickCommands(command, 0.0f);
        flightControlMethods.disableVirtualStickControl();


        //rotateGimbalToDegree(command.getGimbalPitch());
    }

    private void accurateLanding() {
        boolean isEdgeDetect = !controllerImageDetection.isEdgeDetectionMode();
        if (isEdgeDetect || !controllerImageDetection.isObjectDetecting()) {
            edgeDetect.setBackgroundColor(Color.GREEN);
        } else {
            edgeDetect.setBackgroundColor(Color.WHITE);
        }
        startObjectDetectionAlgo();
    }

    public void upButton() {
        try {
            flightControlMethods.goThrottle(throttle);
        } catch (NumberFormatException e) {
            showToast("not float");
        }
    }

    public void downButton() {
        try {
            flightControlMethods.goThrottle(-1 * throttle);
        } catch (NumberFormatException e) {
            showToast("not float");
        }
    }

    public void landBtnFunc() {
        flightControlMethods.land(null, null);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.EdgeDetect:
                accurateLanding();
                break;

            case R.id.stop_btn:
                stopBtnFunc();
                break;

            case R.id.land_btn:
                landBtnFunc();
                break;

            //-------- set Throttle ----------
            case R.id.t_minus_btn:
                downButton();
                break;
            case R.id.t_plus_btn:
                upButton();
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
            case R.id.recordBtn:
                setRecIconVisibility();
                break;

            case R.id.guardian:
                toggleMovementDetection();
                break;

            case R.id.gimbal_pitch_update:
                gimbalController.rotateGimbalToDegree(Float.parseFloat(gimbal.getText().toString()));

            default:
                break;
        }
    }

    // Toggle the movement detection process
    private void toggleMovementDetectionStart() {

        gimbalController.rotateGimbalToDegree(0);
        // Start movement detection
        isMovementDetectionRunning = true;
        movementDetector.setOriginalImage(convertBitmapToMat(droneIMG));  // Set the initial frame
        movementDetectionHandler.post(movementDetectionRunnable);  // Start periodic checking
        guard_btn.setBackgroundColor(Color.GREEN);  // Indicate it's running
        showToast("Movement detection started");
    }

    private void toggleMovementDetection() {
        if (isMovementDetectionRunning) {
            // Stop movement detection
            toggleMovementDetectionEnd();
        } else {
            // Start movement detection
            gimbalController.rotateGimbalToDegree(-45);
            toggleMovementDetectionStart();

        }
    }

    private void toggleMovementDetectionEnd() {
        // Stop movement detection
        gimbalController.rotateGimbalToDegree(-45);
        isMovementDetectionRunning = false;
        movementDetectionHandler.removeCallbacks(movementDetectionRunnable);
        guard_btn.setBackgroundColor(Color.WHITE);  // Indicate it's stopped
        showToast("Movement detection stopped");
    }

    private Mat convertBitmapToMat(Bitmap frame) {
        Mat newCurrentImg = new Mat();
        Utils.bitmapToMat(frame, newCurrentImg);
        return newCurrentImg;
    }

    private void startObjectDetectionAlgo() {
        controllerImageDetection.startObjectDetectionAlgo();
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
        return "";
    }
}
