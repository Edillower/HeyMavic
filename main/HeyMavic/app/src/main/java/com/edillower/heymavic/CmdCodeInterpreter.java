package com.edillower.heymavic;

import android.widget.Button;

import dji.common.flightcontroller.*;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.flightcontroller.*;

import java.util.Timer;
import java.util.TimerTask;

import dji.common.error.DJIError;
import dji.sdk.missionmanager.missionstep.DJIGoToStep;


/**
 * @author  Eddie Wang, Eric Xu
 */

public class CmdCodeInterpreter{
    private DJIFlightController mFlightController;
    private Button mBtnTakeOff;
    private Button mBtnLand;
    private OnScreenJoystick mScreenJoystickRight;
    private OnScreenJoystick mScreenJoystickLeft;
    private int mCmdCode[];
    private float mPitch;
    private float mRoll;
    private float mYaw;
    private float mThrottle;

    public CmdCodeInterpreter(int[] cmd_input, DJIFlightController flightController, Button take_off, Button land, OnScreenJoystick left, OnScreenJoystick right,Timer mSendVirtualStickDataTimer, TimerTask mSendVirtualStickDataTask){
        int len = cmd_input.length;
        mCmdCode = new int[len];
        for(int i=0;i<len;++i){
            mCmdCode[i]= cmd_input[i];
        }
        mFlightController = flightController;
        mBtnTakeOff = take_off;
        mBtnLand = land;
        mScreenJoystickLeft = left;
        mScreenJoystickRight = right;
    }

    private void showToast(final String msg) {

    }

    private void land(){
        mBtnLand.performClick();
    }

    private void take_off(){
        mBtnTakeOff.performClick();
    }

    private void stop(){}

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

    private void go(int movingDirection, int optionalMovingDistance){
        if (optionalMovingDistance!=-1){
            // With the optional distance parameter
            double bearing = mFlightController.getCompass().getHeading();
            DJILocationCoordinate3D location = mFlightController.getCurrentState().getAircraftLocation();
            double lati = location.getLatitude();
            double longi = location.getLongitude();
            double[] destination;
            DJIGoToStep action;
            switch(movingDirection){
                case 301:
                    destination=calcDestination(lati,longi,bearing,optionalMovingDistance);
                    action = new DJIGoToStep(destination[0], destination[1],new DJICommonCallbacks.DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                showToast(djiError.getDescription());
                            } else {
                                showToast("");
                            }
                        }
                    });
                    action.setFlightSpeed(3);
                    action.run();
                    break;
                case 302:
                    bearing += 180;
                    destination=calcDestination(lati,longi,bearing,optionalMovingDistance);
                    action = new DJIGoToStep(destination[0], destination[1],new DJICommonCallbacks.DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                showToast(djiError.getDescription());
                            } else {
                                showToast("");
                            }
                        }
                    });
                    action.setFlightSpeed(3);
                    action.run();
                    break;
                case 303:
                    bearing -= 90;
                    destination=calcDestination(lati,longi,bearing,optionalMovingDistance);
                    action = new DJIGoToStep(destination[0], destination[1],new DJICommonCallbacks.DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                showToast(djiError.getDescription());
                            } else {
                                showToast("");
                            }
                        }
                    });
                    action.setFlightSpeed(3);
                    action.run();
                    break;
                case 304:
                    bearing += 90;
                    destination=calcDestination(lati,longi,bearing,optionalMovingDistance);
                    action = new DJIGoToStep(destination[0], destination[1],new DJICommonCallbacks.DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                showToast(djiError.getDescription());
                            } else {
                                showToast("");
                            }
                        }
                    });
                    action.setFlightSpeed(3);
                    action.run();
                    break;
                default:
                    stop();
                    break;
            }
        }else{
            // Without the optional distance parameter
            mFlightController.setRollPitchControlMode(DJIVirtualStickRollPitchControlMode.Velocity);
            switch(movingDirection){
                case 301:
                    mFlightController.sendVirtualStickFlightControlData(new DJIVirtualStickFlightControlData(0,3,0,0),new DJICommonCallbacks.DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                showToast(djiError.getDescription());
                            } else {
                                showToast("");
                            }
                        }
                    });
                    break;
                case 302:
                    mFlightController.sendVirtualStickFlightControlData(new DJIVirtualStickFlightControlData(0,-3,0,0),new DJICommonCallbacks.DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                showToast(djiError.getDescription());
                            } else {
                                showToast("");
                            }
                        }
                    });
                    break;
                case 303:
                    mFlightController.sendVirtualStickFlightControlData(new DJIVirtualStickFlightControlData(-3,0,0,0),new DJICommonCallbacks.DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                showToast(djiError.getDescription());
                            } else {
                                showToast("");
                            }
                        }
                    });
                    break;
                case 304:
                    mFlightController.sendVirtualStickFlightControlData(new DJIVirtualStickFlightControlData(3,0,0,0),new DJICommonCallbacks.DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                showToast(djiError.getDescription());
                            } else {
                                showToast("");
                            }
                        }
                    });
                    break;
                default:
                    stop();
                    break;
            }
        }
    }



    private void turn(int turningDirection, int optionalTurningDegree){
        if(mFlightController!=null) {
            mFlightController.setFlightOrientationMode(DJIFlightOrientationMode.DefaultAircraftHeading, new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        showToast(djiError.getDescription());
                    } else {
                        showToast("");
                    }
                }
            });
        }else{
            showToast("");
        }
    }

    private void up(int optionalMovingDistance){
    }

    private void down(int optionalMovingDistance){

    }

    public String executeCmdCode(){
        if(mCmdCode == null || mCmdCode.length == 0){
            //warning
            return "";
        }
        int idx = 0,para_dir=303, para_dis = 5, para_deg = 90;
        switch(mCmdCode[idx]){
            case 100:
                take_off();
                return "";
            case 101:
                land();
                return "";
            case 102:
                //perform stop
                return "";
            case 103:
                if(idx+2<mCmdCode.length && mCmdCode[idx+1]==201){
                    para_dir = mCmdCode[idx+2];
                    idx+=2;
                }else{
                    //warning
                }

                if(idx+2<mCmdCode.length && mCmdCode[idx+1]==202){
                    para_dis = mCmdCode[idx+2];
                    idx+=2;
                }
                go(para_dir, para_dis);
                return "";
            case 104:
                if(idx+2<mCmdCode.length && mCmdCode[idx+1]==203){
                    para_dir = mCmdCode[idx+2];
                    idx += 2;
                }else{
                    System.out.println("");
                }

                if(idx+2<mCmdCode.length && mCmdCode[idx+1]==204){
                    para_deg = mCmdCode[idx+2];
                    idx += 2;
                }
                turn(para_dir,para_deg);
                return "turn completed";
            case 105: break;
            case 106: break;
            default: break;
        }
        return "executeCmdCode(): should never reach here";
    }
}
