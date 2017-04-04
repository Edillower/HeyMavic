package com.edillower.heymavic.flightcontrol;


import android.content.Context;

import com.edillower.heymavic.common.DJISimulatorApplication;
import com.edillower.heymavic.common.Utils;

import java.util.ArrayList;

import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.timeline.actions.GoToAction;
import dji.sdk.mission.timeline.actions.MissionAction;
import dji.sdk.mission.timeline.triggers.Trigger;
import dji.sdk.products.Aircraft;
import dji.common.error.DJIError;

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
    private MyAbstractMission mMission;
    private MyVirtualStickExecutor mSingletonVirtualStickExecutor;

    /**
     * default constructor
     * @param context
     */
    public CommandInterpreter(Context context) {
        mContext = context;
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
            MyChangeSettingsExecutor.mEnableVS(); //this should be deleted after set to private
            Utils.setResultToToast(mContext, "CI init FlightController success with mode "+mFlightController.getState().getFlightMode());
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
            Utils.setResultToToast(mContext, "VS is no");
            mSingletonVirtualStickExecutor.mStop();
        }else{
            //do nothing, should review here
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
        Utils.setResultToToast(mContext, "before cmd mode:"+mFlightController.getState().getFlightMode());
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
                mMission = new MyTestMission(mContext);
                mMission.load();
                Utils.setResultToToast(mContext, "after mission load mode:"+mFlightController.getState().getFlightMode());
                mMission.upload();
                Utils.setResultToToast(mContext, "after upload mode:"+mFlightController.getState().getFlightMode());
                mMission.start();
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
                //1. should reivew order here
                //2. stop when other cmd come in
                MyChangeSettingsExecutor.mDisableVS();
                MyVirtualStickExecutor.destroyInstance();

                GoToAction up = new GoToAction(para_dis);
                up.run();
                break;
            case 106:
                if(idx+2<mCmdCode.length && mCmdCode[idx+1]==202){
                    para_dis = mCmdCode[idx+2];
                    idx+=2;
                }
                //1. should reivew order here
                //2. stop when other cmd come in
                MyChangeSettingsExecutor.mDisableVS();
                MyVirtualStickExecutor.destroyInstance();

                GoToAction down = new GoToAction(0-para_dis);
                down.run();
                break;
            case 107:
                break;
            case 108:
                break;
            case 109:
                break;
            default:
                mStop();
                break;
        }
        Utils.setResultToToast(mContext, "after cmd mode:"+mFlightController.getState().getFlightMode());
    }
}
