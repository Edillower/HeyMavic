package com.edillower.heymavic.flightcontrol;


import android.content.Context;
import android.graphics.PointF;
import android.os.Environment;

import com.edillower.heymavic.common.DJISimulatorApplication;
import com.edillower.heymavic.common.Utils;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.sdk.camera.MediaFile;
import dji.sdk.camera.MediaManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.timeline.actions.GoToAction;
import dji.sdk.products.Aircraft;

public class CommandInterpreter {
    private Context mContext;

    public Aircraft aircraft; //need to be local, should not be declare here
    public FlightController mFlightController; //need to be private
    public boolean mVirtualStickEnabled=false;
    /*
    do not use in this way
    use DJIFlightControllerFlightMode (Enum)
    isVirtualStickControlModeAvailable()
    */
    private MyVirtualStickExecutor mSingletonVirtualStickExecutor;
    private GoToAction mGoToAction;
    //private Trigger mTrigger;
    private MediaManager mMediaManager;
    private MediaFile media;
    //int flag;


    private int object_id;
    int count = 1;

    private static CommandInterpreter uniqueInstance = null;

    private CommandInterpreter(Context context){
        mContext = context;
        //mTrigger = Trigger.getInstance();
        //flag = 0;
    }

    public static CommandInterpreter getUniqueInstance(Context context){
        if(uniqueInstance == null){
            return new CommandInterpreter(context);
        }else{
            return uniqueInstance;
        }
    }


    /**
     * should be private, not be used by others
     * now it's used by FPVFullscreen
     * */
    public void initFlightController() {
        aircraft = DJISimulatorApplication.getAircraftInstance();

        if (aircraft == null || !aircraft.isConnected()) {
            Utils.setResultToToast(mContext, "aircraft not found!");
            mFlightController = null;
        } else {
            mFlightController = aircraft.getFlightController();
//            MyChangeSettingsExecutor.mEnableVS(); //this should be deleted after set to private
//            Utils.setResultToToast(mContext, "CI init FlightController success with mode "+mFlightController.getState().getFlightMode());
        }
    }

    /**
     * This is completed !
     * directly call on FlightController
     * */
    private void mTakeoff(){
        if (mFlightController != null){
            mFlightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {

                }
            });
        }
    }

    /**
     * This is completed !
     * directly call on FlightController
     *
     * */
    private void mLand(){
        if (mFlightController != null){
            mFlightController.startLanding(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {

                }
            });
        }
    }

    /**
     * need to be private this one
     *
     * should stop VirtualStick's working or Mission
     */
    public void mStop(){
        if(mFlightController.isVirtualStickControlModeAvailable()){
            Utils.setResultToToast(mContext, "VS is on");
            mSingletonVirtualStickExecutor.mStop();
        }else if(mGoToAction!=null&&mGoToAction.isRunning()){
            mGoToAction.stop();
        }else{
            MyChangeSettingsExecutor.mEnableVS();
            mSingletonVirtualStickExecutor = MyVirtualStickExecutor.getUniqueInstance();
            mSingletonVirtualStickExecutor.mStop();
        }
    }

    /**
     *
     * @param mEncoded
     */
    public void executeCmd(ArrayList<Integer> mEncoded) {
        int len = mEncoded.size();
        int[] mCmdCode = new int[len];
        for (int i = 0; i < len; i++) {
            mCmdCode[i] = mEncoded.get(i);
        }
        if(mCmdCode.length == 0){
            Utils.setResultToToast(mContext, "Wrong Command Code [null]");
            return;
        }
        initFlightController();
        int idx = 0, para_dir_go = 301, para_dir = 303, para_dis = -1, para_deg = 90;
//        Utils.setResultToToast(mContext, "before cmd mode:"+mFlightController.getState().getFlightMode());

        if(mGoToAction!=null&&mGoToAction.isRunning()){
            mGoToAction.stop();
        }

        switch (mCmdCode[idx]) {
            case 100:
                mTakeoff();
                break;
            case 101:
                mLand();
                break;
            case 102:
                mStop();
                break;
            case 103:
                if(idx+2<mCmdCode.length && mCmdCode[idx+1]==201){
                    para_dir_go = mCmdCode[idx+2];
                    idx+=2;
                }else{
                    Utils.setResultToToast(mContext, "Wrong Command Code [103]");
                }
                if(idx+2<mCmdCode.length && mCmdCode[idx+1]==202){
                    para_dis = mCmdCode[idx+2];
                    idx+=2;
                }
                if(para_dis==-1){
                    mSingletonVirtualStickExecutor = MyVirtualStickExecutor.getUniqueInstance();
                    mSingletonVirtualStickExecutor.mGo(para_dir_go);
                }else{
                    //1. should reivew order here
                    //2. stop when other cmd come in
                    MyVirtualStickExecutor.destroyInstance();
                    double mHomeLatitude = mFlightController.getState().getAircraftLocation().getLatitude();
                    double mHomeLongitude = mFlightController.getState().getAircraftLocation().getLongitude();
                    double bearing = mFlightController.getCompass().getHeading();
                    if(para_dir_go==302){
                        bearing += 180;
                    }else if(para_dir_go==303){
                        bearing -= 90;
                    }else if(para_dir_go==304){
                        bearing += 90;
                    }
                    double[] des = Utils.calcDestination(mHomeLatitude, mHomeLongitude, bearing, para_dis);
                    final double aa = des[0];
                    final double bb = des[1];

                    Runnable goDis = new Runnable() {
                        @Override
                        public void run() {
                            try{
                                TimeUnit.SECONDS.sleep(2);
                            }catch (Exception e){

                            }
                            MyChangeSettingsExecutor.mDisableVS();
                            if(mFlightController.isVirtualStickControlModeAvailable()){
                                Utils.setResultToToast(mContext, "VS still on");
                            }
                            mGoToAction = new GoToAction(new LocationCoordinate2D(aa,bb));
                            mGoToAction.run();
                            try{
                                TimeUnit.SECONDS.sleep(2);
                            }catch (Exception e){

                            }
                            mGoToAction.didRun();
                        }
                    };

                    goDis.run();

                }
                break;
            case 104:
                if(idx+2<mCmdCode.length && mCmdCode[idx+1]==203){
                    para_dir = mCmdCode[idx+2];
                    idx += 2;
                }else{
                    Utils.setResultToToast(mContext, "Wrong Command Code [104]");
                }
                if(idx+2<mCmdCode.length && mCmdCode[idx+1]==204){
                    para_deg = mCmdCode[idx+2];
                    idx += 2;
                }
                mSingletonVirtualStickExecutor = MyVirtualStickExecutor.getUniqueInstance();
                mSingletonVirtualStickExecutor.mTurn(para_dir,para_deg);
                break;
            case 105:
                if(idx+2<mCmdCode.length && mCmdCode[idx+1]==202){
                    para_dis = mCmdCode[idx+2];
                    idx+=2;
                }
                if(para_dis == -1){
                    Utils.setResultToToast(mContext, "Up no dis");
                    mSingletonVirtualStickExecutor = MyVirtualStickExecutor.getUniqueInstance();
                    mSingletonVirtualStickExecutor.mUp();
                }else{
                    //1. should reivew order here
                    //2. stop when other cmd come in
                    Utils.setResultToToast(mContext, "Up dis: " + Integer.toString(para_dis));
                    MyChangeSettingsExecutor.mDisableVS();
                    MyVirtualStickExecutor.destroyInstance();

                    mGoToAction = new GoToAction(para_dis);
                    mGoToAction.run();
                }
                break;
            case 106:
                if(idx+2<mCmdCode.length && mCmdCode[idx+1]==202){
                    para_dis = mCmdCode[idx+2];
                    idx+=2;
                }
                if(para_dis == -1){
                    mSingletonVirtualStickExecutor = MyVirtualStickExecutor.getUniqueInstance();
                    mSingletonVirtualStickExecutor.mDown();
                }else{
                    //1. should reivew order here
                    //2. stop when other cmd come in
                    MyChangeSettingsExecutor.mDisableVS();
                    MyVirtualStickExecutor.destroyInstance();

                    mGoToAction = new GoToAction(0-para_dis);
                    mGoToAction.run();
                }
                break;
            case 107:
                int para_lati_int=25, para_lati_decimal=0, para_logi_int=113, para_logi_decimal=0;

                if(idx+5<mCmdCode.length && mCmdCode[idx+1]==205){
                    para_lati_int = mCmdCode[idx+2];
                    para_lati_decimal = mCmdCode[idx+3];
                    para_logi_int = mCmdCode[idx+4];
                    para_logi_decimal = mCmdCode[idx+5];
                    idx+=5;
                }else{
                    Utils.setResultToToast(mContext, "Wrong Command Code [107]");
                }
                //1. should reivew order here
                //2. stop when other cmd come in
                MyChangeSettingsExecutor.mDisableVS();
                MyVirtualStickExecutor.destroyInstance();

                double para_lati = para_lati_int + (double) para_lati_decimal/100000.0;
                double para_logi = para_logi_int + (double) para_logi_decimal/100000.0;
                Utils.setResultToToast(mContext, para_lati+","+para_logi);
                //=======================
                mGoToAction = new GoToAction(new LocationCoordinate2D(para_lati, para_logi));
                MissionControl mc = MissionControl.getInstance();
                mc.scheduleElement(mGoToAction);
                mc.startTimeline();
                //=======================

                break;
            case 108:
                int set_type = 0, set_param = 0;
                if(idx+2<mCmdCode.length && mCmdCode[idx+1]==206){
                    set_type= mCmdCode[idx+2];
                    idx += 2;
                }else{
                    Utils.setResultToToast(mContext, "Wrong Command Code [108]");
                }
                if(idx+2<mCmdCode.length && mCmdCode[idx+1]==207){
                    set_param = mCmdCode[idx+2];
                    idx += 2;
                }
//                MyChangeSettingsExecutor.execute(set_type, set_param);
                break;
            case 109:
                object_id = mCmdCode[1];
                shootPhoto();
                break;
            default:
                mStop();
                break;
        }
//        Utils.setResultToToast(mContext, "after cmd mode:"+mFlightController.getState().getFlightMode());
    }

    public void shootPhoto() {
        // take photo
        //
        // Utils.setResultToToast(mContext, mTrigger.trig());
        //flag ++;
        DJISimulatorApplication.getProductInstance().getCamera().startShootPhoto(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    Utils.setResultToToast(mContext, "shoot photo: success"); //TODO
                    try{
                        TimeUnit.SECONDS.sleep((long) 2.5);
                    }catch (Exception e){

                    }
                    getPhoto();
                } else {
                    Utils.setResultToToast(mContext, "shoot error:" + error.getDescription()); //TODO
                }
            }
        });
    }

    public void getPhoto() {

        //setDownloadMode();

        mMediaManager = DJISimulatorApplication.getProductInstance().getCamera().getMediaManager();
        // fetch photo from SD card
        if (mMediaManager == null) {
            Utils.setResultToToast(mContext, "error get media manager");
        }else{
            // get the photo at top and set camera to download mode

            mMediaManager.fetchMediaList(new MediaManager.DownloadListener<List<MediaFile>>() {
                @Override
                public void onStart() {
                    //Log.d(TAG, "Come to access data on SD card");
                }

                @Override
                public void onRateUpdate(long l, long l1, long l2) {

                }

                @Override
                public void onProgress(long l, long l1) {

                }

                @Override
                public void onSuccess(List<MediaFile> djiMedias) {
                    if(djiMedias == null) {
                        Utils.setResultToToast(mContext, "no media in SD card");
                    }
                    else {
                        Utils.setResultToToast(mContext, "get photo: success"); //TODO
                        media = djiMedias.get(djiMedias.size()-1);

                        fetchPhoto();
                    }

                }

                @Override
                public void onFailure(DJIError djiError) {
                    //Log.e(TAG, djiError.getDescription());
                }
            });
        }
    }

    public void fetchPhoto() {

        final File destDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString()
                + "/Camera");
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = new Date();
        final String Name = "test" + Integer.toString(count) + "_" + dateFormat.format(date);
        count ++;
        // download photo to dir to achieve image processing
        if(media == null) {
            Utils.setResultToToast(mContext, "fetch photo: error"); //TODO
        }else{
            Utils.setResultToToast(mContext, "fetched photo: "+ media.getFileName()); //TODO
            DownloadHandler<String> downloadHandler = new DownloadHandler<>(mContext, destDir+Name, uniqueInstance);
            DJISimulatorApplication.getProductInstance().getCamera().getMediaManager().fetchMediaData(media,destDir,Name,downloadHandler);

            try{
                TimeUnit.SECONDS.sleep(3);
            }catch (Exception e){

            }
            shoot();

        }

    }



    public void setPhotoMode(){
        DJISimulatorApplication.getProductInstance().getCamera().setPhotoAspectRatio(SettingsDefinitions.PhotoAspectRatio.RATIO_16_9,
                new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (null != djiError) {
                            //Log.e(TAG,djiError.getDescription());
                        }
                    }
                });

        DJISimulatorApplication.getProductInstance()
                .getCamera()
                .setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, null);


        DJISimulatorApplication.getProductInstance().getCamera().setShootPhotoMode(SettingsDefinitions.ShootPhotoMode.SINGLE,
                new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (null != djiError) {
                            //Log.e(TAG,djiError.getDescription());
                        }else{
                            Utils.setResultToToast(mContext, "set single photo mode");
                        }
                    }
                });
        DJISimulatorApplication.getProductInstance().getCamera().setPhotoFileFormat(SettingsDefinitions.PhotoFileFormat.JPEG,
                new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (null != djiError) {
                            //Log.e(TAG,djiError.getDescription());
                        }
                    }
                });
    }

    public void setDownloadMode(){

        DJISimulatorApplication.getProductInstance()
                .getCamera()
                .setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD,
                        new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (null != djiError) {
                                    //Log.e(TAG,djiError.getDescription());
                                    Utils.setResultToToast(mContext, "set mode " + djiError.getDescription());
                                }else{
                                    Utils.setResultToToast(mContext, "set to download mode!");
                                }
                            }});


    }

    public void focusLen(float[] focusCoordinates) {

        PointF targ = new PointF(focusCoordinates[0], focusCoordinates[1]);

        if (DJISimulatorApplication.getProductInstance().getCamera() != null) {
            DJISimulatorApplication.getProductInstance().getCamera().setFocusTarget(targ, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError error) {
                            if (error == null) {
                                //if(mTrigger.value()) {
                                //shootPhoto();
                                Utils.setResultToToast(mContext, "focus success" );
                            } else {
                                Utils.setResultToToast(mContext, "focus " + error.getDescription());
                            }

                        }
                    }
            );
        }
    }

    public void shoot(){
        DJISimulatorApplication.getProductInstance().getCamera().startShootPhoto(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    Utils.setResultToToast(mContext, "shoot photo with focus: success"); //TODO
                } else {
                    Utils.setResultToToast(mContext, "shoot error:" + error.getDescription()); //TODO
                }
            }
        });
    }


}



