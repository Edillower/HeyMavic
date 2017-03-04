package com.edillower.heymavic;


import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

import java.util.Timer;
import java.util.TimerTask;

import dji.common.flightcontroller.DJIFlightControllerCurrentState;
import dji.common.flightcontroller.DJILocationCoordinate3D;
import dji.common.flightcontroller.DJISimulatorInitializationData;
import dji.common.flightcontroller.DJISimulatorStateData;
import dji.common.flightcontroller.DJIVirtualStickFlightControlData;
import dji.common.flightcontroller.DJIVirtualStickFlightCoordinateSystem;
import dji.common.flightcontroller.DJIVirtualStickRollPitchControlMode;
import dji.common.flightcontroller.DJIVirtualStickVerticalControlMode;
import dji.common.flightcontroller.DJIVirtualStickYawControlMode;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.flightcontroller.DJIFlightController;
import dji.sdk.flightcontroller.DJIFlightControllerDelegate;
import dji.sdk.flightcontroller.DJISimulator;
import dji.sdk.missionmanager.DJIMission;
import dji.sdk.missionmanager.DJIMissionManager;
import dji.sdk.missionmanager.missionstep.DJIGoToStep;
import dji.sdk.products.DJIAircraft;
import dji.common.error.DJIError;

public class CommandInterpreter {

    private static String TAG = "";

    public DJIAircraft aircraft;
    public DJIFlightController mFlightController;

    private Timer mSendVirtualStickDataTimer;
    private SendVirtualStickDataTask mSendVirtualStickDataTask;

    DJIGoToStep action;

    private float mPitch = 0;
    private float mRoll = 0;
    private float mYaw = 0;
    private float mThrottle = 0;

    // Mission Manager
    protected DJIMission mDJIMission;
    private DJIMissionManager mMissionManager;

    // default constructor
    public CommandInterpreter(String activityTag) {
        TAG = activityTag;
    }


    public void initFlightController() {

        aircraft = DJISimulatorApplication.getAircraftInstance();
        if (aircraft == null || !aircraft.isConnected()) {
            Log.e(TAG, "Disconnected");
            mFlightController = null;
            return;
        } else {
            mFlightController = aircraft.getFlightController();
            mEnableVS();
        }
    }

    private void checkSendVirtualStickDataTimer() {
        if (null == mSendVirtualStickDataTimer) {
            mSendVirtualStickDataTask = new SendVirtualStickDataTask();
            mSendVirtualStickDataTimer = new Timer();
            mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
        }
    }

    private void destroySendVirtualStickDataTask() {
        if (mSendVirtualStickDataTask != null) {
            mSendVirtualStickDataTask.cancel();
            mSendVirtualStickDataTask = null;
        }
    }

    private void checkSendVirtualStickDataTask() {
        if (mSendVirtualStickDataTimer != null && mSendVirtualStickDataTask == null) {
            mSendVirtualStickDataTask = new SendVirtualStickDataTask();
            mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
        }
    }

    public void executeCmd(ArrayList<Integer> mEncoded) {
        // TODO: to be modified
        int len = mEncoded.size();
        int[] mCmdCode = new int[len];
        for (int i = 0; i < len; i++) {
            mCmdCode[i] = mEncoded.get(i);
        }

        if (mCmdCode == null || mCmdCode.length == 0) {
            Log.e(TAG, "Wrong Command Code [null]");
        }
        int idx = 0, para_dir_go = 301, para_dir = 303, para_dis = -1, para_deg = 90;
        switch (mCmdCode[idx]) {
            case 100:
                mYaw = (float) mFlightController.getCompass().getHeading();
                mTakeoff();
                break;
            case 101:
                mLand();
                break;
            case 102:
                mStop();
                break;
            case 103:
                mYaw = (float) mFlightController.getCompass().getHeading();
                if (idx + 2 < mCmdCode.length && mCmdCode[idx + 1] == 201) {
                    para_dir_go = mCmdCode[idx + 2];
                    idx += 2;
                } else {
                    Log.e(TAG, "Wrong Command Code [103]");
                }

                if (idx + 2 < mCmdCode.length && mCmdCode[idx + 1] == 202) {
                    para_dis = mCmdCode[idx + 2];
                    idx += 2;
                }
                mGo(para_dir_go, para_dis);
                break;
            case 104:
                checkSendVirtualStickDataTimer();
                if (idx + 2 < mCmdCode.length && mCmdCode[idx + 1] == 203) {
                    para_dir = mCmdCode[idx + 2];
                    idx += 2;
                } else {
                    Log.e(TAG, "Wrong Command Code [104]");
                }
                if (idx + 2 < mCmdCode.length && mCmdCode[idx + 1] == 204) {
                    para_deg = mCmdCode[idx + 2];
                    idx += 2;
                }
                mTurn(para_dir, para_deg);
                break;
//            case 105:
//                // TODO: implement increase
//                break;
//            case 106:
//                // TODO: implement decrease height
//                break;
            default:
                mStop();
                break;
        }
    }

    private void mEnableVS() {
        if (mFlightController != null) {
            mFlightController.setRollPitchControlMode(DJIVirtualStickRollPitchControlMode.Velocity);
            mFlightController.setYawControlMode(DJIVirtualStickYawControlMode.Angle);
            mFlightController.setVerticalControlMode(DJIVirtualStickVerticalControlMode.Velocity);
            mFlightController.setHorizontalCoordinateSystem(DJIVirtualStickFlightCoordinateSystem.Body);
            mFlightController.enableVirtualStickControlMode(
                    new DJICommonCallbacks.DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                Log.e(TAG, djiError.getDescription());
                            } else {
                                Log.e(TAG, "Enable Virtual Stick Success");
                            }
                        }
                    }
            );
            mPitch = 0;
            mRoll = 0;
            mYaw = (float) mFlightController.getCompass().getHeading();
            mThrottle = 0;
            checkSendVirtualStickDataTimer();
        }
    }

    private void mDisableVS() {
        if (mFlightController != null) {
            mFlightController.disableVirtualStickControlMode(
                    new DJICommonCallbacks.DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                Log.e(TAG, djiError.getDescription());
                            } else {
                                Log.e(TAG, "Disable Virtual Stick Success");
                            }
                        }
                    }
            );
        }
    }

    private void mTakeoff() {
        if (mFlightController != null) {
            mFlightController.takeOff(
                    new DJICommonCallbacks.DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                Log.e(TAG, djiError.getDescription());
                            } else {
                                Log.e(TAG, "Take off Success");
                            }
                        }
                    }
            );
        } else {
            Log.e(TAG, "mFlightController == null");
        }
    }

    private void mLand() {
        if (mFlightController != null) {
            mFlightController.autoLanding(
                    new DJICommonCallbacks.DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                Log.e(TAG, djiError.getDescription());
                            } else {
                                Log.e(TAG, "AutoLand Started");
                            }
                        }
                    }
            );
        } else {
            Log.e(TAG, "mFlightController == null");
        }

    }

    public void mStop() {
        if (!mFlightController.isVirtualStickControlModeAvailable()) {
            mEnableVS();
        }
        mPitch = 0;
        mRoll = 0;
        mThrottle = 0;
        if (action == null) {
            checkSendVirtualStickDataTimer();
        } else {
            action.onCancel(new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        Log.e(TAG, djiError.getDescription());
                    }
                }
            });
        }
        Log.e(TAG, "Stopping");
    }

    private void mGo(int movingDirection, final int optionalMovingDistance) {
        if (optionalMovingDistance != -1) {
            // With the optional distance parameter
//            mDestroy();
            double bearing = mFlightController.getCompass().getHeading();
            DJILocationCoordinate3D location = mFlightController.getCurrentState().getAircraftLocation();
            double lati = location.getLatitude();
            double longi = location.getLongitude();
            double[] destination;
            switch (movingDirection) {
                case 301:
                    destination = calcDestination(lati, longi, bearing, optionalMovingDistance);
                    action = new DJIGoToStep(destination[0], destination[1], new DJICommonCallbacks.DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                Log.e(TAG, djiError.getDescription());
                            } else {
                                Log.e(TAG, "Moving forward " + optionalMovingDistance + " meters");
                            }
                        }
                    });
                    action.setFlightSpeed(3);
                    action.run();
                    break;
                case 302:
                    bearing += 180;
                    destination = calcDestination(lati, longi, bearing, optionalMovingDistance);
                    action = new DJIGoToStep(destination[0], destination[1], new DJICommonCallbacks.DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                Log.e(TAG, djiError.getDescription());
                            } else {
                                Log.e(TAG, "Moving back " + optionalMovingDistance + " meters");
                            }
                        }
                    });
                    action.setFlightSpeed(3);
                    action.run();
                    break;
                case 303:
                    bearing -= 90;
                    destination = calcDestination(lati, longi, bearing, optionalMovingDistance);
                    action = new DJIGoToStep(destination[0], destination[1], new DJICommonCallbacks.DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                Log.e(TAG, djiError.getDescription());
                            } else {
                                Log.e(TAG, "Moving left " + optionalMovingDistance + " meters");
                            }
                        }
                    });
                    action.setFlightSpeed(3);
                    action.run();
                    break;
                case 304:
                    bearing += 90;
                    destination = calcDestination(lati, longi, bearing, optionalMovingDistance);
                    action = new DJIGoToStep(destination[0], destination[1], new DJICommonCallbacks.DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                Log.e(TAG, djiError.getDescription());
                            } else {
                                Log.e(TAG, "Moving right " + optionalMovingDistance + " meters");
                            }
                        }
                    });
                    action.setFlightSpeed(3);
                    action.run();
                    break;
                default:
                    mStop();
                    break;
            }
        } else {
            // Without the optional distance parameter
            checkSendVirtualStickDataTimer();
            switch (movingDirection) {
                case 301:
                    mPitch = 0;
                    mRoll = 3;
                    mThrottle = 0;
                    Log.e(TAG, "Moving Forward");
                    break;
                case 302:
                    mPitch = 0;
                    mRoll = -3;
                    mThrottle = 0;
                    Log.e(TAG, "Moving Back");
                    break;
                case 303:
                    mPitch = -3;
                    mRoll = 0;
                    mThrottle = 0;
                    Log.e(TAG, "Moving Left");
                    break;
                case 304:
                    mPitch = 3;
                    mRoll = 0;
                    mThrottle = 0;
                    Log.e(TAG, "Moving Right");
                    break;
                default:
                    mStop();
                    break;
            }
        }

    }
    /*
 *  Calculate the destination coordinates by origin coordinates, heading direction, and moving distance
 *  @param
 *      double lati: origin latitude in decimal degrees
 *      double longi: origin longitude in decimal degrees
 *      double bearing: heading direction in decimal degrees, clockwise from the north
 *      double distance: moving distance in meters
 *  @return
 *      double[] destination: destination[0]=latitude in decimal degrees, destination[1]=longitude in decimal degrees
 */
    private static double[] calcDestination(double lati, double longi,
                                            double bearing, double distance) {
        double[] destination = new double[2]; // double[0]=latitude double[1]=longitude

        // Setup parameters
        double radius = 6371000; // Earth radius in meters
        double ber = bearing; // Heading direction, clockwise from north
        if (bearing < 0) {
            ber = 360 - ber;
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

    private void mTurn(int turningDirection, int optionalTurningDegree) {
        mThrottle = 0;
        mPitch = 0;
        mRoll = 0;
        float currHeading = (float) mFlightController.getCompass().getHeading();
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
        Log.e(TAG, "Turning from " + currHeading + " to " + mYaw);
    }

    public void mDestroy() {
        mStop();
        if (null != mSendVirtualStickDataTimer) {
            if (null != mSendVirtualStickDataTask) {
                mSendVirtualStickDataTask.cancel();
                mSendVirtualStickDataTask = null;
            }
            mSendVirtualStickDataTimer.cancel();
            mSendVirtualStickDataTimer.purge();
            mSendVirtualStickDataTimer = null;
        }
        mDisableVS();
    }

    class SendVirtualStickDataTask extends TimerTask {
        @Override
        public void run() {

            if (mFlightController != null) {
                mFlightController.sendVirtualStickFlightControlData(
                        new DJIVirtualStickFlightControlData(
                                mPitch, mRoll, mYaw, mThrottle
                        ), new DJICommonCallbacks.DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError != null) {
                                    Log.e(TAG, djiError.getDescription());
                                }
                            }
                        }
                );
            }
        }
    }



}
