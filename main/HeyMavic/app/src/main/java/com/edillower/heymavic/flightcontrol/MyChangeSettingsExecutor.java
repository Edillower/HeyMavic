package com.edillower.heymavic.flightcontrol;

import com.edillower.heymavic.common.DJISimulatorApplication;
import com.edillower.heymavic.common.Utils;

import dji.common.error.DJIError;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

/**
 * Created by Eric on 4/3/17.
 */

public class MyChangeSettingsExecutor {
    /**
     * todo implement it later
     * @param command
     */
    public static void executeCommand(int[] command){

    }

    public static void mEnableVS() {
        FlightController fc = DJISimulatorApplication.getFlightController();
        if(fc!=null){
            fc.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {

                }
            });
        }
    }

    public static void mDisableVS() {
        FlightController fc = DJISimulatorApplication.getFlightController();
        if(fc!=null){
            fc.setVirtualStickModeEnabled(false, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {

                }
            });
        }
    }

    public static void setConventionVirtualStickMode(){
        FlightController fc = DJISimulatorApplication.getFlightController();
        if(fc!=null){
            fc.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
            fc.setYawControlMode(YawControlMode.ANGLE);
            fc.setVerticalControlMode(VerticalControlMode.VELOCITY);
            fc.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
        }
    }

    /**
     * @param height
     * @requires 20.0 <= height <= 500.0
     */
    public static void setGoHomeHeightInMeters(float height){
        FlightController fc = DJISimulatorApplication.getFlightController();
        if(fc!=null){
            fc.setGoHomeHeightInMeters(height,new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {

                }
            });
        }
    }

    /**
     * @param maxHeight
     *
     * @requires 15.0 <= maxHeight <= 500.0
     */
    public static void setMaxFlightHeight(float maxHeight){
        FlightController fc = DJISimulatorApplication.getFlightController();
        if(fc!=null){
            fc.setMaxFlightHeight(maxHeight,new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {

                }
            });
        }
    }

//    /**
//     * @param maxRadius
//     *
//     * @requires 15.0 <= maxHeight <= 500.0
//     */
//    public static void setMaxFlightRadius(float maxRadius){
//        FlightController fc = DJISimulatorApplication.getFlightController();
//        if(fc!=null){
//            fc.setMaxFlightRadius(maxRadius,new CommonCallbacks.CompletionCallback() {
//                @Override
//                public void onResult(DJIError djiError) {
//
//                }
//            });
//        }
//    }


}
