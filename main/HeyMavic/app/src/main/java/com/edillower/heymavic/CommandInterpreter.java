package com.edillower.heymavic;


import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

import java.util.Timer;
import java.util.TimerTask;

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
import dji.sdk.flightcontroller.DJISimulator;
import dji.sdk.missionmanager.missionstep.DJIGoToStep;
import dji.sdk.products.DJIAircraft;
import dji.common.error.DJIError;

public class CommandInterpreter{

    private static String TAG = "";

    public DJIFlightController mFlightController;

//    protected TextView mConnectStatusTextView;

//    private Button mBtnEnableVirtualStick;
//    private Button mBtnDisableVirtualStick;
//    private ToggleButton mBtnSimulator;
//    private Button mBtnTakeOff;
//    private Button mBtnLand;
//    private Button mBtnSpeak;

//    // cmd test field
//    private TextView mTextCmd;
//    private Button mBtnSubmitCmd;
//
//    // fpv
//    private Button mBtnFPV;


//    private TextView mTextView;

//    private OnScreenJoystick mScreenJoystickRight;
//    private OnScreenJoystick mScreenJoystickLeft;

    private Timer mSendVirtualStickDataTimer;
    private SendVirtualStickDataTask mSendVirtualStickDataTask;

    DJIGoToStep action;

//    /** voice file store path */
//    private static final String PATH = "/sdcard/MyVoiceForder/Record/";
//
//    /** voice file name */
//    private String mVoiceFileName = null;

//    /** voice to cmd code machine */
//    private VoiceToCodeHelper mCmdCodeGenerator = new VoiceToCodeHelper(PATH, mVoiceFileName);

//    /** cmd code array */
//    private int mCmdCode[];
//    /** execute cmd */
//    private CmdCodeInterpreter mCmdInterpreter;

    private float mPitch=0;
    private float mRoll=0;
    private float mYaw=0;
    private float mThrottle=0;
//    private boolean mTurningFlag=false;

    // default constructor
    public CommandInterpreter(String activityTag){
        TAG = activityTag;
    }
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        // When the compile and target version is higher than 22, please request the
//        // following permissions at runtime to ensure the
//        // SDK work well.
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
//                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
//                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
//                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
//                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
//                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
//                            Manifest.permission.READ_PHONE_STATE,
//                    }
//                    , 1);
//        }
//
//        setContentView(com.edillower.heymavic.R.layout.activity_main);
//
//        initUI();
//
// TODO: add connection status
//        // Register the broadcast receiver for receiving the device connection's changes.
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(DJISimulatorApplication.FLAG_CONNECTION_CHANGE);
//        registerReceiver(mReceiver, filter);
//    }

//    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            updateTitleBar();
//        }
//    };

//    public void Log.e(TAG, final String msg) {
//        runOnUiThread(new Runnable() {
//            public void run() {
//                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
//            }
//        });
//    }

//    private void updateTitleBar() {
//        if(mConnectStatusTextView == null) return;
//        boolean ret = false;
//        DJIBaseProduct product = DJISimulatorApplication.getProductInstance();
//        if (product != null) {
//            if(product.isConnected()) {
//                //The product is connected
//                mConnectStatusTextView.setText(DJISimulatorApplication.getProductInstance().getModel() + " Connected");
//                ret = true;
//            } else {
//                if(product instanceof DJIAircraft) {
//                    DJIAircraft aircraft = (DJIAircraft)product;
//                    if(aircraft.getRemoteController() != null && aircraft.getRemoteController().isConnected()) {
//                        // The product is not connected, but the remote controller is connected
//                        mConnectStatusTextView.setText("only RC Connected");
//                        ret = true;
//                    }
//                }
//            }
//        }
//
//        if(!ret) {
//            // The product or the remote controller are not connected.
//            mConnectStatusTextView.setText("Disconnected");
//        }
//    }

//    @Override
//    public void onResume() {
//        Log.e(TAG, "onResume");
//        super.onResume();
//        updateTitleBar();
//        initFlightController();
//    }
//
//    @Override
//    public void onPause() {
//        Log.e(TAG, "onPause");
//        super.onPause();
//    }
//
//    @Override
//    public void onStop() {
//        Log.e(TAG, "onStop");
//        super.onStop();
//    }
//
//    public void onReturn(View view){
//        Log.e(TAG, "onReturn");
//        this.finish();
//    }
//
//    @Override
//    protected void onDestroy() {
//        Log.e(TAG, "onDestroy");
//        unregisterReceiver(mReceiver);
//        if (null != mSendVirtualStickDataTimer) {
//            mSendVirtualStickDataTask.cancel();
//            mSendVirtualStickDataTask = null;
//            mSendVirtualStickDataTimer.cancel();
//            mSendVirtualStickDataTimer.purge();
//            mSendVirtualStickDataTimer = null;
//        }
//        super.onDestroy();
//    }

    public void initFlightController() {

        DJIAircraft aircraft = DJISimulatorApplication.getAircraftInstance();
        if (aircraft == null || !aircraft.isConnected()) {
            Log.e(TAG,"Disconnected");
            mFlightController = null;
            return;
        } else {
            mFlightController = aircraft.getFlightController();
//            mFlightController.getSimulator().setUpdatedSimulatorStateDataCallback(new DJISimulator.UpdatedSimulatorStateDataCallback() {
//                @Override
//                public void onSimulatorDataUpdated(final DJISimulatorStateData djiSimulatorStateData) {
//                    new Handler(Looper.getMainLooper()).post(new Runnable() {
//                        @Override
//                        public void run() {
//
//                            String yaw = String.format("%.2f", djiSimulatorStateData.getYaw());
//                            String pitch = String.format("%.2f", djiSimulatorStateData.getPitch());
//                            String roll = String.format("%.2f", djiSimulatorStateData.getRoll());
//                            String positionX = String.format("%.2f", djiSimulatorStateData.getPositionX());
//                            String positionY = String.format("%.2f", djiSimulatorStateData.getPositionY());
//                            String positionZ = String.format("%.2f", djiSimulatorStateData.getPositionZ());
//
//                            mTextView.setText("Yaw : " + yaw + ", Pitch : " + pitch + ", Roll : " + roll + "\n" + ", PosX : " + positionX +
//                                    ", PosY : " + positionY +
//                                    ", PosZ : " + positionZ);
//                        }
//                    });
//                }
//            });
            mEnableVS();
        }
    }

    private void checkSendVirtualStickDataTimer(){
        if (null == mSendVirtualStickDataTimer) {
            mSendVirtualStickDataTask = new SendVirtualStickDataTask();
            mSendVirtualStickDataTimer = new Timer();
            mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
        }
    }
//    private void initUI() {
//
//        mBtnEnableVirtualStick = (Button) findViewById(com.edillower.heymavic.R.id.btn_enable_virtual_stick);
//        mBtnDisableVirtualStick = (Button) findViewById(com.edillower.heymavic.R.id.btn_disable_virtual_stick);
//        mBtnTakeOff = (Button) findViewById(com.edillower.heymavic.R.id.btn_take_off);
//        mBtnLand = (Button) findViewById(com.edillower.heymavic.R.id.btn_land);
//        mBtnSimulator = (ToggleButton) findViewById(com.edillower.heymavic.R.id.btn_start_simulator);
//        mTextView = (TextView) findViewById(com.edillower.heymavic.R.id.textview_simulator);
//        mConnectStatusTextView = (TextView) findViewById(com.edillower.heymavic.R.id.ConnectStatusTextView);
//        mScreenJoystickRight = (OnScreenJoystick)findViewById(com.edillower.heymavic.R.id.directionJoystickRight);
//        mScreenJoystickLeft = (OnScreenJoystick)findViewById(com.edillower.heymavic.R.id.directionJoystickLeft);
//        mBtnSpeak = (Button) findViewById(com.edillower.heymavic.R.id.btn_speak);
//
//        // cmd code test field
//        mBtnSubmitCmd = (Button) findViewById(com.edillower.heymavic.R.id.sub_btn);
//        mTextCmd = (TextView) findViewById(com.edillower.heymavic.R.id.cmd_input);
//        mBtnSubmitCmd.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                mCmdCodeGenerator.setCmdCode(mTextCmd.getText().toString());
//                mTextCmd.setText("");
//                stopVoice();
//            }
//        });
//
//        // FPV
//        mBtnFPV=(Button) findViewById(R.id.fpv_btn);
//        mBtnFPV.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                Intent fpvIntent = new Intent(MainActivity.this,FPVFullscreenActivity.class);
//                startActivity(fpvIntent);
//            }
//        });
//
//        mBtnEnableVirtualStick.setOnClickListener(this);
//        mBtnDisableVirtualStick.setOnClickListener(this);
//        mBtnTakeOff.setOnClickListener(this);
//        mBtnLand.setOnClickListener(this);
//        mBtnSpeak.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                switch(event.getAction()){
//                    case MotionEvent.ACTION_DOWN:
//                        startVoice();
//                        break;
//                    case MotionEvent.ACTION_UP:
//                        stopVoice();
//                        break;
//                    default:
//                        break;
//                }
//                return false;
//            }
//        });
//
//        mBtnSimulator.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                if (isChecked) {
//
//                    mTextView.setVisibility(View.VISIBLE);
//
//                    if (mFlightController != null) {
//                        mFlightController.getSimulator()
//                                .startSimulator(new DJISimulatorInitializationData(
//                                                23, 113, 10, 10
//                                        )
//                                        , new DJICommonCallbacks.DJICompletionCallback() {
//                                            @Override
//                                            public void onResult(DJIError djiError) {
//                                                if (djiError != null) {
//                                                    Log.e(TAG, djiError.getDescription());
//                                                }else
//                                                {
//                                                    Log.e(TAG, "Start Simulator Success");
//                                                }
//                                            }
//                                        });
//                    }
//
//                } else {
//
//                    mTextView.setVisibility(View.INVISIBLE);
//
//                    if (mFlightController != null) {
//                        mFlightController.getSimulator()
//                                .stopSimulator(
//                                        new DJICommonCallbacks.DJICompletionCallback() {
//                                            @Override
//                                            public void onResult(DJIError djiError) {
//                                                if (djiError != null) {
//                                                    Log.e(TAG, djiError.getDescription());
//                                                }else
//                                                {
//                                                    Log.e(TAG, "Stop Simulator Success");
//                                                }
//                                            }
//                                        }
//                                );
//                    }
//                }
//            }
//        });
//
//        mScreenJoystickLeft.setJoystickListener(new OnScreenJoystickListener(){
//
//            @Override
//            public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
//                Log.e(TAG, "left: "+ pX + "," + pY);
//                if(Math.abs(pX) < 0.02 ){
//                    pX = 0;
//                }
//
//                if(Math.abs(pY) < 0.02 ){
//                    pY = 0;
//                }
//                float pitchJoyControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickRollPitchControlMaxVelocity;
//                float rollJoyControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickRollPitchControlMaxVelocity;
//
//                mPitch = (float)(pitchJoyControlMaxSpeed * pY);
//
//                mRoll = (float)(rollJoyControlMaxSpeed * pX);
//
//                if (null == mSendVirtualStickDataTimer) {
//                    mSendVirtualStickDataTask = new SendVirtualStickDataTask();
//                    mSendVirtualStickDataTimer = new Timer();
//                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
//                }
//
//            }
//
//        });
//
//        mScreenJoystickRight.setJoystickListener(new OnScreenJoystickListener() {
//
//            @Override
//            public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
//                Log.e(TAG, "right: "+ pX + "," + pY);
//                if(Math.abs(pX) < 0.02 ){
//                    pX = 0;
//                }
//
//                if(Math.abs(pY) < 0.02 ){
//                    pY = 0;
//                }
//                float verticalJoyStickControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickVerticalControlMaxVelocity;
//                float yawJoyStickControlMaxSpeed = DJIFlightControllerDataType.DJIVirtualStickYawControlMaxAngularVelocity;
//
//                mYaw = (float)(yawJoyStickControlMaxSpeed * pX);
//                mThrottle = (float)(yawJoyStickControlMaxSpeed * pY);
//
//                if (null == mSendVirtualStickDataTimer) {
//                    mSendVirtualStickDataTask = new SendVirtualStickDataTask();
//                    mSendVirtualStickDataTimer = new Timer();
//                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
//                }
//
//            }
//        });
//    }

//    private void startVoice(){
//        Log.e(TAG, "Recording....");
//    }

    public void executeCmd(ArrayList<Integer> mEncoded){
        // TODO: to be modified
        int len = mEncoded.size();
        int[] mCmdCode = new int[len];
        for (int i=0; i<len; i++){
            mCmdCode[i]=mEncoded.get(i);
        }

        if(mCmdCode == null || mCmdCode.length == 0){
            Log.e(TAG, "Wrong Command Code [null]");
        }
        int idx = 0, para_dir_go = 301, para_dir=303, para_dis = -1, para_deg = 90;
        switch(mCmdCode[idx]){
            case 100:
                mYaw=(float)mFlightController.getCompass().getHeading();
                mTakeoff();
                break;
            case 101:
                mLand();
                break;
            case 102:
                mStop();
                break;
            case 103:
                mYaw=(float)mFlightController.getCompass().getHeading();
//                mFlightController.setYawControlMode(DJIVirtualStickYawControlMode.AngularVelocity);
                checkSendVirtualStickDataTimer();
                if(idx+2<mCmdCode.length && mCmdCode[idx+1]==201){
                    para_dir_go = mCmdCode[idx+2];
                    idx+=2;
                }else{
                    Log.e(TAG, "Wrong Command Code [103]");
                }

                if(idx+2<mCmdCode.length && mCmdCode[idx+1]==202){
                    para_dis = mCmdCode[idx+2];
                    idx+=2;
                }
                mGo(para_dir_go, para_dis);
                break;
            case 104:
//                mYaw = (float)mFlightController.getCompass().getHeading();
//                mFlightController.setYawControlMode(DJIVirtualStickYawControlMode.Angle);
                checkSendVirtualStickDataTimer();
                if(idx+2<mCmdCode.length && mCmdCode[idx+1]==203){
                    para_dir = mCmdCode[idx+2];
                    idx += 2;
                }else{
                    Log.e(TAG, "Wrong Command Code [104]");
                }
                if(idx+2<mCmdCode.length && mCmdCode[idx+1]==204){
                    para_deg = mCmdCode[idx+2];
                    idx += 2;
                }
                mTurn(para_dir,para_deg);
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

    private void mEnableVS(){
        if (mFlightController != null){
            mFlightController.setRollPitchControlMode(DJIVirtualStickRollPitchControlMode.Velocity);
            mFlightController.setYawControlMode(DJIVirtualStickYawControlMode.Angle);
            mFlightController.setVerticalControlMode(DJIVirtualStickVerticalControlMode.Velocity);
            mFlightController.setHorizontalCoordinateSystem(DJIVirtualStickFlightCoordinateSystem.Body);
            mFlightController.enableVirtualStickControlMode(
                    new DJICommonCallbacks.DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null){
                                Log.e(TAG, djiError.getDescription());
                            }else
                            {
                                Log.e(TAG, "Enable Virtual Stick Success");
                            }
                        }
                    }
            );
            mPitch=0;
            mRoll=0;
            mYaw=(float) mFlightController.getCompass().getHeading();
            mThrottle=0;
            checkSendVirtualStickDataTimer();
        }
    }

    private void mDisableVS(){
        if (mFlightController != null){
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

    private void mTakeoff(){
        if (mFlightController != null){
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
        }else{
            Log.e(TAG, "mFlightController == null");
        }
    }

    private void mLand(){
        if (mFlightController != null){
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
        }else{
            Log.e(TAG, "mFlightController == null");
        }

    }

    public void mStop(){
        if (!mFlightController.isVirtualStickControlModeAvailable()){
            mEnableVS();
        }
        mPitch=0;
        mRoll=0;
        mThrottle=0;
        if (action==null){
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

    private void mGo(int movingDirection, final int optionalMovingDistance){
        if (optionalMovingDistance!=-1){
            // With the optional distance parameter
//            mPitch=0;
//            mRoll=0;
//            mThrottle=0;
//            if (mSendVirtualStickDataTimer!=null){
//                if (null != mSendVirtualStickDataTimer) {
//                    mSendVirtualStickDataTask.cancel();
//                    mSendVirtualStickDataTask = null;
//                    mSendVirtualStickDataTimer.cancel();
//                    mSendVirtualStickDataTimer.purge();
//                    mSendVirtualStickDataTimer = null;
//                }
//            }
            double bearing = mFlightController.getCompass().getHeading();
            DJILocationCoordinate3D location = mFlightController.getCurrentState().getAircraftLocation();
            double lati = location.getLatitude();
            double longi = location.getLongitude();
            double[] destination;
            if (null != mSendVirtualStickDataTimer) {
//                mSendVirtualStickDataTask.cancel();
//                mSendVirtualStickDataTask = null;
                mSendVirtualStickDataTimer.cancel();
                mSendVirtualStickDataTimer.purge();
                mSendVirtualStickDataTimer = null;
            }
            switch(movingDirection){
                case 301:
                    destination=calcDestination(lati,longi,bearing,optionalMovingDistance);
                    action = new DJIGoToStep(destination[0], destination[1],new DJICommonCallbacks.DJICompletionCallback() {
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
                    destination=calcDestination(lati,longi,bearing,optionalMovingDistance);
                    action = new DJIGoToStep(destination[0], destination[1],new DJICommonCallbacks.DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                Log.e(TAG, djiError.getDescription());
                            } else {
                                Log.e(TAG, "Moving back " + optionalMovingDistance + " meters");
                            }
                            checkSendVirtualStickDataTimer();
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
                                Log.e(TAG, djiError.getDescription());
                            } else {
                                Log.e(TAG, "Moving left " + optionalMovingDistance + " meters");
                            }
                            checkSendVirtualStickDataTimer();
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
                                Log.e(TAG, djiError.getDescription());
                            } else {
                                Log.e(TAG, "Moving right " + optionalMovingDistance + " meters");
                            }
                            checkSendVirtualStickDataTimer();
                        }
                    });
                    action.setFlightSpeed(3);
                    action.run();
                    break;
                default:
                    mStop();
                    break;
            }
        }else{
            // Without the optional distance parameter
            switch(movingDirection){
                case 301:
                    mPitch=0;
                    mRoll=3;
                    mThrottle=0;
                    Log.e(TAG, "Moving Forward");
                    break;
                case 302:
                    mPitch=0;
                    mRoll=-3;
                    mThrottle=0;
                    Log.e(TAG, "Moving Back");
                    break;
                case 303:
                    mPitch=-3;
                    mRoll=0;
                    mThrottle=0;
                    Log.e(TAG, "Moving Left");
                    break;
                case 304:
                    mPitch=3;
                    mRoll=0;
                    mThrottle=0;
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

    private void mTurn(int turningDirection, int optionalTurningDegree){
//        mFlightController.setYawControlMode(DJIVirtualStickYawControlMode.Angle);
        mThrottle = 0;
        mPitch=0;
        mRoll=0;
        float currHeading = (float) mFlightController.getCompass().getHeading();
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
//        mTurningFlag=true;
        Log.e(TAG, "Turning from "+ currHeading + " to " + mYaw);
    }

    public void mDestroy(){
        mStop();
        if (null != mSendVirtualStickDataTimer) {
            mSendVirtualStickDataTask.cancel();
            mSendVirtualStickDataTask = null;
            mSendVirtualStickDataTimer.cancel();
            mSendVirtualStickDataTimer.purge();
            mSendVirtualStickDataTimer = null;
        }
        mDisableVS();
    }

//    @Override
//    public void onClick(View v) {
//
//        switch (v.getId()) {
//            case com.edillower.heymavic.R.id.btn_enable_virtual_stick:
//                if (mFlightController != null){
//                    mFlightController.enableVirtualStickControlMode(
//                            new DJICommonCallbacks.DJICompletionCallback() {
//                                @Override
//                                public void onResult(DJIError djiError) {
//                                    if (djiError != null){
//                                        Log.e(TAG, djiError.getDescription());
//                                    }else
//                                    {
//                                        Log.e(TAG, "Enable Virtual Stick Success");
//                                    }
//                                }
//                            }
//                    );
//                }
//                break;
//
//            case com.edillower.heymavic.R.id.btn_disable_virtual_stick:
//                if (mFlightController != null){
//                    mFlightController.disableVirtualStickControlMode(
//                            new DJICommonCallbacks.DJICompletionCallback() {
//                                @Override
//                                public void onResult(DJIError djiError) {
//                                    if (djiError != null) {
//                                        Log.e(TAG, djiError.getDescription());
//                                    } else {
//                                        Log.e(TAG, "Disable Virtual Stick Success");
//                                    }
//                                }
//                            }
//                    );
//                }
//                break;
//
//            case com.edillower.heymavic.R.id.btn_take_off:
//                if (mFlightController != null){
//                    mFlightController.takeOff(
//                            new DJICommonCallbacks.DJICompletionCallback() {
//                                @Override
//                                public void onResult(DJIError djiError) {
//                                    if (djiError != null) {
//                                        Log.e(TAG, djiError.getDescription());
//                                    } else {
//                                        Log.e(TAG, "Take off Success");
//                                    }
//                                }
//                            }
//                    );
//                }else{
//                    Log.e(TAG, "mFlightController == null");
//                }
//
//                break;
//
//            case com.edillower.heymavic.R.id.btn_land:
//                if (mFlightController != null){
//
//                    mFlightController.autoLanding(
//                            new DJICommonCallbacks.DJICompletionCallback() {
//                                @Override
//                                public void onResult(DJIError djiError) {
//                                    if (djiError != null) {
//                                        Log.e(TAG, djiError.getDescription());
//                                    } else {
//                                        Log.e(TAG, "AutoLand Started");
//                                    }
//                                }
//                            }
//                    );
//
//                }else{
//                    Log.e(TAG, "mFlightController == null");
//                }
//
//                break;
//
//            default:
//                break;
//        }
//    }



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
//                                else{
//                                    if (mTurningFlag) {
//                                        mFlightController.setYawControlMode(DJIVirtualStickYawControlMode.AngularVelocity);
//                                        mTurningFlag=false;
//                                    }
//                                }
                            }
                        }
                );
            }
        }
    }

}
