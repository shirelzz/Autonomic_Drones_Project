package com.dji.sdk.sample.demo.kcgremotecontroller;

import static androidx.core.content.ContextCompat.getSystemService;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.location.Location;
import android.location.LocationManager;
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
import com.dji.sdk.sample.demo.speechToText.FlightCommandUI;
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
import dji.common.mission.followme.FollowMeHeading;
import dji.common.mission.followme.FollowMeMission;
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
    private FlightCommandUI UI_commands;

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
    }

//    public KcgRemoteControllerView(Context context, AttributeSet attributeSet) {
//        super(context, null);
//        ctx = context;
//        init(context);
//    }

    public KcgRemoteControllerView(Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
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

        UI_commands = new FlightCommandUI(cont.getLog());


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

        switch (text) {
            case "abort":
            case "a bird":
            case "about":
            case "a boat":

                break;
            case "landing":
                hoverLandBtnFunc(R.id.land_btn);
                break;
            case "take off":
                UI_commands.takeoff();
                break;
            case "go to":

                break;
            case "track me":
            case "talk me":

                break;
            case "panic":
            case "funny":

                break;
            case "higher":
            case "tier":

                break;
            case "lower":

                break;
            case "stay": //our algorithm (pause) replace with pose?
                pauseBtnFunc();
                break;
            case "stop":
                stopBtnFunc();
                break;
            case "hover": //Asaf algorithm
            case "over":
                hoverLandBtnFunc(R.id.hover_btn);
                break;
            case "camera up":
            case "camera app":

                break;
            case "camera down":
                break;
            case "turn right":
                UI_commands.turn_right();

                break;
            case "turn left":
                UI_commands.turn_left();

                break;
            case "backward":
                UI_commands.backward();

                break;
            case "forward":
                UI_commands.forward();

                break;
            case "follow me":
                followMeMissionFunc();
            default:
                break;
        }
    }

    public void followMeMissionFunc() {
        followMeMissionOperator = MissionControl.getInstance().getFollowMeMissionOperator();

        followMeMissionOperator.startMission(new FollowMeMission(FollowMeHeading.TOWARD_FOLLOW_POSITION,
                latitude + 5 * ONE_METER_OFFSET, longitude + 5 * ONE_METER_OFFSET, 30f
        ), new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                ToastUtils.setResultToToast(djiError != null ? djiError.getDescription() : "start success");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int cnt = 0;
                        while (cnt < 100) {
                            latitude = latitude + 5 * ONE_METER_OFFSET;
                            longitude = longitude + 5 * ONE_METER_OFFSET;
                            LocationCoordinate2D newLocation = new LocationCoordinate2D(latitude, longitude);
                            followMeMissionOperator.updateFollowingTarget(newLocation, djiError1 -> {
                                try {
                                    Thread.sleep(1500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            });
                            cnt++;
                        }
                    }
                }).start();
            }
        });
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
//        cont.setBitmapFrame(droneIMG, tracker);
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

    public void pauseBtnFunc() {
        setFlightControlSetup();
        if (flightController == null) {
            return;
        }
        try {
            if (!tracker.isPaused()) {
                // Pause tracking and store the current location
                flightController.setStateCallback(new FlightControllerState.Callback() {
                    @Override
                    public void onUpdate(@NonNull FlightControllerState flightControllerState) {
                        latitude = flightControllerState.getAircraftLocation().getLatitude();
                        longitude = flightControllerState.getAircraftLocation().getLongitude();
                    }
                });

                tracker.pause();
                tracker.setFlightController(flightController);
                tracker.setInitialLocation(latitude, longitude);

            } else {
                // Resume tracking
                tracker.resume();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void hoverLandBtnFunc(int id) {
        setFlightControlSetup();
        if (flightController == null) {
            return;
        }
//start
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
            if (id == R.id.land_btn) {
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
    }

    public void stopBtnFunc() {
        setFlightControlSetup();
        if (flightController == null) {
            return;
        }
        ;
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

    }

    public void setFlightControlSetup() {
        if (flightController == null) {
            flightController = ModuleVerificationUtil.getFlightController();
            if (flightController == null) {
                return;
            }
        }

        flightController.setYawControlMode(YawControlMode.ANGLE);
        flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
        flightController.setYawControlMode(YawControlMode.ANGLE);
        flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
        flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
    }

    @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
    public void onClick(View v) {
        setFlightControlSetup();
        switch (v.getId()) {
            case R.id.stop_btn:
                stopBtnFunc();
                break;
            case R.id.hover_btn:
            case R.id.land_btn:
                hoverLandBtnFunc(v.getId());
                break;

            case R.id.pause_btn:
                pauseBtnFunc();
                break;

            case R.id.follow_me:
                followMeMissionFunc();
                break;

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

}
