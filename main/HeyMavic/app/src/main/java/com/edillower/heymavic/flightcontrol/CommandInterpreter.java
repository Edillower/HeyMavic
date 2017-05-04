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
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.MediaFile;
import dji.sdk.camera.MediaManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.timeline.actions.GoToAction;
import dji.sdk.products.Aircraft;


/**
 * Interpreter that maps command to action of the drone
 * @author Eddie Wang, Eric Xu, Melody Cai
 */
public class CommandInterpreter {

    private Context mContext;

    public Aircraft aircraft; //need to be local, should not be declare here
    public FlightController mFlightController; //need to be private
    public boolean mVirtualStickEnabled=false;

    private MyVirtualStickExecutor mSingletonVirtualStickExecutor;
    private GoToAction mGoToAction;
    //private Trigger mTrigger;
    private MediaManager mMediaManager;
    private MediaFile media;

    private int object_id;
    int count = 1;

    /**
     *  Constructor and Basics
     *  @author Eric Xu
     */

    /**
     * singleton pattern
     */
    private static CommandInterpreter uniqueInstance = null;

    /**
     * always private, no use
     */
    private CommandInterpreter(Context context){
        mContext = context;
    }

    /**
     * @param context
     * @return instance of the command interpreter
     */
    public static CommandInterpreter getUniqueInstance(Context context){
        if(uniqueInstance == null){
            return new CommandInterpreter(context);
        }else{
            return uniqueInstance;
        }
    }

    /**
     *  END of Constructor and Basics
     *  @author Eric Xu
     */

    /**
     * Initialize flight controller
     * @author Eric Xu
     */
    public void initFlightController() {
        aircraft = DJISimulatorApplication.getAircraftInstance();

        if (aircraft == null || !aircraft.isConnected()) {
//            Utils.setResultToToast(mContext, "aircraft not found!");
            mFlightController = null;
        } else {
            mFlightController = aircraft.getFlightController();
//            Utils.setResultToToast(mContext, "CI init FlightController success with mode "+mFlightController.getState().getFlightMode());
        }
    }

    /**
     * Take off
     * @author Eric Xu
     */
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
     * Landing
     * @author Eric Xu
     */
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
     * Stop
     * @author Eddie Wang
     */
    public void mStop(){
        if(mFlightController.isVirtualStickControlModeAvailable()){
            mSingletonVirtualStickExecutor.mStop();
        }else{
            MyChangeSettingsExecutor.mEnableVS(); //delete
            mSingletonVirtualStickExecutor = MyVirtualStickExecutor.getUniqueInstance();
            mSingletonVirtualStickExecutor.mStop();
        }
    }

    /**
     * Encoded String Mapper
     * See encoding protocol for details (http://heymavic.edillower.com/protocol)
     * @author Eric Xu, Eddie Wang
     * @param mEncoded Encoded string given by NLP module
     */
    public void executeCmd(ArrayList<Integer> mEncoded) {
        // prepare execution
        int len = mEncoded.size();
        int[] mCmdCode = new int[len];
        for (int i = 0; i < len; i++) {
            mCmdCode[i] = mEncoded.get(i);
        }
        if(mCmdCode.length == 0){
            Utils.setResultToToast(mContext, "Wrong Command Code [null]");
            return;
        }

        BaseProduct product = DJISimulatorApplication.getProductInstance();
        if(product == null || !product.isConnected()){
//            Utils.setResultToToast(mContext, "CI: disconnect");
            return;
        }else{
            if(product instanceof Aircraft){
                mFlightController = ((Aircraft)product).getFlightController();
//                Utils.setResultToToast(mContext, "CI: FC good");
            }else{
                return;
            }
        }

        // mapping
        int idx = 0, para_dis, para_dir_go, para_dir, para_deg,para_type,para_val;
        switch (mCmdCode[idx]) {
            case 100: // Take off
                mTakeoff();
                break;
            case 101: // Landing
                mLand();
                break;
            case 102: // Stop
                mStop();
                break;
            case 103: // Go/Move
                para_dis = -1;
                para_dir_go = 90;
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
                mSingletonVirtualStickExecutor = MyVirtualStickExecutor.getUniqueInstance();
                mSingletonVirtualStickExecutor.mGo(para_dir_go, para_dis);
                break;
            case 104: // Turn
                para_dir = 0;
                para_deg = 90;
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
            case 105: // Up
                para_dis = -1;
                if(idx+2<mCmdCode.length && mCmdCode[idx+1]==202){
                    para_dis = mCmdCode[idx+2];
                    idx+=2;
                }

                mSingletonVirtualStickExecutor = MyVirtualStickExecutor.getUniqueInstance();
                mSingletonVirtualStickExecutor.mUp(para_dis);
                break;
            case 106: // Down
                para_dis = -1;
                if(idx+2<mCmdCode.length && mCmdCode[idx+1]==202){
                    para_dis = mCmdCode[idx+2];
                    idx+=2;
                }
                mSingletonVirtualStickExecutor = MyVirtualStickExecutor.getUniqueInstance();
                mSingletonVirtualStickExecutor.mDown(para_dis);
                break;
            case 107: // FlyTo (a specific location)
                double para_lati = 25, para_logi = 113;
                if(idx+5<mCmdCode.length && mCmdCode[idx+1]==205){
                    para_lati = mCmdCode[idx+2] + mCmdCode[idx+3]/100000.0;
                    para_logi = mCmdCode[idx+4] + mCmdCode[idx+5]/100000.0;
                    idx+=5;
                }else{
                    Utils.setResultToToast(mContext, "Wrong Command Code [107]");
                }
                mSingletonVirtualStickExecutor = MyVirtualStickExecutor.getUniqueInstance();
                mSingletonVirtualStickExecutor.mFlyto(para_lati, para_logi);
                break;
            case 108: // Change settings
                if(idx+2<mCmdCode.length && mCmdCode[idx+1]==206){
                    para_type = mCmdCode[idx+2];
                    idx += 2;
                    if(idx+2<mCmdCode.length && mCmdCode[idx+1]==207){
                        para_val = mCmdCode[idx+2];
                        idx += 2;
                        changeSetting(para_type,para_val);
                    }else{
                        Utils.setResultToToast(mContext, "Wrong Command Code [108]");
                    }
                }else{
                    Utils.setResultToToast(mContext, "Wrong Command Code [108]");
                }
                break;
            case 109: // Take photo (with focusing on a specific object)
                object_id = mCmdCode[1];
                shootPhoto();
                break;
            default:
                mStop();
                break;
        }
    }

    /**
     * Change setting
     * @author Eddie Wang
     * @param type code of target setting
     * @param para parameter of target setting
     */
    private void changeSetting(int type, int para){
        switch (type) {
            case 401:
                MyChangeSettingsExecutor.setGoHomeHeightInMeters((float)para);
                Utils.setResultToToast(mContext,"Return to home altitude has been set to " + Integer.toString(para) + " meters");
                break;
            case 402:
                MyChangeSettingsExecutor.setMaxFlightHeight((float)para);
                Utils.setResultToToast(mContext,"Max flight height has been set to " + Integer.toString(para) + " meters");
                break;
            case 403:
                MyVirtualStickExecutor.getUniqueInstance().setSpeed(para);
                Utils.setResultToToast(mContext,"Speed has been set to " + Integer.toString(para) + " m/s");
                break;
            default:
                mStop();
                break;
        }
    }

    /**
     * Photo Shooting Module
     * @author Melody Cai
     * @assitants David Yang, Eddie Wang, Eric Xu
     */

    /**
     * Take a photo
     * @author Melody Cai
     */
    public void shootPhoto() {
        DJISimulatorApplication.getProductInstance().getCamera().startShootPhoto(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
//                    Utils.setResultToToast(mContext, "shoot photo: success");
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

    /**
     * Get media list from the drone
     * @author Melody Cai
     */
    public void getPhoto() {
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
                        //Utils.setResultToToast(mContext, "get photo: success");
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

    /**
     * Download the latest photo from the drone and save it on SD card
     * @author Melody Cai
     */
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
//            Utils.setResultToToast(mContext, "fetched photo: "+ media.getFileName());
            DownloadHandler<String> downloadHandler = new DownloadHandler<>(mContext, destDir+Name, uniqueInstance);
            DJISimulatorApplication.getProductInstance().getCamera().getMediaManager().fetchMediaData(media,destDir,Name,downloadHandler);

            try{
                TimeUnit.SECONDS.sleep(3);
            }catch (Exception e){

            }
            shoot();

        }

    }

    /**
     * Prepare for taking photo
     * @maintainer Melody Cai
     */
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
//                            Utils.setResultToToast(mContext, "set single photo mode");
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

    /**
     * Change to Download Mode
     * @maintainer Melody Cai
     */
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
//                                    Utils.setResultToToast(mContext, "set to download mode!");
                                }
                            }});


    }

    /**
     * Set focusing point
     * @author Melody Cai
     */
    public void focusLen(float[] focusCoordinates) {

        PointF targ = new PointF(focusCoordinates[0], focusCoordinates[1]);

        if (DJISimulatorApplication.getProductInstance().getCamera() != null) {
            DJISimulatorApplication.getProductInstance().getCamera().setFocusTarget(targ, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError error) {
                            if (error == null) {
                                //if(mTrigger.value()) {
                                //shootPhoto();
                                Utils.setResultToToast(mContext, "Focus and Shooting Succeed" ); // TODO: shooting not succeed yet
                            } else {
                                Utils.setResultToToast(mContext, "focus " + error.getDescription());
                            }

                        }
                    }
            );
        }
    }

    /**
     * Take a photo with focusing on the specific object
     * @author Melody Cai
     */
    public void shoot(){
        DJISimulatorApplication.getProductInstance().getCamera().startShootPhoto(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
//                    Utils.setResultToToast(mContext, "Shooting Photo Succeed"); //TODO
                } else {
                    Utils.setResultToToast(mContext, "shoot error:" + error.getDescription()); //TODO
                }
            }
        });
    }

    /**
     * END of Photo Shooting Module
     * @author Melody Cai
     * @assitants David Yang, Eddie Wang, Eric Xu
     */

}
