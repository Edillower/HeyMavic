package com.edillower.heymavic.flightcontrol;

import android.content.Context;
import android.support.annotation.NonNull;

import com.edillower.heymavic.common.DJISimulatorApplication;

import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.FlightMode;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

/**
 * Created by Eric on 4/3/17.
 */

public class MyAbstractMission {
    protected FlightController flightController;

    protected double homeLatitude = 181;
    protected double homeLongitude = 181;
    protected FlightMode flightState = null;

    public MyAbstractMission(){
        BaseProduct product = DJISimulatorApplication.getProductInstance();
        if (product == null || !product.isConnected()) {
            flightController = null;
        } else {
            if (product instanceof Aircraft) {
                flightController = ((Aircraft) product).getFlightController();
            }

            if (flightController != null) {

                flightController.setStateCallback(new FlightControllerState.Callback() {
                    @Override
                    public void onUpdate(@NonNull FlightControllerState flightControllerState) {
                        homeLatitude = flightControllerState.getHomeLocation().getLatitude();
                        homeLongitude = flightControllerState.getHomeLocation().getLongitude();
                        flightState = flightControllerState.getFlightMode();
                    }
                });
            }
        }
    }
    protected void load(){}
    protected void upload(){}
    protected void start(){}
    protected void stop(){}
}
