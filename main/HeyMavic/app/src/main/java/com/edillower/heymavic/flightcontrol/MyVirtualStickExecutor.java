package com.edillower.heymavic.flightcontrol;

import android.util.Log;

import com.edillower.heymavic.common.DJISimulatorApplication;
import com.edillower.heymavic.common.Utils;

import java.util.Timer;
import java.util.TimerTask;

import dji.common.error.DJIError;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;

/**
 * Created by Eric on 4/3/17.
 *
 * !!! This is Singleton pattern class !!!
 */
public class MyVirtualStickExecutor {
    final private double EPS = Utils.ONE_METER_OFFSET / 100;

    private Timer mSendVirtualStickDataTimer;
    private SendVirtualStickDataTask mSendVirtualStickDataTask;

    private static FlightController mFlightController;

    private float mPitch=0;
    private float mRoll=0;
    private float mYaw=0;
    private float mThrottle=0;

    /**
     * singleton pattern
     */
    private static MyVirtualStickExecutor uniqueInstance = null;

    /**
     * always private, no use
     */
    private MyVirtualStickExecutor(){}

    public static void initFlightController() {
        mFlightController = DJISimulatorApplication.getFlightController();
    }

    /**
     *
     */
    private void checkSendVirtualStickDataTimer() {
        if (null == mSendVirtualStickDataTimer) {
            mSendVirtualStickDataTask = new SendVirtualStickDataTask();
            mSendVirtualStickDataTimer = new Timer();
            mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
        }
    }

    /**
     * This is completed !
     *
     * a new java TimerTask to send DJIVirtualStickFlightControlData with four global params
     * (mPitch, mRoll, mYaw, mThrottle)
     *
     * show error toast on FPVFullscreenActivity
     * */
    class SendVirtualStickDataTask extends TimerTask {
        @Override
        public void run() {
            if (mFlightController != null) {
                mFlightController.sendVirtualStickFlightControlData(
                        new FlightControlData(
                                mPitch, mRoll, mYaw, mThrottle
                        ), new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {

                            }
                        }
                );
            }
        }
    }

    /**
     * do steps in order:
     * ----
     * get instance
     * initial flight controller
     * enable virtual stick
     * set virtual stick convention control mode
     * set Timer
     * set task
     *
     *
     * @return this class unique instance
     */
    public static MyVirtualStickExecutor getUniqueInstance(){
        if(uniqueInstance == null){
            uniqueInstance = new MyVirtualStickExecutor();
        }
        initFlightController();
        MyChangeSettingsExecutor.mEnableVS();
        MyChangeSettingsExecutor.setConventionVirtualStickMode();
        uniqueInstance.checkSendVirtualStickDataTimer();
        return uniqueInstance;
    }

    /**
     * destroy unique instance in this class
     *
     * if instance exits, do steps in order:
     * ----
     * stop current virtual stick working
     * cancel SendVirtualStickDataTask and set to null
     * cancel SendVirtualStickDataTimer, purge and set to null
     * disable VirtualStickMode
     * set uniqueInstance to null
     */
    public static void destroyInstance(){
        if(uniqueInstance != null){
            uniqueInstance.mStop();

            if (uniqueInstance.mSendVirtualStickDataTimer != null) {
                uniqueInstance.mSendVirtualStickDataTask.cancel();
                uniqueInstance.mSendVirtualStickDataTask = null;
                uniqueInstance.mSendVirtualStickDataTimer.cancel();
                uniqueInstance.mSendVirtualStickDataTimer.purge();
                uniqueInstance.mSendVirtualStickDataTimer = null;
            }

            MyChangeSettingsExecutor.mDisableVS();

            uniqueInstance = null;
        }
    }

    /**
     * This is completed !
     *
     * ONLY STOP actions triggered by FlightController.SendVirtualStickDataTask()
     * (i.e. up, down, turn, move)
     *
     * @require
     * RollPitchControlMode: velocity
     * VerticalControlMode: velocity
     * YawControlMode: angle
     * HorizontalCoordinate: body
     *
     * @check
     * VirtualStick need to be on
     * SendVirtualStickDataTimer be active
     *
     * @clear mThrottle, mPitch, mRoll
     * @restore mYaw
     * */
    protected void mStop(){
        checkSendVirtualStickDataTimer();
        mPitch=0;
        mRoll=0;
        mThrottle=0;
    }

    /**
     * This is completed !
     * using vectors and EPS to double check the arriving condition
     *
     * @requires
     * VirtualStick: on
     * RollPitchControlMode: velocity
     * VerticalControlMode: velocity
     * YawControlMode: angle
     * HorizontalCoordinate: body
     *
     * @check SendVirtualStickDataTimer be active
     * @restore mYaw, mPitch, mRoll
     * @updates mThrottle, mHomeAltitude
     * */
    protected void mUp(){
        checkSendVirtualStickDataTimer();
        mThrottle = 3;
    }

    /**
     * todo need review for landing
     * using vectors and EPS to double check the arriving condition
     *
     * @require
     * VirtualStick: on
     * RollPitchControlMode: velocity
     * VerticalControlMode: velocity
     * YawControlMode: angle
     * HorizontalCoordinate: body
     *
     * @check SendVirtualStickDataTimer be active
     * @restore mYaw, mPitch, mRoll
     * @update mThrottle, mHomeAltitude
     * @ensure
     * if currAltitude < 1.2m OR currAltitude < optionalMovingDistance, directly landing
     * */
    protected void mDown(){
        checkSendVirtualStickDataTimer();
        mThrottle = -3;
    }

    /**
     * This is completed !
     *
     * @require
     * VirtualStick: on
     * RollPitchControlMode: velocity
     * VerticalControlMode: velocity
     * YawControlMode: angle
     * HorizontalCoordinate: body
     *
     * @check SendVirtualStickDataTimer be active
     * @restore mPitch, mRoll, mThrottle
     * @update mYaw
     * */
    protected void mTurn(int turningDirection, int optionalTurningDegree){
        checkSendVirtualStickDataTimer();

        float currHeading = (float) mFlightController.getCompass().getHeading();
        mYaw = currHeading;
        if(turningDirection == 303){
            mYaw -= optionalTurningDegree;
            if(mYaw < -180){
                mYaw += 360;
            }
        }else{
            mYaw += optionalTurningDegree;
            if(mYaw > 180){
                mYaw -= 360;
            }
        }
        Log.e("eric", "Turning from "+ currHeading + " to " + mYaw);
    }

    /**
     * only use vectors to double check the arriving condition
     *
     * in order to avoid bias effect, do not use EPS to check arriving
     *
     * @require
     * VirtualStick: on
     * RollPitchControlMode: velocity
     * VerticalControlMode: velocity
     * YawControlMode: angle
     * HorizontalCoordinate: body
     *
     * @check SendVirtualStickDataTimer be active
     * @restore mYaw, mThrottle
     * @update mPitch, mRoll, mDestination[], mHomeLatitude, mHomeLongitude
     * */
    protected void mGo(int movingDirection){
        checkSendVirtualStickDataTimer();

        int dir[] = {0,1,0,-1,0};
        int idx = 0;
        if(movingDirection==302){
            idx = 2;
        }else if(movingDirection==303){
            idx = 3;
        }else if(movingDirection==304){
            idx = 1;
        }
        mPitch = (float)(3 * dir[idx]);
        mRoll = (float)(3 * dir[idx+1]);
    }
}
