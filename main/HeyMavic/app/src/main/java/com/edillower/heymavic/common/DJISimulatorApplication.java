package com.edillower.heymavic.common;

import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;


public class DJISimulatorApplication extends Application {

    private static final String TAG = DJISimulatorApplication.class.getName();

    public static final String FLAG_CONNECTION_CHANGE = "com_edillower_heymavic_connection_change";

    private static BaseProduct product;
    private static DJISimulatorApplication instance;

    private Handler mHandler;

    /**
     * Gets instance of the specific product connected after the
     * API KEY is successfully validated. Please make sure the
     * API_KEY has been added in the Manifest
     */
    public static synchronized BaseProduct getProductInstance() {
        if (null == product) {
            product = DJISDKManager.getInstance().getProduct();
        }
        return product;
    }

    public static boolean isAircraftConnected() {
        return getProductInstance() != null && getProductInstance() instanceof Aircraft;
    }

    public static synchronized Aircraft getAircraftInstance() {
        if (!isAircraftConnected()) return null;
        return (Aircraft) getProductInstance();
    }

    //added by keao xu
    public static FlightController getFlightController(){
        Aircraft aircraft = getAircraftInstance();
        if (aircraft == null || !aircraft.isConnected()) {
            return null;
        } else {
            return aircraft.getFlightController();
        }
    }

    public static DJISimulatorApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(Looper.getMainLooper());

        /*
         * handles SDK Registration using the API_KEY
         */

        int permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE);
        if (permissionCheck == 0 || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            DJISDKManager.getInstance().registerApp(this, mDJISDKManagerCallback);
        }
        instance = this;
    }

    private DJISDKManager.SDKManagerCallback mDJISDKManagerCallback = new DJISDKManager.SDKManagerCallback() {

        @Override
        public void onRegister(DJIError error) {
            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                DJISDKManager.getInstance().startConnectionToProduct();
            } else {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "R.string.sdk_registration_message", Toast.LENGTH_LONG)
                                .show();
                    }
                });
            }
            Log.v(TAG, error.getDescription());
        }

        @Override
        public void onProductChange(BaseProduct oldProduct, BaseProduct newProduct) {

            Log.d(TAG, String.format("onProductChanged oldProduct:%s, newProduct:%s", oldProduct, newProduct));
            product = newProduct;
            if (product != null) {
                product.setBaseProductListener(mDJIBaseProductListener);
            }

            notifyStatusChange();
        }

        private BaseProduct.BaseProductListener mDJIBaseProductListener = new BaseProduct.BaseProductListener() {

            @Override
            public void onComponentChange(BaseProduct.ComponentKey key,
                                          BaseComponent oldComponent,
                                          BaseComponent newComponent) {

                if (newComponent != null) {
                    newComponent.setComponentListener(mDJIComponentListener);
                }
                Log.d(TAG,
                        String.format("onComponentChange key:%s, oldComponent:%s, newComponent:%s",
                                key,
                                oldComponent,
                                newComponent));

                notifyStatusChange();
            }

            @Override
            public void onConnectivityChange(boolean isConnected) {

                Log.d(TAG, "onProductConnectivityChanged: " + isConnected);

                notifyStatusChange();
            }
        };

        private BaseComponent.ComponentListener mDJIComponentListener = new BaseComponent.ComponentListener() {

            @Override
            public void onConnectivityChange(boolean isConnected) {
                Log.d(TAG, "onComponentConnectivityChanged: " + isConnected);
                notifyStatusChange();
            }
        };

        private void notifyStatusChange() {
            mHandler.removeCallbacks(updateRunnable);
            mHandler.postDelayed(updateRunnable, 500);
        }

        private Runnable updateRunnable = new Runnable() {

            @Override
            public void run() {
                Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
                sendBroadcast(intent);
            }
        };
    };

}
