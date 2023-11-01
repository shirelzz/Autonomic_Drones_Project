    package com.dji.sdk.sample.demo.kcgremotecontroller;

    import android.annotation.SuppressLint;
    import android.app.Service;
    import android.content.Context;
    import android.content.DialogInterface;
    import android.graphics.Bitmap;
    import android.graphics.BitmapFactory;
    import android.graphics.Color;
    import android.graphics.SurfaceTexture;
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
    import androidx.appcompat.app.AlertDialog;

    import com.dji.sdk.sample.R;
    import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
    import com.dji.sdk.sample.internal.utils.ToastUtils;
    import com.dji.sdk.sample.internal.view.PresentableView;

    import boofcv.struct.image.GrayF32;
    import boofcv.struct.image.Planar;
    import dji.common.error.DJIError;
    import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
    import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
    import dji.common.flightcontroller.virtualstick.VerticalControlMode;
    import dji.common.flightcontroller.virtualstick.YawControlMode;
    import dji.common.model.LocationCoordinate2D;
    import dji.common.util.CommonCallbacks;
    import dji.sdk.camera.VideoFeeder;
    import dji.sdk.codec.DJICodecManager;
    import dji.sdk.flightcontroller.FlightController;
    import com.dji.sdk.sample.demo.stitching.Stitching;
    import static com.dji.sdk.sample.internal.utils.ToastUtils.showToast;

    import dji.sdk.codec.DJICodecManager.VideoSource;
//    import boofcv.android.VisualizeImageData;
//    import boofcv.android.gui.VideoDisplayActivity;
    import boofcv.core.image.ConvertImage;
    import boofcv.struct.image.GrayU8;
    import boofcv.struct.image.ImageBase;
    import boofcv.struct.image.GrayF32;


    /**
     * Class for mobile remote controller.
     */
    public class KcgRemoteControllerView extends RelativeLayout
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, View.OnFocusChangeListener,
            PresentableView , TextureView.SurfaceTextureListener {

        static String TAG = "KCG land";

        private Context ctx;

        private Stitching stitching;

        private Button btnDisableVirtualStick;
        private Button btnStart;
        private Button btnPause;
        private Button btnLand;

        // Codec for video live view
        protected DJICodecManager mCodecManager = null;

        private Bitmap droneIMG;
        protected TextureView mVideoSurface = null;

        private double latitude = 0;
        private double longitude = 0;

        protected ImageView imgView;
        protected ImageView recIcon;
        protected TextView textView;
        protected TextView dataTv;
        protected TextView autonomous_mode_txt;
        protected TextView sawModeTextView;

        protected EditText textP, textI, textD, textT;
        protected Button btnTminus,btnTplus,btnPminus,btnPplus,btnIminus,btnIplus,btnDminus,btnDplus;
        protected Spinner spinner;
        protected String pid_type = "roll";

        private Controller cont;
        private float p = 0.5f, i = 0.02f, d = 0.01f, max_i = 1, t = -0.6f;//t fot vertical throttle



        public KcgRemoteControllerView(Context context) {
            super(context);
            ctx = context;
            init(context);
            stitching = new Stitching(null);


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

        @NonNull
        @Override
        public String getHint() {
            return this.getClass().getSimpleName() + ".java";
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
        }


        @Override
        protected void onDetachedFromWindow() {
            tearDownListeners();
            super.onDetachedFromWindow();
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

            cont.initPIDs(p, i, d, max_i,"roll");
            cont.initPIDs(p, i, d, max_i,"pitch");
            cont.initPIDs(p, i, d, max_i,"throttle");
            cont.setDescentRate(t);




            boolean isErrorReport = ErrorReporter.getInstance().CheckError(context);
            if (isErrorReport) {
                showErrAvailable();
                return;
            }


        }

    private void initUI() {
        // init mVideoSurface
        mVideoSurface = findViewById(R.id.video_previewer_surface);

        textView = findViewById(R.id.textView);
        dataTv = findViewById(R.id.dataTv);
        sawModeTextView = findViewById(R.id.SawTarget);
        autonomous_mode_txt = findViewById(R.id.autonomous);
        autonomous_mode_txt.setTextColor(Color.rgb(255, 0, 0));
        imgView = findViewById(R.id.imgView);
        recIcon = findViewById(R.id.recIcon);

        spinner = findViewById(R.id.static_spinner);
        String[] items = {"roll","pitch","throttle"};
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
    }

    public void setRecIconVisibility(boolean isVisible){
            if (isVisible){
                recIcon.setVisibility(View.VISIBLE);
            }
            else{
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

        public void setDebugData(String debugData){
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
        public void doToast(String text){
            showToast(text);
        }


        public void disable(FlightController flightController){
            autonomous_mode_txt.setText("not autonomous");
            autonomous_mode_txt.setTextColor(Color.rgb(255,0,0));


            //send stop to aircraft
            cont.stopOnPlace();

            flightController.setVirtualStickModeEnabled(false, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if(djiError == null) {
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
                    try{

                        VideoFeeder.getInstance()
                                .getPrimaryVideoFeed()
                                .addVideoDataListener(new VideoFeeder.VideoDataListener() {
                                    @Override
                                    public void onReceive(byte[] videoData, int size) {

                                        int frameWidth = 3840;
                                        int frameHeight = 2160;
                                        // Convert the video data to GrayF32 format
                                        GrayF32 frame = convertVideoDataToGrayF32(videoData, frameWidth, frameHeight);

                                        // Create a Planar<GrayF32> image from the GrayF32 frame
                                        Planar<GrayF32> planarFrame = new Planar<>(GrayF32.class, frameWidth, frameHeight, 1);
                                        planarFrame.bands[0] = frame.clone(); // Clone the GrayF32 frame

                                        // Perform your stitching or other processing here using the GrayF32 frame
                                        int[] vec = stitching.process(planarFrame);
                                        System.out.println("dx: " + vec[0] + " dy: " + vec[1]);

                                        // Update your UI with the processed frame if necessary
                                        updateUIWithProcessedFrame(frame);
                                    }
                                });

//                        // Initialize the video feeder and set up the video data listener
//                        VideoFeeder videoFeeder = VideoFeeder.getInstance();
//
////                        VideoSource videoSource = videoFeeder.getPrimaryVideoFeed().getSource();
//                        int frameWidth = 3840; // videoSource.getVideoSourceSize().width;
//                        int frameHeight = 2160; // videoSource.getVideoSourceSize().height;
//
//                        VideoFeeder.getInstance()
//                        .getPrimaryVideoFeed()
//                        .addVideoDataListener(new VideoFeeder.VideoDataListener() {
//
//                            @Override
//                            public void onReceive(byte[] videoData, int size) {
//                                // Process the live video data here
//                                processLiveVideoData(videoData, frameWidth, frameHeight);
//                            }
//                        });





//                        // Assuming you have a videoDataListener set up to receive the live video feed.
//                        VideoFeeder.VideoDataListener videoDataListener = new VideoFeeder.VideoDataListener() {
//                            @Override
//                            public void onReceive(byte[] videoData, int size) {
//                                // Convert the video data to a format suitable for BoofCV processing (e.g., convert to GrayF32).
//                                GrayF32 frame = convertVideoDataToGrayF32(videoData);
//
//                                // Perform BoofCV stitching or feature-based processing on the frame.
//                                // You should implement the stitching logic here using BoofCV.
//
//                                // Display the processed frame in real-time on your user interface.
//                                // Update the TextureView, SurfaceView, or any UI element.
//                                updateUIWithProcessedFrame(frame);
//                            }
//                        };
//
//                        // Add the video data listener to receive live video data.
//                        VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(videoDataListener);





//                        Bitmap loadedBitmap1 = BitmapFactory.decodeFile("images/image1.JPG");
//                        Planar<GrayF32> image1 = Stitching.convert(loadedBitmap1);
//
//                        Bitmap loadedBitmap2 = BitmapFactory.decodeFile("images/image2.JPG");
//                        Planar<GrayF32> image2 = Stitching.convert(loadedBitmap2);
//
//                        Stitching stitch = new Stitching(image1);
//                        int[] vec = stitch.process(image2);
//                        System.out.println("dx: " + vec[0] + " dy: " + vec[1]);


                        // Assuming you have a videoDataListener set up to receive the live video feed.
//                        VideoFeeder.VideoDataListener videoDataListener = videoData -> {
//                            // Convert the video data to a format suitable for BoofCV processing (e.g., convert to GrayF32).
//                            GrayF32 frame = convertVideoDataToGrayF32(videoData);
//
//                            // Perform BoofCV stitching or feature-based processing on the frame.
//                            // You should implement the stitching logic here using BoofCV.
//
//                            // Display the processed frame in real-time on your user interface.
//                            // Update the TextureView, SurfaceView, or any UI element.
//                            updateUIWithProcessedFrame(frame);
//                        };

//                        latitude = latitude + 5 * ONE_METER_OFFSET; //dx
//                        longitude = longitude + 5 * ONE_METER_OFFSET; //dy
//                        LocationCoordinate2D newLocation = new LocationCoordinate2D(latitude, longitude);
//                        followMeMissionOperator.updateFollowingTarget(newLocation, djiError1 -> {
//                            try {
//                                Thread.sleep(1500);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        });
//                        Stitching stitch = new Stitching();
//                        while (video.hasNext()) {
                //            count++;
                //            Planar<GrayF32> frame = video.next();
                //            int[] vec = stitch.process(frame);
                //            System.out.println("dx: " + vec[0] + " dy: " + vec[1]);
                //            if (count % 100 == 0) {
                //                System.out.println("reset");
                //                stitch.reset(frame);
                //            }
                //        }



                    }catch(Exception e){
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
                        cont.initPIDs(p, i, d, max_i,pid_type);
                    } catch (NumberFormatException e) {
                        showToast("not float");
                    }
                    break;

                case R.id.p_plus_btn:
                    try {
                        p = Float.parseFloat(textP.getText().toString());
                        p += 0.01f;
                        textP.setText(String.format("%.2f", p));
                        cont.initPIDs(p, i, d, max_i,pid_type);
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
                        cont.initPIDs(p, i, d, max_i,pid_type);
                    } catch (NumberFormatException e) {
                        showToast("not float");
                    }
                    break;

                case R.id.i_plus_btn:
                    try {
                        i = Float.parseFloat(textI.getText().toString());
                        i += 0.0001f;
                        textI.setText(String.format("%.4f", i));
                        cont.initPIDs(p, i, d, max_i,pid_type);
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
                        cont.initPIDs(p, i, d, max_i,pid_type);
                    } catch (NumberFormatException e) {
                        showToast("not float");
                    }
                    break;

                case R.id.d_plus_btn:
                    try {
                        d = Float.parseFloat(textD.getText().toString());
                        d += 0.01f;
                        textD.setText(String.format("%.2f", d));
                        cont.initPIDs(p, i, d, max_i,pid_type);
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

            cont.initPIDs(p, i, d, max_i,pid_type);
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
                frame.set(i % width, i / width, pixelValue);
            }

            return frame;

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

        // If needed, you can add a method to convert a GrayF32 image to a Bitmap
        // You may need this for displaying the processed frame in an ImageView
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
