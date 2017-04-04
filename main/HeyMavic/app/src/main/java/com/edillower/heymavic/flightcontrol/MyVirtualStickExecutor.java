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
    protected void mUp(final int optionalMovingDistance){
        double mHomeAltitude = mFlightController.getState().getAircraftLocation().getAltitude();
        mThrottle = 3;
        checkSendVirtualStickDataTimer();

        if(optionalMovingDistance!=-1){
            double targetAltitude = mHomeAltitude+optionalMovingDistance;

            double currAltitude = 0;
            double x = 0; //home_to_dest_vector
            double y = 0; //drone_to_dest_vector

            int keep = 1;
            while(keep == 1){
                currAltitude = mFlightController.getState().getAircraftLocation().getAltitude();
                if(Math.abs(currAltitude - targetAltitude)<EPS){
                    mStop();
                    keep = 0;
                }else{
                    x = targetAltitude - mHomeAltitude;
                    y = targetAltitude - currAltitude;
                    if (x*y < 0) {
                        mStop();
                        keep = 0;
                    }
                }
            }
        }
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
    protected void mDown(final int optionalMovingDistance){
        double mHomeAltitude = mFlightController.getState().getAircraftLocation().getAltitude();

        if(mHomeAltitude < 1.2f){
            Log.e("eric", "mHomeAltitude < 1.2m, landing ...");
            //mLand();
            return;
        }
        mThrottle = -3;
        checkSendVirtualStickDataTimer();
        if(optionalMovingDistance!=-1){
            if(optionalMovingDistance >= mHomeAltitude){
                Log.e("eric", "mHomeAltitude < optionalMovingDistance, landing ...");
                //mLand();
                return;
            }
            double targetAltitude = mHomeAltitude-optionalMovingDistance;
            Log.e("eric", "tar: "+ targetAltitude);
            double currAltitude = 0;
            double x = 0; //home_to_dest_vector
            double y = 0; //drone_to_dest_vector
            int keep = 1;
            while(keep == 1){
                currAltitude = mFlightController.getState().getAircraftLocation().getAltitude();
                if(Math.abs(currAltitude - targetAltitude)<EPS){
                    Log.e("eric", "arrive: "+ currAltitude);
                    mStop();
                    keep = 0;

                }else{
                    x = targetAltitude - mHomeAltitude;
                    y = targetAltitude - currAltitude;
                    if (x*y < 0) {
                        Log.e("eric", "arrive: "+ currAltitude);
                        mStop();
                        keep = 0;

                    }
                }
            }
        }
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
     * todo need review
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
    protected void mGo(int movingDirection, final int optionalMovingDistance){
        LocationCoordinate3D location = mFlightController.getState().getAircraftLocation();
        double mHomeLatitude = location.getLatitude();
        double mHomeLongitude = location.getLongitude();
        Log.e("eric", "home: "+mHomeLatitude+","+mHomeLongitude);
        double bearing = mFlightController.getCompass().getHeading();
        int dir[] = {0,1,0,-1,0};
        float pitchJoyControlMaxSpeed = 3;
        float rollJoyControlMaxSpeed = 3;
        int idx = 0;
        if(movingDirection==302){
            idx = 2;
            bearing += 180;
        }else if(movingDirection==303){
            idx = 3;
            bearing -= 90;
        }else if(movingDirection==304){
            idx = 1;
            bearing += 90;
        }
        mPitch = (float)(pitchJoyControlMaxSpeed * dir[idx]); //pX
        mRoll = (float)(rollJoyControlMaxSpeed * dir[idx+1]); //pY
        checkSendVirtualStickDataTimer();
        if (optionalMovingDistance!=-1) {
            double[] mDestination = calcDestination(mHomeLatitude, mHomeLongitude, bearing, optionalMovingDistance);
            double targetLatitude = mDestination[0];
            double targetLongitude = mDestination[1];
//            double targetLatitude = mHomeLatitude + optionalMovingDistance*dir[idx]*Utils.ONE_METER_OFFSET;
//            double targetLongitude = mHomeLongitude + optionalMovingDistance*dir[idx+1]*Utils.ONE_METER_OFFSET;
            Log.e("eric", "tar: "+targetLatitude+","+targetLongitude);
            double currLatitude = 0;
            double currLongitude = 0;
            double x[] = {0,0};//home_to_dest_vector
            double y[] = {0,0};//drone_to_dest_vector
            double cos_xy = 0;//cosine
            int keep = 1;
            while (keep == 1) {
                location = mFlightController.getState().getAircraftLocation();
                currLatitude = location.getLatitude();
                currLongitude = location.getLongitude();
                x[0] = targetLatitude - mHomeLatitude;
                x[1] = targetLongitude - mHomeLongitude;
                y[0] = targetLatitude - currLatitude;
                y[1] = targetLongitude - currLongitude;
                cos_xy = (x[0] * x[1] + y[0] * y[1]) / (Math.sqrt(x[0] * x[0] + x[1] * x[1]) * Math.sqrt(y[0] * y[0] + y[1] * y[1]));
                if (cos_xy < 0) {
                    Log.e("eric", "exceed: "+currLatitude+","+currLongitude);
                    mStop();
                    keep = 0;
                }
            }
        }
    }

    /**
     *
     * This is completed ! pass test!
     *
     * @param lati
     * @param longi
     * @param bearing [-180, 180]
     * @param distance
     * @return
     */
    private static double[] calcDestination(double lati, double longi,
                                            double bearing, double distance) {
        double[] destination = new double[2]; // double[0]=latitude double[1]=longitude

        // Setup parameters
        double radius = 6371000; // Earth radius in meters
        double ber = bearing; // Heading direction, clockwise from north
        if (bearing < 0) {
            ber += 360;
        }
        ber = Math.toRadians(ber);
        double oriLati = Math.toRadians(lati); // Latitude of the origin point
        double oriLongi = Math.toRadians(longi); // Longitude of the origin point
        double agDist = distance / radius; // Angular distance
        destination[0] = Math.asin(Math.sin(oriLati) * Math.cos(agDist)
                + Math.cos(oriLati) * Math.sin(agDist) * Math.cos(ber));
        destination[1] = oriLongi
                + Math.atan2(
                Math.sin(ber) * Math.sin(agDist) * Math.cos(oriLati),
                Math.cos(agDist) - Math.sin(oriLati)
                        * Math.sin(destination[0]));
        destination[0] = Math.toDegrees(destination[0]);
        destination[1] = Math.toDegrees(destination[1]);
        return destination;
    }
}
