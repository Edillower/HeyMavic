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
        fc.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
        fc.setYawControlMode(YawControlMode.ANGLE);
        fc.setVerticalControlMode(VerticalControlMode.VELOCITY);
        fc.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
    }



}
