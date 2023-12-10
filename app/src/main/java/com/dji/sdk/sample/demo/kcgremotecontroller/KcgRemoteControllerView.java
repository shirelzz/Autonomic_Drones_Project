package com.dji.sdk.sample.demo.kcgremotecontroller;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.demo.speechToText.SpeechToText;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
import com.dji.sdk.sample.internal.utils.ToastUtils;
import com.dji.sdk.sample.internal.view.PresentableView;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;

import com.dji.sdk.sample.demo.stitching.Tracker;

import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;

import java.text.AttributedCharacterIterator;

import dji.sdk.codec.DJICodecManager.VideoSource;
//    import boofcv.android.VisualizeImageData;
//    import boofcv.android.gui.VideoDisplayActivity;
import boofcv.core.image.ConvertImage;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.GrayF32;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.followme.FollowMeMissionOperator;
import dji.sdk.products.Aircraft;


/**
 * Class for mobile remote controller.
 */
public class KcgRemoteControllerView extends RelativeLayout
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, View.OnFocusChangeListener,
        PresentableView, TextureView.SurfaceTextureListener {

    static String TAG = "KCG land";

    private Context ctx;

    private Tracker tracker = new Tracker();

    private Button btnDisableVirtualStick;
    private Button btnStart;
    private Button btnPause;
    private Button btnLand;

    // Codec for video live view
    protected DJICodecManager mCodecManager = null;

    private Bitmap droneIMG;
    protected TextureView mVideoSurface = null;
    private static final double ONE_METER_OFFSET = 0.00000899322;
    private double latitude = 0;
    private double longitude = 0;

    private FollowMeMissionOperator followMeMissionOperator = null;

    protected ImageView imgView;
    protected ImageView recIcon, audioIcon;
    protected TextView textView;
    protected TextView dataTv;
    protected TextView autonomous_mode_txt;
    protected TextView sawModeTextView;
    protected TextView dataTry;

    protected TextView audioText, audioError;
    private final String LOG_TAG = "VoiceRecognition";

    protected EditText textP, textI, textD, textT;
    protected Button btnTminus, btnTplus, btnPminus, btnPplus, btnIminus, btnIplus, btnDminus, btnDplus;
    protected Spinner spinner;
    protected String pid_type = "roll";

    private FlightController flightController;
    private SpeechToText speechToText;
    private boolean isViewVisible = false;

    private Controller cont;
    private float p = 0.5f, i = 0.02f, d = 0.01f, max_i = 1, t = -0.6f;//t fot vertical throttle


    public KcgRemoteControllerView(Context context) {
        super(context);
        ctx = context;
        init(context);
//            stitching = new Stitching();


        // Initialize the video feeder and set up the video data listener
//            VideoFeeder videoFeeder = VideoFeeder.getInstance();

//            VideoFeeder.getInstance()
//                    .getPrimaryVideoFeed()
//                    .addVideoDataListener(new VideoFeeder.VideoDataListener() {
//
//                @Override
//                public void onReceive(byte[] videoData, int size) {
//                    // Process the live video data here
//                    processLiveVideoData(videoData, size);
//                }
//            });

    }

//    public KcgRemoteControllerView(Context context, AttributeSet attributeSet) {
//        super(context, null);
//        ctx = context;
//        init(context);
//    }

    public KcgRemoteControllerView(Context context, @Nullable AttributeSet attributeSet) {
        super(context, null);
        ctx = context;
        init(context);
    }

    @NonNull
    @Override
    public String getHint() {
        return this.getClass().getSimpleName() + ".java";
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        isViewVisible = true;
        onResume(); // Call your onResume() logic
    }


    @Override
    protected void onDetachedFromWindow() {
        tearDownListeners();
        super.onDetachedFromWindow();
        isViewVisible = false;
        onPause(); // Call your onPause() logic
    }

    private void init(Context context) {

        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.view_kcg_rc, this, true);

        initUI();

        cont = new Controller(this);


        p = Float.parseFloat(textP.getText().toString());
        i = Float.parseFloat(textI.getText().toString());
        d = Float.parseFloat(textD.getText().toString());
        t = Float.parseFloat(textT.getText().toString());

        cont.initPIDs(p, i, d, max_i, "roll");
        cont.initPIDs(p, i, d, max_i, "pitch");
        cont.initPIDs(p, i, d, max_i, "throttle");
        cont.setDescentRate(t);


        boolean isErrorReport = ErrorReporter.getInstance().CheckError(context);
        if (isErrorReport) {
            showErrAvailable();
            return;
        }


    }

    private void initUI() {
        // init mVideoSurface
        try {
            mVideoSurface = findViewById(R.id.video_previewer_surface);

            textView = findViewById(R.id.textView);
            dataTv = findViewById(R.id.dataTv);
            dataTry = findViewById(R.id.data_try);
            sawModeTextView = findViewById(R.id.SawTarget);
            autonomous_mode_txt = findViewById(R.id.autonomous);
            autonomous_mode_txt.setTextColor(Color.rgb(255, 0, 0));
            imgView = findViewById(R.id.imgView);
            audioIcon = findViewById(R.id.audioIcon);
            recIcon = findViewById(R.id.recIcon);

            spinner = findViewById(R.id.static_spinner);
            String[] items = {"roll", "pitch", "throttle"};
            ArrayAdapter adapter = new ArrayAdapter<String>(this.ctx, android.R.layout.simple_list_item_1, items);

            spinner.setAdapter(adapter);

            textP = findViewById(R.id.setP_tv);
            textI = findViewById(R.id.setI_tv);
            textD = findViewById(R.id.setD_tv);
            textT = findViewById(R.id.setT_tv);

            btnTminus = findViewById(R.id.t_minus_btn);
            btnTplus = findViewById(R.id.t_plus_btn);
            btnPminus = findViewById(R.id.p_minus_btn);
            btnPplus = findViewById(R.id.p_plus_btn);
            btnIminus = findViewById(R.id.i_minus_btn);
            btnIplus = findViewById(R.id.i_plus_btn);
            btnDminus = findViewById(R.id.d_minus_btn);
            btnDplus = findViewById(R.id.d_plus_btn);

            btnTminus.setOnClickListener(this);
            btnTplus.setOnClickListener(this);
            btnPminus.setOnClickListener(this);
            btnPplus.setOnClickListener(this);
            btnIminus.setOnClickListener(this);
            btnIplus.setOnClickListener(this);
            btnDminus.setOnClickListener(this);
            btnDplus.setOnClickListener(this);

            textP.setOnFocusChangeListener(this);
            textI.setOnFocusChangeListener(this);
            textD.setOnFocusChangeListener(this);
            textT.setOnFocusChangeListener(this);

            btnDisableVirtualStick = findViewById(R.id.stop_btn);
            btnStart = findViewById(R.id.hover_btn);
            btnPause = findViewById(R.id.pause_btn);
            btnLand = findViewById(R.id.land_btn);

            audioText = findViewById(R.id.audioTextData);
            audioError = findViewById(R.id.audioErrorData);

            btnDisableVirtualStick.setOnClickListener(this);
            btnStart.setOnClickListener(this);
            btnLand.setOnClickListener(this);
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }

            btnPause.setOnClickListener(this);

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long l) {
                    String item = parent.getItemAtPosition(position).toString();
                    pid_type = item;

                    double[] pids = cont.getPIDs(pid_type);
                    textP.setText(String.format("%.2f", pids[0]));
                    textI.setText(String.format("%.4f", pids[1]));
                    textD.setText(String.format("%.2f", pids[2]));

                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            initializeSpeechToText();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeSpeechToText() {
        speechToText = new SpeechToText(this.getContext(), this::performActionOnResults, this::writeOnScreen, this::updateStartListening);
        speechToText.startListening();
    }

    public void updateStartListening() {
        audioIcon.setVisibility(View.VISIBLE);
    }

    public void onResume() {
        Log.i(LOG_TAG, "resume");
        if (!speechToText.resetSpeechRecognizer()) {
//            cont.finish();
        }
        if (isViewVisible) {

            speechToText.startListening();
        }
    }

    protected void onPause() {
        Log.i(LOG_TAG, "pause");
        if (!isViewVisible) {
            speechToText.stopListening();
        }
    }

//    @Override
    protected void onStop() {
        Log.i(LOG_TAG, "stop");
//        super.onStop();
        speechToText.destroyListening();
    }

    public void writeOnScreen(String text) {
        if (text.startsWith("Error:")) {
            audioError.setText(text.substring(7)); // Remove "Error: " prefix
        } else {
            audioText.setText(text);
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public void performActionOnResults(String text) {
        String message;
        switch (text) {
            case "abort":
            case "a bird":
            case "about":
            case "a boat":
                message = "Abort";
                break;
            case "landing":
                message = "Landing";
                break;
            case "take off":
                message = "Take Off";
                break;
            case "go to":
                message = "Go to";
                break;
            case "track me":
            case "talk me":
                message = "Track Me";
                break;
            case "panic":
            case "funny":
                message = "Panic";
                break;
            case "higher":
            case "tier":
                message = "Higher";
                break;
            case "lower":
                message = "Lower";
                break;
            case "stay": //our algorithm (pause)
                message = "Stay";
                break;
            case "camera up":
            case "camera app":
                message = "Camera up";
                break;
            case "camera down":
                message = "camera down";
                break;
            case "turn right":
                message = "turn right";
                break;
            case "turn left":
                message = "turn left";
                break;
            case "backward":
                message = "backward";
                break;
            case "forward":
                message = "forward";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
//        funcText.setText(message);
    }


    public void setRecIconVisibility(boolean isVisible) {
        if (isVisible) {
            recIcon.setVisibility(View.VISIBLE);
        } else {
            recIcon.setVisibility(View.INVISIBLE);
        }

    }

    private void tearDownListeners() {

        //        screenJoystickLeft.setJoystickListener(null);
        //        screenJoystickRight.setJoystickListener(null);

    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
    }

    @Override
    public int getDescription() {
        return R.string.component_listview_kcg_remote_controll;
    }

    public void setVideoData(byte[] videoBuffer, int size) {
        if (mCodecManager != null) {
            mCodecManager.sendDataToDecoder(videoBuffer, size);
        }
    }

    public void setDebugData(String debugData) {
        dataTv.setText(debugData);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        if (mCodecManager == null) {
            showToast("" + width + "," + height);
            mCodecManager = new DJICodecManager(ctx, surface, width, height);

        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG, "onSurfaceTextureDestroyed");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }

        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        droneIMG = mVideoSurface.getBitmap();

        cont.setBitmapFrame(droneIMG);
        cont.setBitmapFrame(droneIMG, tracker);
        imgView.setImageBitmap(droneIMG);
    }


    public void onDroneCompletionCallback(final String text) {
        //        ctx.runOnUiThread(new Runnable() {
        //            public void run() {
        //                sawModeTextView.setText(text);
        //            }
        //        });
    }

    //------------------------------
    // ui code
    public void doToast(String text) {
        showToast(text);
    }


    public void disable(FlightController flightController) {
        autonomous_mode_txt.setText("not autonomous");
        autonomous_mode_txt.setTextColor(Color.rgb(255, 0, 0));


        //send stop to aircraft
        cont.stopOnPlace();

        flightController.setVirtualStickModeEnabled(false, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {
                    ToastUtils.setResultToToast("Virtual sticks disabled!");
                }
            }
        });

    }


    @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
    public void onClick(View v) {
        FlightController flightController = ModuleVerificationUtil.getFlightController();
        if (flightController == null) {
            return;
        }
        flightController.setYawControlMode(YawControlMode.ANGLE);
        flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
        flightController.setYawControlMode(YawControlMode.ANGLE);
        flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
        flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
        switch (v.getId()) {
            case R.id.stop_btn:
                flightController.getFlightAssistant().setLandingProtectionEnabled(true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) showToast("" + djiError);
                        else showToast("Landing protection DISABLED!");
                    }
                });
                disable(flightController);

                textP.setEnabled(true);
                textI.setEnabled(true);
                textD.setEnabled(true);
                textT.setEnabled(true);
                FlightControll_v2.flag = false;
                break;
            case R.id.hover_btn:
            case R.id.land_btn:
//sart
                //    FlightControll_v2.flag = true;
                textP.setEnabled(false);
                textI.setEnabled(false);
                textD.setEnabled(false);
                textT.setEnabled(false);

                flightController.getFlightAssistant().setLandingProtectionEnabled(false, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) showToast("" + djiError);
                        else showToast("Landing protection DISABLED!");
                    }
                });
                flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError == null) {
                            showToast("Virtual sticks enabled!");
                            cont.allowControl();
                        } else showToast("" + djiError);
                    }
                });


                try {
                    if (v.getId() == R.id.land_btn) {
                        FlightControll_v2.flag = true;
//                            float descentRate = Float.parseFloat(textT.getText().toString());
//                            if (descentRate > 0) {
//                                descentRate = descentRate * -1;
//                            }

//                            cont.setDescentRate(descentRate);
                    } else {
                        cont.setDescentRate(0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                autonomous_mode_txt.setText("autonomous");
                autonomous_mode_txt.setTextColor(Color.rgb(0, 255, 0));
                break;

            //end
            case R.id.pause_btn:
                try {
                    if (DJISampleApplication.getProductInstance() instanceof Aircraft) {
                        flightController = ((Aircraft) DJISampleApplication.getProductInstance()).getFlightController();
                    }
                    if (flightController == null) {
                        if (ModuleVerificationUtil.isFlightControllerAvailable()) {
                            flightController = DJISampleApplication.getAircraftInstance().getFlightController();
                        }
                    }
                    if (flightController != null) {
                        flightController.setStateCallback(new FlightControllerState.Callback() {
                            @Override
                            public void onUpdate(@NonNull FlightControllerState flightControllerState) {
                                latitude = flightControllerState.getAircraftLocation().getLatitude();
                                longitude = flightControllerState.getAircraftLocation().getLongitude();
                            }
                        });
                    }

                    VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(new VideoFeeder.VideoDataListener() {
                        // Create a buffer to accumulate video data
                        byte[] videoBuffer;
                        int receivedDataSize = 0;

                        @Override
                        public void onReceive(byte[] videoData, int size) {

                            dataTry.setText("hello");

                            mCodecManager.sendDataToDecoder(videoData, size);
                            dataTry.setText("hello------------");

                            int frameWidth = mCodecManager.getVideoWidth();
                            int frameHeight = mCodecManager.getVideoHeight();
                            int EXPECTED_FRAME_SIZE = frameWidth * frameHeight;
                            videoBuffer = new byte[EXPECTED_FRAME_SIZE];

                            if (size <= EXPECTED_FRAME_SIZE) {
                                System.arraycopy(videoData, 0, videoBuffer, receivedDataSize, size);
                                receivedDataSize += size;
                                // Check if we have received enough data to form a complete frame
                                if (receivedDataSize >= EXPECTED_FRAME_SIZE) {
                                    // Process the complete frame (e.g., convert to GrayF32)
                                    visualizeVideoFrame(videoBuffer, frameWidth, frameHeight);

                                    GrayF32 frame = convertVideoDataToGrayF32(videoBuffer, frameWidth, frameHeight);
                                    dataTry.setText("hello111111");
                                    Planar<GrayF32> planarFrame = new Planar<>(GrayF32.class, frameWidth, frameHeight, 1);
                                    dataTry.setText("hello222222");

                                    planarFrame.bands[0] = frame.clone(); // Clone the GrayF32 frame
                                    dataTry.setText("hello33333");
                                    double[] vec = tracker.process(planarFrame);
                                    dataTry.setText("hello44444");

                                    System.out.println("dx: " + vec[0] + " dy: " + vec[1]);
                                    dataTry.setText("dx: " + vec[0] + " dy: " + vec[1]);


                                    updateUIWithProcessedFrame(frame);

                                    // Reset the buffer for the next frame
                                    receivedDataSize = 0;
                                }

                                // Handle a size mismatch, which might indicate an issue with the data
                                Log.e(TAG, "Received data size does not match the expected frame size.");
                            }

                            // Visualize the frame (for debugging purposes)
//                                visualizeVideoFrame(videoBuffer, frameWidth, frameHeight);

                            // Convert the video data to GrayF32 format
//                                GrayF32 frame = convertVideoDataToGrayF32(videoBuffer, frameWidth, frameHeight);
//                                dataTry.setText("hello111111");

                            // Create a Planar<GrayF32> image from the GrayF32 frame
//                                Planar<GrayF32> planarFrame = new Planar<>(GrayF32.class, frameWidth, frameHeight, 1);
//                                dataTry.setText("hello222222");
//
//                                planarFrame.bands[0] = frame.clone(); // Clone the GrayF32 frame
//                                dataTry.setText("hello33333");

                            // Perform your stitching or other processing here using the GrayF32 frame
//                                int[] vec = stitching.process(planarFrame);
//                                dataTry.setText("hello44444");

                            //------------
//                            int[] vec = new int[]{0, 0};

//                            int dx = vec[0];
//                            int dy = vec[1];
//                            System.out.println("dx: " + vec[0] + " dy: " + vec[1]);
//                            dataTry.setText("dx: " + vec[0] + " dy: " + vec[1]);
//                            latitude = latitude + dx * ONE_METER_OFFSET; //dx
//                            longitude = longitude + dy * ONE_METER_OFFSET; //dy
//                            LocationCoordinate2D newLocation = new LocationCoordinate2D(latitude, longitude);
//                            followMeMissionOperator.updateFollowingTarget(newLocation, djiError1 -> {
//                                try {
//                                    Thread.sleep(1500);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
//                            });
//                                updateUIWithProcessedFrame(frame);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;


            //--------- set vertical throttle
            case R.id.t_minus_btn:
                try {
                    t = Float.parseFloat(textT.getText().toString());
                    t -= 0.01f;
                    textT.setText(String.format("%.2f", t));
                    cont.setDescentRate(t);
                } catch (NumberFormatException e) {
                    showToast("not float");
                }
                break;

            case R.id.t_plus_btn:
                try {
                    t = Float.parseFloat(textT.getText().toString());
                    t += 0.01f;
                    textT.setText(String.format("%.2f", t));
                    cont.setDescentRate(t);
                } catch (NumberFormatException e) {
                    showToast("not float");
                }
                break;
            //-------- set P ----------
            case R.id.p_minus_btn:
                try {
                    p = Float.parseFloat(textP.getText().toString());
                    p -= 0.01f;
                    textP.setText(String.format("%.2f", p));
                    cont.initPIDs(p, i, d, max_i, pid_type);
                } catch (NumberFormatException e) {
                    showToast("not float");
                }
                break;

            case R.id.p_plus_btn:
                try {
                    p = Float.parseFloat(textP.getText().toString());
                    p += 0.01f;
                    textP.setText(String.format("%.2f", p));
                    cont.initPIDs(p, i, d, max_i, pid_type);
                } catch (NumberFormatException e) {
                    showToast("not float");
                }
                break;

            //-------- set I ----------
            case R.id.i_minus_btn:
                try {
                    i = Float.parseFloat(textI.getText().toString());
                    i -= 0.0001f;
                    textI.setText(String.format("%.4f", i));
                    cont.initPIDs(p, i, d, max_i, pid_type);
                } catch (NumberFormatException e) {
                    showToast("not float");
                }
                break;

            case R.id.i_plus_btn:
                try {
                    i = Float.parseFloat(textI.getText().toString());
                    i += 0.0001f;
                    textI.setText(String.format("%.4f", i));
                    cont.initPIDs(p, i, d, max_i, pid_type);
                } catch (NumberFormatException e) {
                    showToast("not float");
                }
                break;

            //-------- set D ----------
            case R.id.d_minus_btn:
                try {
                    d = Float.parseFloat(textD.getText().toString());
                    d -= 0.01f;
                    textD.setText(String.format("%.2f", d));
                    cont.initPIDs(p, i, d, max_i, pid_type);
                } catch (NumberFormatException e) {
                    showToast("not float");
                }
                break;

            case R.id.d_plus_btn:
                try {
                    d = Float.parseFloat(textD.getText().toString());
                    d += 0.01f;
                    textD.setText(String.format("%.2f", d));
                    cont.initPIDs(p, i, d, max_i, pid_type);
                } catch (NumberFormatException e) {
                    showToast("not float");
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            userFinishedEditing();
        }
    }

    private void userFinishedEditing() {
        try {
            t = Float.parseFloat(textT.getText().toString());
            p = Float.parseFloat(textP.getText().toString());
            i = Float.parseFloat(textI.getText().toString());
            d = Float.parseFloat(textD.getText().toString());
        } catch (NumberFormatException e) {
            showToast("not float");
        }

        cont.initPIDs(p, i, d, max_i, pid_type);
        cont.setDescentRate(t);
    }

    public void showErrAvailable() {
        AlertDialog.Builder alertDialogBuilder =
                new AlertDialog.Builder(ctx);
        //new AlertDialog.Builder(activity,android.R.style.Theme_Holo_Light_Dialog_NoActionBar_MinWidth);
        alertDialogBuilder.setTitle("Crash Log Available");
        alertDialogBuilder.setMessage("What to you prefer to do with crash log ? ");
        alertDialogBuilder.setPositiveButton("Send",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        ErrorReporter.getInstance().CheckErrorAndSendMail(ctx);
                    }
                });
        alertDialogBuilder.setNeutralButton("Delete",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        ErrorReporter.getInstance().deleteAllReports();
                        //                        onResume();
                    }
                });

        alertDialogBuilder.setNegativeButton("Later",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        //                        onResume();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();

        alertDialog.setCanceledOnTouchOutside(false);

        alertDialog.show();
    }

    private void processLiveVideoData(byte[] videoData, int width, int height) {
        // Convert the video data to a format suitable for your stitching technique
        GrayF32 frame = convertVideoDataToGrayF32(videoData, width, height);

        // Perform the stitching technique or any other video processing here
        // You can use the frame for your processing

        // Update the UI with the processed frame if necessary
        updateUIWithProcessedFrame(frame);
    }

    private GrayF32 convertVideoDataToGrayF32(byte[] videoData, int width, int height) {
        try {


            // Implement the conversion of video data to GrayF32 format here
            // This will depend on the format of the video data and the libraries you're using
            // You may need to decode the video data if it's in a compressed format

            // Return the processed frame as a GrayF32 object
            // Make sure to handle the conversion appropriately based on your video format
            // For example, you can use BoofCV to process the data.

            GrayF32 frame = new GrayF32(width, height);

            // Copy the video data to the GrayF32 image. The conversion depends on the video data format.
            // You'll need to adjust this part based on the actual data format you receive.
            for (int i = 0; i < width * height; i++) {
                // Assuming that the video data contains grayscale pixel values
                float pixelValue = (float) (videoData[i] & 0xFF) / 255.0f;
                System.out.println("i:    " + i);
                frame.set(i % width, i / width, pixelValue);
            }

            return frame;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }


//            // Implement the conversion of video data to GrayF32 format here
//            // This will depend on the format of the video data and the libraries you're using
//            // You may need to decode the video data if it's in a compressed format
//
//            // Return the processed frame as a GrayF32 object
//            // Make sure to handle the conversion appropriately based on your video format
//            // For example, you can use BoofCV to process the data.
//
//            // Create an image buffer to store the video data
//            GrayU8 videoImage = new GrayU8(width, height);
//            // Convert the video data to GrayU8 format (adjust this part as needed)
//            ConvertImage.byteArrayToGray(videoData, videoImage);
//
//            // Convert the GrayU8 image to GrayF32
//            GrayF32 frame = new GrayF32(width, height);
//            ConvertImage.convert(videoImage, frame);
//
//            return frame;
    }

    private void visualizeVideoFrame(byte[] videoData, int width, int height) {
        // Assuming you're working in an Android application
        // You can create a Bitmap from the video data and display it in an ImageView or log it

        // Create a Bitmap from the video data
        Bitmap frameBitmap = createBitmapFromVideoData(videoData, width, height);

        // Display the frame in an ImageView
        imgView.setImageBitmap(frameBitmap);

        // Log the frame data for debugging (you might want to do this selectively or use a logger)
        Log.d(TAG, "Received frame: " + frameBitmap.getWidth() + "x" + frameBitmap.getHeight());
    }

    private Bitmap createBitmapFromVideoData(byte[] videoData, int width, int height) {
        // Implement the conversion from byte array to Bitmap based on your video data format
        // You'll need to decode or process the video data to create a Bitmap.

        // Here's a simple example if the video data represents grayscale values:

        int[] colors = new int[width * height];
        for (int i = 0; i < width * height; i++) {
            int grayValue = videoData[i] & 0xFF;  // Assuming it's a grayscale value
            colors[i] = Color.rgb(grayValue, grayValue, grayValue);
        }

        return Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888);
    }

    private void updateUIWithProcessedFrame(GrayF32 frame) {
        // Update your UI with the processed frame, e.g., display it on a TextureView or ImageView
        // You can set the processed frame to your image view like this:
        // imgView.setImageBitmap(convertGrayF32ToBitmap(frame));

        // Example code to update an ImageView:
        imgView.setImageBitmap(convertGrayF32ToBitmap(frame));

        // Example code to use the displacement values:
//            int dx = displacement[0]; // Displacement in the x direction
//            int dy = displacement[1]; // Displacement in the y direction
    }


    public Planar<GrayF32> convertYUVToRGB(byte[] decodedData, int width, int height) {
        // Create a Planar<GrayF32> image with 3 bands (Red, Green, Blue)
        Planar<GrayF32> rgbImage = new Planar<>(GrayF32.class, width, height, 3);

        // Assuming YUV420p format (common in many video codecs)
        int ySize = width * height;
        int uvSize = (width * height) / 4; // U and V components

        for (int i = 0; i < ySize; i++) {
            int y = decodedData[i] & 0xFF;
            int u = decodedData[ySize + (i / 4)] & 0xFF;
            int v = decodedData[ySize + uvSize + (i / 4)] & 0xFF;

            // Perform YUV to RGB conversion
            int c = y - 16;
            int d = u - 128;
            int e = v - 128;

            int r = (298 * c + 409 * e + 128) >> 8;
            int g = (298 * c - 100 * d - 208 * e + 128) >> 8;
            int b = (298 * c + 516 * d + 128) >> 8;

            // Set the RGB values in the Planar image
            rgbImage.bands[0].set(i % width, i / width, r);
            rgbImage.bands[1].set(i % width, i / width, g);
            rgbImage.bands[2].set(i % width, i / width, b);
        }

        return rgbImage;
    }

    //         If needed, you can add a method to convert a GrayF32 image to a Bitmap
//         You may need this for displaying the processed frame in an ImageView
    private Bitmap convertGrayF32ToBitmap(GrayF32 frame) {

        // Create a Bitmap from the GrayF32 image
        Bitmap bitmap = Bitmap.createBitmap(frame.width, frame.height, Bitmap.Config.ARGB_8888);

        // Convert the GrayF32 image to the Bitmap (adjust this part as needed)
        for (int y = 0; y < frame.height; y++) {
            for (int x = 0; x < frame.width; x++) {
                int pixelValue = (int) (255 * frame.get(x, y));
                int color = Color.rgb(pixelValue, pixelValue, pixelValue);
                bitmap.setPixel(x, y, color);
            }
        }

        return bitmap;
    }

}
