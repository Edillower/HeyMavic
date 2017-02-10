package com.edillower.heymavic;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Timer;
import java.util.TimerTask;

import dji.common.flightcontroller.DJISimulatorInitializationData;
import dji.common.flightcontroller.DJISimulatorStateData;
import dji.common.flightcontroller.DJIVirtualStickFlightControlData;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.flightcontroller.DJIFlightController;
import dji.common.flightcontroller.DJIFlightControllerDataType;
import dji.sdk.flightcontroller.DJISimulator;
import dji.sdk.products.DJIAircraft;
import dji.sdk.base.DJIBaseProduct;
import dji.common.error.DJIError;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getName();

    private DJIFlightController mFlightController;

    protected TextView mConnectStatusTextView;

    private Button mBtnEnableVirtualStick;
    private Button mBtnDisableVirtualStick;
    private ToggleButton mBtnSimulator;
    private Button mBtnTakeOff;
    private Button mBtnLand;
    private Button mBtnSpeak;

    // cmd test field
    private TextView mTextCmd;
    private Button mBtnSubmitCmd;

    private TextView mTextView;

    private OnScreenJoystick mScreenJoystickRight;
    private OnScreenJoystick mScreenJoystickLeft;

    private Timer mSendVirtualStickDataTimer;
    private SendVirtualStickDataTask mSendVirtualStickDataTask;

    /** voice file store path */
    private static final String PATH = "/sdcard/MyVoiceForder/Record/";

    /** voice file name */
    private String mVoiceFileName = null;

    /** voice to cmd code machine */
    private VoiceToCodeHelper mCmdCodeGenerator = new VoiceToCodeHelper(PATH, mVoiceFileName);

    /** cmd code array */
    private int mCmdCode[];
    /** execute cmd */
    private CmdCodeInterpreter mCmdInterpreter;

    private float mPitch;
    private float mRoll;
    private float mYaw;
    private float mThrottle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // When the compile and target version is higher than 22, please request the
        // following permissions at runtime to ensure the
        // SDK work well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

        setContentView(com.edillower.heymavic.R.layout.activity_main);

        initUI();

        // Register the broadcast receiver for receiving the device connection's changes.
        IntentFilter filter = new IntentFilter();
        filter.addAction(DJISimulatorApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            updateTitleBar();
        }
    };

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTitleBar() {
        if(mConnectStatusTextView == null) return;
        boolean ret = false;
        DJIBaseProduct product = DJISimulatorApplication.getProductInstance();
        if (product != null) {
            if(product.isConnected()) {
                //The product is connected
                mConnectStatusTextView.setText(DJISimulatorApplication.getProductInstance().getModel() + " Connected");
                ret = true;
            } else {
                if(product instanceof DJIAircraft) {
                    DJIAircraft aircraft = (DJIAircraft)product;
                    if(aircraft.getRemoteController() != null && aircraft.getRemoteController().isConnected()) {
                        // The product is not connected, but the remote controller is connected
                        mConnectStatusTextView.setText("only RC Connected");
                        ret = true;
                    }
                }
            }
        }

        if(!ret) {
            // The product or the remote controller are not connected.
            mConnectStatusTextView.setText("Disconnected");
        }
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        updateTitleBar();
        initFlightController();
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    public void onReturn(View view){
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        unregisterReceiver(mReceiver);
        if (null != mSendVirtualStickDataTimer) {
            mSendVirtualStickDataTask.cancel();
            mSendVirtualStickDataTask = null;
            mSendVirtualStickDataTimer.cancel();
            mSendVirtualStickDataTimer.purge();
            mSendVirtualStickDataTimer = null;
        }
        super.onDestroy();
    }

    private void initFlightController() {

        DJIAircraft aircraft = DJISimulatorApplication.getAircraftInstance();
        if (aircraft == null || !aircraft.isConnected()) {
            showToast("Disconnected");
            mFlightController = null;
            return;
        } else {
            mFlightController = aircraft.getFlightController();
            mFlightController.getSimulator().setUpdatedSimulatorStateDataCallback(new DJISimulator.UpdatedSimulatorStateDataCallback() {
                @Override
                public void onSimulatorDataUpdated(final DJISimulatorStateData djiSimulatorStateData) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {

                            String yaw = String.format("%.2f", djiSimulatorStateData.getYaw());
                            String pitch = String.format("%.2f", djiSimulatorStateData.getPitch());
                            String roll = String.format("%.2f", djiSimulatorStateData.getRoll());
                            String positionX = String.format("%.2f", djiSimulatorStateData.getPositionX());
                            String positionY = String.format("%.2f", djiSimulatorStateData.getPositionY());
                            String positionZ = String.format("%.2f", djiSimulatorStateData.getPositionZ());

                            mTextView.setText("Yaw : " + yaw + ", Pitch : " + pitch + ", Roll : " + roll + "\n" + ", PosX : " + positionX +
                                    ", PosY : " + positionY +
                                    ", PosZ : " + positionZ);
                        }
                    });
                }
            });
        }
    }

    private void initUI() {

        mBtnEnableVirtualStick = (Button) findViewById(com.edillower.heymavic.R.id.btn_enable_virtual_stick);
        mBtnDisableVirtualStick = (Button) findViewById(com.edillower.heymavic.R.id.btn_disable_virtual_stick);
        mBtnTakeOff = (Button) findViewById(com.edillower.heymavic.R.id.btn_take_off);
        mBtnLand = (Button) findViewById(com.edillower.heymavic.R.id.btn_land);
        mBtnSimulator = (ToggleButton) findViewById(com.edillower.heymavic.R.id.btn_start_simulator);
        mTextView = (TextView) findViewById(com.edillower.heymavic.R.id.textview_simulator);
        mConnectStatusTextView = (TextView) findViewById(com.edillower.heymavic.R.id.ConnectStatusTextView);
        mScreenJoystickRight = (OnScreenJoystick)findViewById(com.edillower.heymavic.R.id.directionJoystickRight);
        mScreenJoystickLeft = (OnScreenJoystick)findViewById(com.edillower.heymavic.R.id.directionJoystickLeft);
        mBtnSpeak = (Button) findViewById(com.edillower.heymavic.R.id.btn_speak);

        // cmd code test field
        mBtnSubmitCmd = (Button) findViewById(com.edillower.heymavic.R.id.sub_btn);
        mTextCmd = (TextView) findViewById(com.edillower.heymavic.R.id.cmd_input);
        mBtnSubmitCmd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCmdCodeGenerator.setCmdCode(mTextCmd.getText().toString());
                mTextCmd.setText("");
                stopVoice();
            }
        });

        mBtnEnableVirtualStick.setOnClickListener(this);
        mBtnDisableVirtualStick.setOnClickListener(this);
        mBtnTakeOff.setOnClickListener(this);
        mBtnLand.setOnClickListener(this);
        mBtnSpeak.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        startVoice();
                        break;
                    case MotionEvent.ACTION_UP:
                        stopVoice();
                        break;
                    default:
                        break;
                }
                return false;
            }
        });

        mBtnSimulator.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {

                    mTextView.setVisibility(View.VISIBLE);

                    if (mFlightController != null) {
                        mFlightController.getSimulator()
                                .startSimulator(new DJISimulatorInitializationData(
                                        23, 113, 10, 10
                                )
                                        , new DJICommonCallbacks.DJICompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {
                                        if (djiError != null) {
                                            showToast(djiError.getDescription());
                                        }else
                                        {
                                            showToast("Start Simulator Success");
                                        }
                                    }
                                });
                    }

                } else {

                    mTextView.setVisibility(View.INVISIBLE);

                    if (mFlightController != null) {
                        mFlightController.getSimulator()
                                .stopSimulator(
                                        new DJICommonCallbacks.DJICompletionCallback() {
                                            @Override
                                            public void onResult(DJIError djiError) {
                                                if (djiError != null) {
                                                    showToast(djiError.getDescription());
                                                }else
                                                {
                                                    showToast("Stop Simulator Success");
                                                }
                                            }
                                        }
                                );
                    }
                }
            }
        });

        mScreenJoystickLeft.setJoystickListener(new OnScreenJoystickListener(){

            @Override
            public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
                showToast("left: "+ pX + "," + pY);
                if(Math.abs(pX) < 0.02 ){
                    pX = 0;
                }

                if(Math.abs(pY) < 0.02 ){
                    pY = 0;
                }
                float pitchJoyControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickRollPitchControlMaxVelocity;
                float rollJoyControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickRollPitchControlMaxVelocity;

                mPitch = (float)(pitchJoyControlMaxSpeed * pY);

                mRoll = (float)(rollJoyControlMaxSpeed * pX);

                if (null == mSendVirtualStickDataTimer) {
                    mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                    mSendVirtualStickDataTimer = new Timer();
                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
                }

            }

        });

        mScreenJoystickRight.setJoystickListener(new OnScreenJoystickListener() {

            @Override
            public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
                showToast("right: "+ pX + "," + pY);
                if(Math.abs(pX) < 0.02 ){
                    pX = 0;
                }

                if(Math.abs(pY) < 0.02 ){
                    pY = 0;
                }
                float verticalJoyStickControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickVerticalControlMaxVelocity;
                float yawJoyStickControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickYawControlMaxAngularVelocity;

                mYaw = (float)(yawJoyStickControlMaxSpeed * pX);
                mThrottle = (float)(yawJoyStickControlMaxSpeed * pY);

                if (null == mSendVirtualStickDataTimer) {
                    mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                    mSendVirtualStickDataTimer = new Timer();
                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
                }

            }
        });
    }

    private void startVoice(){
        showToast("Recording....");
    }

    private void stopVoice(){
        mCmdCode = mCmdCodeGenerator.getCmdCode();

        StringBuilder sb = new StringBuilder();
        for(int i: mCmdCode){
            sb.append(i + " ");
        }
        showToast("cmd code: "+ sb.toString());

        mCmdInterpreter = new CmdCodeInterpreter(mCmdCode, mFlightController, mBtnTakeOff, mBtnLand, mScreenJoystickLeft, mScreenJoystickRight,mSendVirtualStickDataTimer,mSendVirtualStickDataTask);
        String exe_ret = mCmdInterpreter.executeCmdCode();
        showToast(exe_ret);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case com.edillower.heymavic.R.id.btn_enable_virtual_stick:
                if (mFlightController != null){
                    mFlightController.enableVirtualStickControlMode(
                            new DJICommonCallbacks.DJICompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError != null){
                                        showToast(djiError.getDescription());
                                    }else
                                    {
                                        showToast("Enable Virtual Stick Success");
                                    }
                                }
                            }
                    );
                }
                break;

            case com.edillower.heymavic.R.id.btn_disable_virtual_stick:
                if (mFlightController != null){
                    mFlightController.disableVirtualStickControlMode(
                            new DJICommonCallbacks.DJICompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError != null) {
                                        showToast(djiError.getDescription());
                                    } else {
                                        showToast("Disable Virtual Stick Success");
                                    }
                                }
                            }
                    );
                }
                break;

            case com.edillower.heymavic.R.id.btn_take_off:
                if (mFlightController != null){
                    mFlightController.takeOff(
                            new DJICommonCallbacks.DJICompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError != null) {
                                        showToast(djiError.getDescription());
                                    } else {
                                        showToast("Take off Success");
                                    }
                                }
                            }
                    );
                }else{
                    showToast("mFlightController == null");
                }

                break;

            case com.edillower.heymavic.R.id.btn_land:
                if (mFlightController != null){

                    mFlightController.autoLanding(
                            new DJICommonCallbacks.DJICompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError != null) {
                                        showToast(djiError.getDescription());
                                    } else {
                                        showToast("AutoLand Started");
                                    }
                                }
                            }
                    );

                }else{
                    showToast("mFlightController == null");
                }

                break;

            default:
                break;
        }
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

                            }
                        }
                );
            }
        }
    }

}
