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
 * Main flight control module
 * @author Eddie Wang, Eric Xu
 */
public class MyVirtualStickExecutor {
    // set up
    private MyVirtualStickExecutorMode mMode = MyVirtualStickExecutorMode.UNINITIALIZED;

    private float mSpeed = 3;
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
    private MyVirtualStickExecutor() {
    }

    public static void initFlightController() {
        mFlightController = DJISimulatorApplication.getFlightController();
    }

    /**
     * Get an istance of the virtual stick executor
     * @author Eric Xu, Eddie Wang
     * @return virtual stick executor
     */
    public static MyVirtualStickExecutor getUniqueInstance() {
        if (uniqueInstance == null) {
            uniqueInstance = new MyVirtualStickExecutor();
        }
        initFlightController();
        MyChangeSettingsExecutor.mEnableVS();
        MyChangeSettingsExecutor.setConventionVirtualStickMode();
        uniqueInstance.initYaw();
//        uniqueInstance.initAltitude();
        uniqueInstance.checkSendVirtualStickDataTimer();
        return uniqueInstance;
    }

    /**
     * Initialize the drone's heading
     * @author Eddie Wang
     */
    private void initYaw() {
        mYaw = mFlightController.getCompass().getHeading();
    }

//    private void initAltitude (){hpAltitude=mFlightController.getState().getAircraftLocation().getAltitude();}

    /**
     * Get the current altitude of the drone
     * @author Eddie Wang
     */
    private double getCurrentAltitude() {
        double alti = (double) mFlightController.getState().getAircraftLocation().getAltitude(); //-hpAltitude;
//        if (alti<18){
//            alti = (double) mFlightController.getState().getUltrasonicHeightInMeters();
//        }
        return alti;
    }

    /**
     * Change speed
     * @author Eddie Wang
     */
    protected void setSpeed(int s) {
        mSpeed = (float) s;
        if (mPitch != 0) {
            if (mPitch > 0) {
                mPitch = mSpeed;
            } else {
                mPitch = -mSpeed;
            }
        }
        if (mThrottle != 0) {
            if (mThrottle > 0) {
                mThrottle = mSpeed;
            } else {
                mThrottle = -mSpeed;
            }
        }
        if (mRoll != 0) {
            if (mRoll > 0) {
                mRoll = mSpeed;
            } else {
                mRoll = -mSpeed;
            }
        }
    }

    /**
     * Destroy the instance
     * @author Eddie
     */
    public static void destroyInstance() {
        if (uniqueInstance != null) {
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
     * Check for data sending to the drone
     * @maintainer Eric Xu
     */
    private void checkSendVirtualStickDataTimer() {
        if (mSendVirtualStickDataTimer == null) {
            mSendVirtualStickDataTask = new SendVirtualStickDataTask();
            mSendVirtualStickDataTimer = new Timer();
            mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
        }
    }

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
     * END of Check for data sending to the drone
     * @maintainer Eric Xu
     */

    /**
     * mLocationTrackTimer always has only one task: mLocationTrackTask
     * destroy timer first
     *
     * @param mode flight mode
     * @param homeH
     * @param tarH
     */
    private void checkHeightLocationTrackTimer(MyVirtualStickExecutorMode mode, double homeH, double tarH) {
        destroyLocationTrackTimer();
        if (mode == MyVirtualStickExecutorMode.UP_DIS || mode == MyVirtualStickExecutorMode.DOWN_DIS) {
            mLocationTrackTask = new LocationTrackTask(mode, homeH, tarH);
        }
        mLocationTrackTimer = new Timer();
        mLocationTrackTimer.schedule(mLocationTrackTask, 400, 200);
    }

    /**
     * mLocationTrackTimer always has only one task: mLocationTrackTask
     * destroy timer first
     *
     * @param mode
     * @param homeLat
     * @param homeLog
     * @param tarLat
     * @param tarLog
     */
    private void check2DLocationTrackTimer(MyVirtualStickExecutorMode mode, double homeLat, double homeLog, double tarLat, double tarLog) {
        destroyLocationTrackTimer();
        if (mode == MyVirtualStickExecutorMode.MOVE_DIS || mode == MyVirtualStickExecutorMode.FLY_TO) {
            mLocationTrackTask = new LocationTrackTask(mode, homeLat, homeLog, tarLat, tarLog);
        }
        mLocationTrackTimer = new Timer();
        mLocationTrackTimer.schedule(mLocationTrackTask, 1000, 200);
    }

    /**
     * destroy LocationTrackTimer when it is no need
     */
    private void destroyLocationTrackTimer() {
        if (mLocationTrackTimer != null) {
            mLocationTrackTask.cancel();
            mLocationTrackTask = null;
            mLocationTrackTimer.cancel();
            mLocationTrackTimer.purge();
            mLocationTrackTimer = null;
        }
    }

    private boolean secFlag = true;

    /**
     * a new TimerTask to check location and set four global params (mPitch, mRoll, mYaw, mThrottle)
     * when need
     * @author Eric Xu, Eddie Wang
     */
    class LocationTrackTask extends TimerTask {
        private MyVirtualStickExecutorMode m;
        private double homeH, tarH, curH, homeLat, homeLog, tarLat, tarLog, curLat, curLog;

        public LocationTrackTask(MyVirtualStickExecutorMode mode, double homeH, double tarH) {
            this.m = mode;
            this.homeH = homeH;
            this.tarH = tarH;
        }

        public LocationTrackTask(MyVirtualStickExecutorMode mode, double homeLat, double homeLog, double tarLat, double tarLog) {
            this.m = mode;
            this.homeLat = homeLat;
            this.homeLog = homeLog;
            this.tarLat = tarLat;
            this.tarLog = tarLog;
        }

        @Override
        public void run() {
            if (mFlightController != null) {
                if (m == MyVirtualStickExecutorMode.UP_DIS || m == MyVirtualStickExecutorMode.DOWN_DIS) {
                    curH = getCurrentAltitude();
                    double home2cur = curH - homeH;
                    double cur2tar = tarH - curH;
                    if (Math.abs(cur2tar) <= 0.5 || home2cur * cur2tar < 0) {
                        mThrottle = 0;
                        secFlag = false;
                    } else if (Math.abs(cur2tar) < Math.abs(mThrottle)) {
                        if (secFlag) {
                            if (mThrottle > 0) {
                                mThrottle = 1;
                            } else {
                                mThrottle = -1;
                            }
                        }
                    }
                } else if (m == MyVirtualStickExecutorMode.MOVE_DIS || m == MyVirtualStickExecutorMode.FLY_TO) {
                    curLat = mFlightController.getState().getAircraftLocation().getLatitude();
                    curLog = mFlightController.getState().getAircraftLocation().getLongitude();

                    double home2curX = curLat - homeLat;
                    double home2curY = curLog - homeLog;
                    double home2curMag = Math.sqrt(home2curX * home2curX + home2curY * home2curY);

                    double cur2tarX = tarLat - curLat;
                    double cur2tarY = tarLog - curLog;
                    double cur2tarMag = Math.sqrt(cur2tarX * cur2tarX + cur2tarY * cur2tarY);

                    double cosUp = home2curX * cur2tarX + home2curY * cur2tarY;
                    double cosDown = home2curMag * cur2tarMag;

                    // For precise stopping and hovering @author Eddie Wang
                    double dist = Utils.calcDistance(curLat, curLog, tarLat, tarLog);

                    if (dist < 0.33 || cosUp / cosDown < 0) {
                        mPitch = 0;
                        mRoll = 0;
                        secFlag = false;
                    } else if (secFlag) {
                        if (dist < Math.abs(mPitch) || dist < Math.abs(mRoll)) {
                            if (mPitch != 0) {
                                mPitch = 1;
                            }
                            if (mRoll != 0) {
                                mRoll = 1;
                            }
                        } else if (m == MyVirtualStickExecutorMode.FLY_TO) {
                            //set direction
                            mYaw = (float) Utils.calcBearing(curLat, curLog, tarLat, tarLog);
                        }
                    }
                }
            }
        }
    }

    /**
     * Stop
     * @author Eddie Wang
     */
    protected void mStop() {
        mMode = MyVirtualStickExecutorMode.STOP;
        checkSendVirtualStickDataTimer();
        destroyLocationTrackTimer();
        mPitch = 0;
        mRoll = 0;
        mThrottle = 0;
    }

    /**
     * Up
     * @author Eric Xu
     */
    protected void mUp(int dis) {
        mMode = MyVirtualStickExecutorMode.UP_WITHOUT_DIS;
        checkSendVirtualStickDataTimer();
        destroyLocationTrackTimer();
        mThrottle = mSpeed;
        if (dis != -1) {
            secFlag = true;
            mMode = MyVirtualStickExecutorMode.UP_DIS;
            double homeH = getCurrentAltitude();
            double tarH = homeH + dis;
            checkHeightLocationTrackTimer(mMode, homeH, tarH);
        }
    }

    /**
     * Down
     * @author Eric Xu
     */
    protected void mDown(int dis) {
        mMode = MyVirtualStickExecutorMode.DOWN_WITHOUT_DIS;
        checkSendVirtualStickDataTimer();
        destroyLocationTrackTimer();
        mThrottle = -mSpeed;
        if (dis != -1) {
            secFlag = true;
            mMode = MyVirtualStickExecutorMode.DOWN_DIS;
            double homeH = getCurrentAltitude();
            double tarH = homeH - dis;

            if (homeH < 1.2 || tarH <= 0) {
                mFlightController.startLanding(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {

                    }
                });
            } else {
                checkHeightLocationTrackTimer(mMode, homeH, tarH);
            }
        }
    }

    /**
     * Turn
     * @author Eric Xu
     */
    protected void mTurn(int turningDirection, int optionalTurningDegree) {
        mMode = MyVirtualStickExecutorMode.TURN;
        checkSendVirtualStickDataTimer();
        destroyLocationTrackTimer();
        float currHeading = mFlightController.getCompass().getHeading();
        mYaw = currHeading;
        if (turningDirection == 303) {
            mYaw -= optionalTurningDegree;
            if (mYaw < -180) {
                mYaw += 360;
            }
        } else {
            mYaw += optionalTurningDegree;
            if (mYaw > 180) {
                mYaw -= 360;
            }
        }
    }

    /**
     * Move
     * @author Eric Xu, Eddie Wang
     */
    protected void mGo(int movingDirection, double optionalDis) {
        mMode = MyVirtualStickExecutorMode.MOVE_WITHOUT_DIS;
        checkSendVirtualStickDataTimer();
        destroyLocationTrackTimer();

        int dir[] = {0, 1, 0, -1, 0};
        int idx = 0;
        if (movingDirection == 302) {
            idx = 2;
        } else if (movingDirection == 303) {
            idx = 3;
        } else if (movingDirection == 304) {
            idx = 1;
        }
        mPitch = (float) (mSpeed * dir[idx]);
        mRoll = (float) (mSpeed * dir[idx + 1]);

        if (optionalDis != -1) {
            secFlag = true;
            mMode = MyVirtualStickExecutorMode.MOVE_DIS;
            double bearing = mFlightController.getCompass().getHeading();
            if (movingDirection == 302) {
                bearing -= 180;
            } else if (movingDirection == 303) {
                bearing -= 90;
            } else if (movingDirection == 304) {
                bearing += 90;
            }
            double homeLat = mFlightController.getState().getAircraftLocation().getLatitude();
            double homeLog = mFlightController.getState().getAircraftLocation().getLongitude();
            double tar[] = Utils.calcDestination(homeLat, homeLog, bearing, optionalDis);
            check2DLocationTrackTimer(mMode, homeLat, homeLog, tar[0], tar[1]);
        }
    }

    /**
     * Fly to a specific location
     * @author Eddie Wang
     */
    protected void mFlyto(double tarLat, double tarLog) {
        final double initLati = mFlightController.getState().getAircraftLocation().getLatitude();
        final double initLongi = mFlightController.getState().getAircraftLocation().getLongitude();
        final double destLati = tarLat;
        final double destLogi = tarLog;
        final float targetBearing = (float) Utils.calcBearing(initLati, initLongi, destLati, destLogi);
        mMode = MyVirtualStickExecutorMode.TURN;
        checkSendVirtualStickDataTimer();
        destroyLocationTrackTimer();
        mYaw = targetBearing;
        new Thread(new Runnable() {
            public void run() {
                while (Math.abs(mFlightController.getCompass().getHeading() - targetBearing) > 1) {
                }
                secFlag = true;
                mRoll = 10;
                mMode = MyVirtualStickExecutorMode.MOVE_DIS;
                check2DLocationTrackTimer(mMode, initLati, initLongi, destLati, destLogi);
            }
        }).start();
    }
}