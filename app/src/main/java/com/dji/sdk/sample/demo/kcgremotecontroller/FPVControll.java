package com.dji.sdk.sample.demo.kcgremotecontroller;

/*
here I want to centralize all access to fpv data and controll
 */

import android.util.Log;

import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.utils.CallbackHandlers;
import com.dji.sdk.sample.internal.utils.ToastUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import dji.common.battery.BatteryState;
import dji.common.camera.ResolutionAndFrameRate;
import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.adsb.AirSenseSystemInformation;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.gimbal.CapabilityKey;
import dji.common.gimbal.GimbalMode;
import dji.common.gimbal.GimbalState;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.common.util.DJIParamCapability;
import dji.common.util.DJIParamMinMaxCapability;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;
import dji.sdk.sdkmanager.DJISDKManager;

import static com.dji.sdk.sample.internal.controller.DJISampleApplication.getProductInstance;

public class FPVControll implements CommonCallbacks.CompletionCallback{

    //data


    private FlightControlData flightControlData = new FlightControlData(0,0,0,0);
    private Map<String,Double> droneTelemetry = new HashMap<>();

    private VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;

    private Controller controller;

    private Gimbal gimbal = null;
    private int currentGimbalId = 0;
    private float prevDegree = -1000;

    private int maxGimbalDegree = 1000;
    private int minGimbalDegree = -1000;

    private boolean isCameraRecording = false;

    private boolean controllAllowed = false;

    //constructor

    public FPVControll(final Controller controller){

        this.controller = controller;

        //init data listenres
        initStateListeners();

        //init video feed
        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {
            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                controller.setVideoData(videoBuffer,size);
            }
        };

        //gimbal init

        gimbal = getGimbalInstance();
        if (gimbal != null) {
            getGimbalInstance().setMode(GimbalMode.YAW_FOLLOW, new CallbackHandlers.CallbackToastHandler());
        } else {
            ToastUtils.setResultToToast("Ark: Gimbal failed");
        }

        if (!isFeatureSupported(CapabilityKey.ADJUST_PITCH)) {
            ToastUtils.setResultToToast("Ark: Gimbal pitch not supported");
        }

        Object key = CapabilityKey.ADJUST_PITCH;
        minGimbalDegree = ((DJIParamMinMaxCapability) (gimbal.getCapabilities().get(key))).getMin().intValue();
        maxGimbalDegree = ((DJIParamMinMaxCapability) (gimbal.getCapabilities().get(key))).getMax().intValue();

        rotateGimbalToDegree(minGimbalDegree);

        FlightController flightController = DJISampleApplication.getAircraftInstance().getFlightController();
        flightController.getFlightAssistant().setLandingProtectionEnabled(false, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {

            }
        });

        flightController.getFlightAssistant().setActiveObstacleAvoidanceEnabled(false, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {

            }
        });

        flightController.getFlightAssistant().setCollisionAvoidanceEnabled(false, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {

            }
        });

    }

    public int getMaxGimbalDegree(){return maxGimbalDegree;}
    public int getMinGimbalDegree(){return minGimbalDegree;}


    //functions

    private void initStateListeners(){
        if (ModuleVerificationUtil.isFlightControllerAvailable()) {

            DJISampleApplication.getAircraftInstance().getFlightController().setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(FlightControllerState flightControllerState) {

                    //get drone location
                    droneTelemetry.put("lat",flightControllerState.getAircraftLocation().getLatitude());
                    droneTelemetry.put("lon",flightControllerState.getAircraftLocation().getLongitude());
                    droneTelemetry.put("alt",(double)flightControllerState.getAircraftLocation().getAltitude());

                    droneTelemetry.put("HeadDirection",(double)flightControllerState.getAircraftHeadDirection());

                    //get drone velocity
                    droneTelemetry.put("velX",(double)flightControllerState.getVelocityX());
                    droneTelemetry.put("velY",(double)flightControllerState.getVelocityY());
                    droneTelemetry.put("velZ",(double)flightControllerState.getVelocityZ());

                    //get drone attitude
                    droneTelemetry.put("yaw",flightControllerState.getAttitude().yaw);
                    droneTelemetry.put("pitch",flightControllerState.getAttitude().pitch);
                    droneTelemetry.put("roll",flightControllerState.getAttitude().roll);

                    //get another interesting data
                    droneTelemetry.put("UsAlt",(double)flightControllerState.getUltrasonicHeightInMeters());
//                    droneTelemetry.put("UsAltErr",(double)flightControllerState.isE);


//                  flightControllerState.isUltrasonicBeingUsed()
//                  flightControllerState.isVisionPositioningSensorBeingUsed()

//                  flightControllerState.setVisionPositioningSensorBeingUsed();

                    //TODO add gimble pitch degree

                    controller.addTelemetryLog(droneTelemetry);

                }
            });
        }

        if (ModuleVerificationUtil.isGimbalModuleAvailable()) {
            getProductInstance().getGimbal().setStateCallback(new GimbalState.Callback() {
                @Override
                public void onUpdate(GimbalState gimbalState) {
                    droneTelemetry.put("gimbalPitch",(double)(-1 * gimbalState.getAttitudeInDegrees().getPitch()));

                }
            });
        }

        DJISampleApplication.getAircraftInstance().getBattery().setStateCallback(new BatteryState.Callback() {
            @Override
            public void onUpdate(BatteryState batteryState) {
                droneTelemetry.put("batRemainingTime",(double)(batteryState.getLifetimeRemaining()));
                droneTelemetry.put("batCharge",(double)(batteryState.getChargeRemainingInPercent()));

            }
        });

        DJISampleApplication.getAircraftInstance().getFlightController().setASBInformationCallback(new AirSenseSystemInformation.Callback() {
            @Override
            public void onUpdate(AirSenseSystemInformation airSenseSystemInformation) {
                controller.addPlanesLog(airSenseSystemInformation.getAirplaneStates());

            }
        });


    }


    public void initPreviewer() {

        BaseProduct product = getProductInstance();

        if (product == null || !product.isConnected()) {

            controller.showToast("Disconnected");
            return;
        }

//        if (null != mVideoSurface) {
//            mVideoSurface.setSurfaceTextureListener(this);
//        }
        Camera camera = getCameraInstance();

//        camera.setVideoResolutionAndFrameRate(new ResolutionAndFrameRate(SettingsDefinitions.VideoResolution.RESOLUTION_1280x720, SettingsDefinitions.VideoFrameRate.FRAME_RATE_24_FPS),
//                new CommonCallbacks.CompletionCallback() {
//                    @Override
//                    public void onResult(DJIError djiError) {
//                        if (djiError != null){
//                            ToastUtils.setResultToToast("camera: "+djiError);
//                        }
//
//                    }
//                });

        camera.setSystemStateCallback(new SystemState.Callback() {
            @Override
            public void onUpdate(SystemState cameraSystemState) {
                if (null != cameraSystemState) {
//                    ToastUtils.setResultToToast("camera: isRecording "+cameraSystemState.isRecording());

                    if (isCameraRecording != cameraSystemState.isRecording()){
                        isCameraRecording = cameraSystemState.isRecording();
                        controller.updateRecordingStatus(isCameraRecording);
                    }
                }
            }
        });


        if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
            VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
        }

    }

    private void uninitPreviewer() {
//        Camera camera = DJISampleApplication.getCameraInstance();
//        if (camera != null){
            // Reset the callback
//            VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(null);
            VideoFeeder.getInstance().getPrimaryVideoFeed().removeVideoDataListener(mReceivedVideoDataListener);
//        }
    }

    public float getAlt(){

        float usAlt = droneTelemetry.get("UsAlt").floatValue();
        float gpsAlt = droneTelemetry.get("alt").floatValue();
        if (usAlt == 0){
            if (gpsAlt > 4){
                return gpsAlt;
            }
            else{
                return 0;
            }
        }

        return usAlt;

//        return droneTelemetry.get("UsAlt").floatValue();
    }

    public float getYaw(){
        return droneTelemetry.get("yaw").floatValue();
    }

    public void setFlightParams(float yaw,float pitch,float roll,float verticalThrottle){

        flightControlData.setYaw(yaw);

        flightControlData.setRoll(roll);
        flightControlData.setVerticalThrottle(verticalThrottle);
        flightControlData.setPitch(pitch);
        if (DJISampleApplication.getAircraftInstance().getFlightController().isVirtualStickControlModeAvailable()) {
            DJISampleApplication.getAircraftInstance().getFlightController().sendVirtualStickFlightControlData(flightControlData,this);
        }

    }

    public synchronized void setControlAllowed(boolean controllAllowed){
        this.controllAllowed = controllAllowed;
    }

    public synchronized void setControlCommand(ControllCommand command){

        if (controllAllowed == false) { return;}

        flightControlData.setVerticalThrottle(command.getVerticalThrottle());
//        flightControlData.setPitch(command.getPitch());
//        flightControlData.setRoll(command.getRoll());
        //pithc and roll are opposite !!!! we still don't know why
        flightControlData.setRoll(command.getPitch());
        flightControlData.setPitch(command.getRoll());
        flightControlData.setYaw(droneTelemetry.get("yaw").floatValue());
        if (command.getGimbalPitch() != -200){
            rotateGimbalToDegree(command.getGimbalPitch());
        }


        FlightController flightController = DJISampleApplication.getAircraftInstance().getFlightController();
        if (flightController.isVirtualStickControlModeAvailable()) {
            flightController.setVerticalControlMode(command.getControllMode());
            if (command.getControllMode() == VerticalControlMode.VELOCITY){
                flightController.confirmLanding(this);
            }
            // This is the command that get command for virtual stick and completion callback
            // Wait for
            flightController.sendVirtualStickFlightControlData(flightControlData,this);
        }


    }

    public synchronized void stopOnPlace(){
        controllAllowed = false;

        flightControlData.setVerticalThrottle(0);
        flightControlData.setRoll(0);
        flightControlData.setPitch(0);
        flightControlData.setYaw(0);

        //rotateGimbalToDegree(command.getGimbalPitch());

        FlightController flightController = DJISampleApplication.getAircraftInstance().getFlightController();
        if (flightController.isVirtualStickControlModeAvailable()) {
            flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
//            if (command.getControllMode() == VerticalControlMode.VELOCITY){
//                flightController.confirmLanding(this);
//            }
            flightController.sendVirtualStickFlightControlData(flightControlData,this);
        }
    }



//    public String getStateLine(){
//
//        String out = ""+System.currentTimeMillis()+",";
//        String key = "";
//
//        Iterator<String> itr = droneTelemetry.keySet().iterator();
//        while (itr.hasNext()) {
//            key = itr.next();
//            out += key+","+droneTelemetry.get(key)+",";
//        }
//        out += "\r\n";
//
//        return out;
//    }

    @Override
    public void onResult(DJIError djiError) {

        String finalText = "";

        if(djiError != null) {
//            showToast(""+djiError);
//            err+=djiError;
//            ep=p;er=r;et=t;

            finalText += "djiErr: "+djiError;
            controller.showToast(djiError.getDescription());
        }


        controller.onDroneCompletionCallback(finalText);


        //prepeare some text
//        final float p = flightControlData.getPitch();
//        final float r = flightControlData.getRoll();
//        final float t = flightControlData.getVerticalThrottle();
//        String err = "";
//        if(djiError != null) {
////            showToast(""+djiError);
//            err+=djiError;
//            ep=p;er=r;et=t;
//        }
//
//        final String debug = "p : "+p+" , r : "+r+" , t : "+t+"\n"+
//                "ep : "+ep+" , er : "+er+" , et : "+et+"\n"+
//                err;
//
//
//        sawModeTextView.post(new Runnable() {
//            @Override
//            public void run() {
//                sawModeTextView.setText("p : "+p+" , r : "+r+" , t : "+t+"\n"+
//                        "ep : "+ep+" , er : "+er+" , et : "+et);
//            }
//        });

    }

    public void finish(){
        uninitPreviewer();
    }


    private void printMap(Map map){

        String key = "";

        Iterator<CapabilityKey> itr = map.keySet().iterator();
        while (itr.hasNext()) {

            CapabilityKey  key1 = itr.next();


//            key = itr.next();
            Log.i("arrk","key: "+key1+" : "+map.get(key));

        }

    }

    private Gimbal getGimbalInstance() {
        if (gimbal == null) {
            initGimbal();
        }
        return gimbal;
    }

    private void initGimbal() {
        if (DJISDKManager.getInstance() != null) {
            BaseProduct product = DJISDKManager.getInstance().getProduct();
            if (product != null) {
                if (product instanceof Aircraft) {
                    gimbal = ((Aircraft) product).getGimbals().get(currentGimbalId);
                } else {
                    gimbal = product.getGimbal();
                }

                if (isFeatureSupported(CapabilityKey.PITCH_RANGE_EXTENSION)) {
                    gimbal.setPitchRangeExtensionEnabled(true, new CallbackHandlers.CallbackToastHandler());
                }
            }
        }
    }

    private boolean isFeatureSupported(CapabilityKey key) {

        Gimbal gimbal = getGimbalInstance();
        if (gimbal == null) {
            return false;
        }

        DJIParamCapability capability = null;
        if (gimbal.getCapabilities() != null) {
            capability = gimbal.getCapabilities().get(key);
        }

        if (capability != null) {
            return capability.isSupported();
        }
        return false;
    }


    private void rotateGimbalToDegree(float degree) {

        if (gimbal == null) { return;}
        if (degree ==  prevDegree) {return;}
        
        prevDegree = degree;

        if (degree > maxGimbalDegree ){degree = maxGimbalDegree;}
        if (degree < minGimbalDegree){degree = minGimbalDegree;}

        Rotation.Builder builder = new Rotation.Builder().mode(RotationMode.ABSOLUTE_ANGLE).time(2);
        builder.pitch(degree);

        gimbal.rotate(builder.build(), new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null){
                    ToastUtils.setResultToToast("Ark: Gimbal err: "+djiError.getDescription());
                }
            }
        });
    }

    public static synchronized Camera getCameraInstance() {
        if (getProductInstance() == null) return null;
        Camera camera = null;
        if (getProductInstance() instanceof Aircraft){
            camera = ((Aircraft) getProductInstance()).getCamera();
        } else if (getProductInstance() instanceof HandHeld) {
            camera = ((HandHeld) getProductInstance()).getCamera();
        }
        return camera;
    }
}
