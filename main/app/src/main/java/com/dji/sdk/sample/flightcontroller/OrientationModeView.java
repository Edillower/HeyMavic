package com.dji.sdk.sample.flightcontroller;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.common.BaseThreeBtnView;
import com.dji.sdk.sample.common.DJISampleApplication;
import com.dji.sdk.sample.common.Utils;
import com.dji.sdk.sample.utils.DJIModuleVerificationUtil;

import dji.common.error.DJIError;
import dji.common.flightcontroller.DJIFlightControllerCurrentState;
import dji.common.flightcontroller.DJIFlightControllerDataType;
import dji.common.flightcontroller.DJIFlightOrientationMode;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.flightcontroller.DJIFlightController;
import dji.sdk.flightcontroller.DJIFlightControllerDelegate;

/**
 * Class for Orientation mode.
 */
public class OrientationModeView extends BaseThreeBtnView{

    private static final int CHANGE_DESCRIPTION_TEXTVIEW = 0;

    private DJIFlightController mFlightController;

    private String orientationMode;

    private Handler mHandler = new Handler(new Handler.Callback(){

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case CHANGE_DESCRIPTION_TEXTVIEW :
                    mTexInfo.setText("Current Orientation Mode is" + "\n" +
                                orientationMode);
                    break;

                default:
                    break;
            }
            return false;
        }
    });

    public OrientationModeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (DJIModuleVerificationUtil.isFlightControllerAvailable()) {
            mFlightController = DJISampleApplication.getAircraftInstance().getFlightController();

            mFlightController.setUpdateSystemStateCallback(
                    new DJIFlightControllerDelegate.FlightControllerUpdateSystemStateCallback() {
                @Override
                public void onResult(DJIFlightControllerCurrentState
                                             djiFlightControllerCurrentState) {
                    orientationMode = djiFlightControllerCurrentState.getOrientaionMode().name();
                    mHandler.sendEmptyMessage(CHANGE_DESCRIPTION_TEXTVIEW);
                }
            });
        }
    }

    @Override
    protected int getMiddleBtnTextResourceId() {
        return R.string.orientation_mode_home_lock;
    }

    @Override
    protected int getLeftBtnTextResourceId() {
        return R.string.orientation_mode_course_lock;
    }

    @Override
    protected int getRightBtnTextResourceId() {
        return R.string.orientation_mode_aircraft_heading;
    }

    @Override
    protected int getInfoResourceId() {
        return R.string.orientation_mode_description;
    }

    @Override
    protected void getMiddleBtnMethod() {
        if (DJIModuleVerificationUtil.isFlightControllerAvailable()) {
            mFlightController = DJISampleApplication.getAircraftInstance().getFlightController();

            mFlightController.setFlightOrientationMode(
                    DJIFlightOrientationMode.HomeLock,
                    new DJICommonCallbacks.DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            Utils.setResultToToast(
                                    getContext(),
                                    "Result: " + (djiError == null ?
                                            "Success" : djiError.getDescription()));
                        }
                    }
            );
        }
    }

    @Override
    protected void getLeftBtnMethod() {
        if (DJIModuleVerificationUtil.isFlightControllerAvailable()) {
            mFlightController = DJISampleApplication.getAircraftInstance().getFlightController();

            mFlightController.setFlightOrientationMode(
                    DJIFlightOrientationMode.CourseLock,
                    new DJICommonCallbacks.DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            Utils.setResultToToast(
                                    getContext(),
                                    "Result: " + (djiError == null ?
                                            "Success" : djiError.getDescription()));
                        }
                    }
            );
        }
    }

    @Override
    protected void getRightBtnMethod() {
        if (DJIModuleVerificationUtil.isFlightControllerAvailable()) {
            mFlightController = DJISampleApplication.getAircraftInstance().getFlightController();

            mFlightController.setFlightOrientationMode(
                    DJIFlightOrientationMode.DefaultAircraftHeading,
                    new DJICommonCallbacks.DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            Utils.setResultToToast(
                                    getContext(),
                                    "Result: " + (djiError == null ?
                                            "Success" : djiError.getDescription()));
                        }
                    }
            );
        }
    }
}
