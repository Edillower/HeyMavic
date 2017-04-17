package com.edillower.heymavic.flightcontrol;

import com.edillower.heymavic.common.DJISimulatorApplication;
import com.edillower.heymavic.common.Utils;

import java.util.Timer;
import java.util.TimerTask;

import dji.common.error.DJIError;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;

/**
 * Created by Eric on 4/3/17.
 *
 * !!! This is Singleton pattern class !!!
 */
public class MyVirtualStickExecutor {
    private MyVirtualStickExecutorMode mMode = MyVirtualStickExecutorMode.UNINITIALIZED;

    private float mPitch = 0;
    private float mRoll = 0;
    private float mYaw = 0;
    private float mThrottle = 0;

    private Timer mSendVirtualStickDataTimer;
    private SendVirtualStickDataTask mSendVirtualStickDataTask;

    private Timer mLocationTrackTimer;
    private LocationTrackTask mLocationTrackTask;

    private static FlightController mFlightController;

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
     *  mSendVirtualStickDataTimer always has only one task: mSendVirtualStickDataTask
     */
    private void checkSendVirtualStickDataTimer() {
        if (mSendVirtualStickDataTimer == null) {
            mSendVirtualStickDataTask = new SendVirtualStickDataTask();
            mSendVirtualStickDataTimer = new Timer();
            mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
        }
    }

    /**
     * mLocationTrackTimer always has only one task: mLocationTrackTask
     * destroy timer first
     * @param mode
     * @param homeH
     * @param tarH
     */
    private void checkHeightLocationTrackTimer(MyVirtualStickExecutorMode mode, double homeH, double tarH){
        destroyLocationTrackTimer();
        if(mode == MyVirtualStickExecutorMode.UP_DIS || mode == MyVirtualStickExecutorMode.DOWN_DIS){
            mLocationTrackTask = new LocationTrackTask(mode, homeH, tarH);
        }
        mLocationTrackTimer = new Timer();
        mLocationTrackTimer.schedule(mLocationTrackTask, 400, 200);
    }

    /**
     * mLocationTrackTimer always has only one task: mLocationTrackTask
     * destroy timer first
     * @param mode
     * @param homeLat
     * @param homeLog
     * @param tarLat
     * @param tarLog
     */
    private void check2DLocationTrackTimer(MyVirtualStickExecutorMode mode, double homeLat, double homeLog, double tarLat, double tarLog){
        destroyLocationTrackTimer();
        if(mode == MyVirtualStickExecutorMode.MOVE_DIS || mode == MyVirtualStickExecutorMode.FLY_TO){
            mLocationTrackTask = new LocationTrackTask(mode, homeLat, homeLog, tarLat, tarLog);
        }
        mLocationTrackTimer = new Timer();
        mLocationTrackTimer.schedule(mLocationTrackTask, 1000, 200);
    }

    /**
     * destroy LocationTrackTimer when it is no need
     */
    private void destroyLocationTrackTimer(){
        if(mLocationTrackTimer!=null){
            mLocationTrackTask.cancel();
            mLocationTrackTask = null;
            mLocationTrackTimer.cancel();
            mLocationTrackTimer.purge();
            mLocationTrackTimer = null;
        }
    }

    /**
     * a new java TimerTask to send DJIVirtualStickFlightControlData with four global params
     * (mPitch, mRoll, mYaw, mThrottle)
     */
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
     * a new TimerTask to check location and set four global params (mPitch, mRoll, mYaw, mThrottle)
     * when need
     */
    class LocationTrackTask extends TimerTask {
        private MyVirtualStickExecutorMode m;
        private double homeH, tarH, curH, homeLat, homeLog, tarLat, tarLog, curLat, curLog;

        public LocationTrackTask(MyVirtualStickExecutorMode mode, double homeH, double tarH){
            this.m = mode;
            this.homeH = homeH;
            this.tarH = tarH;
        }

        public LocationTrackTask(MyVirtualStickExecutorMode mode, double homeLat, double homeLog, double tarLat, double tarLog){
            this.m = mode;
            this.homeLat = homeLat;
            this.homeLog = homeLog;
            this.tarLat = tarLat;
            this.tarLog = tarLog;
        }

        @Override
        public void run() {
            if (mFlightController!= null) {
                if(m == MyVirtualStickExecutorMode.UP_DIS || m == MyVirtualStickExecutorMode.DOWN_DIS){
                    curH = mFlightController.getState().getAircraftLocation().getAltitude();
                    double home2cur = curH - homeH;
                    double cur2tar = tarH - curH;
                    if(home2cur * cur2tar < 0){
                        mThrottle = 0;
                    }
                }else if(m == MyVirtualStickExecutorMode.MOVE_DIS || m == MyVirtualStickExecutorMode.FLY_TO){
                    curLat = mFlightController.getState().getAircraftLocation().getLatitude();
                    curLog = mFlightController.getState().getAircraftLocation().getLongitude();

                    double home2curX = curLat - homeLat;
                    double home2curY = curLog - homeLog;
                    double home2curMag = Math.sqrt(home2curX*home2curX+home2curY*home2curY);

                    double cur2tarX = tarLat - curLat;
                    double cur2tarY = tarLog - curLog;
                    double cur2tarMag = Math.sqrt(cur2tarX*cur2tarX+cur2tarY*cur2tarY);

                    double cosUp = home2curX*cur2tarX + home2curY*cur2tarY;
                    double cosDown = home2curMag*cur2tarMag;

                    if(cosUp/cosDown < 0){
                        mPitch = 0;
                        mRoll = 0;
                    }else{
                        if(m == MyVirtualStickExecutorMode.FLY_TO){
                            //set direction
                            if(cur2tarX < 0){
                                if(cur2tarY == 0){
                                    mYaw = -90;
                                }else{
                                    mYaw = -(float)(Math.atan(-cur2tarX/cur2tarY));
                                }
                            }else if(cur2tarX > 0){
                                if(cur2tarY == 0){
                                    mYaw = 90;
                                }else{
                                    mYaw = (float)(Math.atan(cur2tarX/cur2tarY));
                                }
                            }else{
                                //north or south or original point
                                if(cur2tarY > 0){
                                    mYaw = 0;
                                }else if(cur2tarY < 0){
                                    mYaw = 180;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * ONLY STOP actions triggered by FlightController.SendVirtualStickDataTask()
     * (i.e. up, down, turn, move, flyto)
     *
     * @require
     * RollPitchControlMode: velocity
     * VerticalControlMode: velocity
     * YawControlMode: angle
     * HorizontalCoordinate: body
     *
     * @check SendVirtualStickDataTimer be active
     * @clear mThrottle, mPitch, mRoll
     * @restore mYaw
     * */
    protected void mStop(){
        mMode = MyVirtualStickExecutorMode.STOP;
        checkSendVirtualStickDataTimer();
        destroyLocationTrackTimer();
        mPitch=0;
        mRoll=0;
        mThrottle=0;
    }

    /**
     * using vectors check the arriving condition
     *
     * @requires
     * RollPitchControlMode: velocity
     * VerticalControlMode: velocity
     * YawControlMode: angle
     * HorizontalCoordinate: body
     *
     * @check SendVirtualStickDataTimer be active
     * @restore mYaw, mPitch, mRoll
     * @updates mThrottle
     * */
    protected void mUp(int dis){
        mMode = MyVirtualStickExecutorMode.UP_WITHOUT_DIS;
        checkSendVirtualStickDataTimer();
        destroyLocationTrackTimer();
        mThrottle = 3;
        if(dis !=-1){
            mMode = MyVirtualStickExecutorMode.UP_DIS;
            double homeH = mFlightController.getState().getAircraftLocation().getAltitude();
            double tarH = homeH + dis;
            checkHeightLocationTrackTimer(mMode, homeH, tarH);
        }
    }

    /**
     * using vectors check the arriving condition
     *
     * @require
     * RollPitchControlMode: velocity
     * VerticalControlMode: velocity
     * YawControlMode: angle
     * HorizontalCoordinate: body
     *
     * @check SendVirtualStickDataTimer be active
     * @restore mYaw, mPitch, mRoll
     * @update mThrottle
     * @ensure
     * if currAltitude < 1.2m OR currAltitude < optionalMovingDistance, directly landing
     * */
    protected void mDown(int dis){
        mMode = MyVirtualStickExecutorMode.DOWN_WITHOUT_DIS;
        checkSendVirtualStickDataTimer();
        destroyLocationTrackTimer();
        mThrottle = -3;
        if(dis != -1){
            mMode = MyVirtualStickExecutorMode.DOWN_DIS;
            double homeH = mFlightController.getState().getAircraftLocation().getAltitude();
            double tarH = homeH - dis;

            if(homeH < 1.2 || tarH <= 0){
                mFlightController.startLanding(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {

                    }
                });
            }else{
                checkHeightLocationTrackTimer(mMode, homeH, tarH);
            }
        }
    }

    /**
     * @require
     * RollPitchControlMode: velocity
     * VerticalControlMode: velocity
     * YawControlMode: angle
     * HorizontalCoordinate: body
     *
     * @check SendVirtualStickDataTimer be active
     * @restore mPitch, mRoll, mThrottle
     * @update mYaw
     * @ensure -180 <= mYaw <= 180
     * */
    protected void mTurn(int turningDirection, int optionalTurningDegree){
        mMode = MyVirtualStickExecutorMode.TURN;
        checkSendVirtualStickDataTimer();
        destroyLocationTrackTimer();
        float currHeading = mFlightController.getCompass().getHeading();
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
    }

    /**
     * only use vectors to double check the arriving condition
     *
     * @require
     * RollPitchControlMode: velocity
     * VerticalControlMode: velocity
     * YawControlMode: angle
     * HorizontalCoordinate: body
     *
     * @check SendVirtualStickDataTimer be active
     * @restore mThrottle
     * @update mPitch, mRoll, mYaw
     * */
    protected void mGo(int movingDirection, double optionalDis){
        mMode = MyVirtualStickExecutorMode.MOVE_WITHOUT_DIS;
        checkSendVirtualStickDataTimer();
        destroyLocationTrackTimer();

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

        if(optionalDis != -1){
            mMode = MyVirtualStickExecutorMode.MOVE_DIS;
            double bearing = mFlightController.getCompass().getHeading();
            if(movingDirection==302){
                bearing -= 180;
            }else if(movingDirection==303){
                bearing -= 90;
            }else if(movingDirection==304){
                bearing += 90;
            }
            double homeLat = mFlightController.getState().getAircraftLocation().getLatitude();
            double homeLog = mFlightController.getState().getAircraftLocation().getLongitude();
            double tar[] = Utils.calcDestination(homeLat, homeLog, bearing, optionalDis);
            check2DLocationTrackTimer(mMode, homeLat, homeLog, tar[0], tar[1]);
        }
    }

    /**
     * only use vectors to double check the arriving condition
     *
     * @require
     * RollPitchControlMode: velocity
     * VerticalControlMode: velocity
     * YawControlMode: angle
     * HorizontalCoordinate: body
     *
     * @check SendVirtualStickDataTimer be active
     * @restore mThrottle
     * @update mPitch, mRoll, mYaw
     * @param tarLat
     * @param tarLog
     */
    protected void mFlyto(double tarLat, double tarLog){
        mMode = MyVirtualStickExecutorMode.FLY_TO;
        double homeLat = mFlightController.getState().getAircraftLocation().getLatitude();
        double homeLog = mFlightController.getState().getAircraftLocation().getLongitude();
        double home2tarX = tarLat- homeLat;
        double home2tarY = tarLog - homeLog;

        //set direction
        if(home2tarX < 0){
            if(home2tarY == 0){
                mYaw = -90;
            }else{
                mYaw = -(float)(Math.atan(-home2tarX/home2tarY));
            }
        }else if(home2tarX > 0){
            if(home2tarY == 0){
                mYaw = 90;
            }else{
                mYaw = (float)(Math.atan(home2tarX/home2tarY));
            }
        }else{
            //north or south or original point
            if(home2tarY > 0){
                mYaw = 0;
            }else if(home2tarY == 0){
                //do nothing, restore mYaw
                mStop();
                return;
            }else{
                mYaw = 180;
            }
        }

        //set Y velocity
        mRoll = 3;

        check2DLocationTrackTimer(mMode, homeLat, homeLog, tarLat, tarLog);
    }
}