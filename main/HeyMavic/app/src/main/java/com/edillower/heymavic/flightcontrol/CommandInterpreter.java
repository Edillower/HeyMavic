package com.edillower.heymavic.flightcontrol;


import android.content.Context;
import android.location.Location;

import com.edillower.heymavic.FPVFullscreenActivity;
import com.edillower.heymavic.common.DJISimulatorApplication;
import com.edillower.heymavic.common.Utils;

import java.util.ArrayList;

import dji.common.camera.SystemState;
import dji.common.util.CommonCallbacks;
import dji.common.util.LocationUtils;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.timeline.Mission;
import dji.sdk.products.Aircraft;
import dji.common.error.DJIError;


/**
 * !!! this is singleton class !!!
 */
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
     *
     * @param context
     * @return
     */
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

        BaseProduct product = DJISimulatorApplication.getProductInstance();
        if(product == null || !product.isConnected()){
            Utils.setResultToToast(mContext, "CI: disconnect");
            return;
        }else{
            if(product instanceof Aircraft){
                mFlightController = ((Aircraft)product).getFlightController();
                Utils.setResultToToast(mContext, "CI: FC good");
            }else{
                return;
            }
        }

        int idx = 0, para_dis, para_dir_go, para_dir, para_deg;
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
            case 104:
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
            case 105:
                para_dis = -1;
                if(idx+2<mCmdCode.length && mCmdCode[idx+1]==202){
                    para_dis = mCmdCode[idx+2];
                    idx+=2;
                }

                mSingletonVirtualStickExecutor = MyVirtualStickExecutor.getUniqueInstance();
                mSingletonVirtualStickExecutor.mUp(para_dis);
                break;
            case 106:
                para_dis = -1;
                if(idx+2<mCmdCode.length && mCmdCode[idx+1]==202){
                    para_dis = mCmdCode[idx+2];
                    idx+=2;
                }
                mSingletonVirtualStickExecutor = MyVirtualStickExecutor.getUniqueInstance();
                mSingletonVirtualStickExecutor.mDown(para_dis);
                break;
            case 107:
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
            default:
                mStop();
                break;
        }
    }
}