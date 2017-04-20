package com.edillower.heymavic;

import android.Manifest;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.edillower.heymavic.common.DJISimulatorApplication;
import com.edillower.heymavic.flightcontrol.CommandInterpreter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneInputStream;
import com.ibm.watson.developer_cloud.android.library.audio.utils.ContentType;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechResults;
import com.ibm.watson.developer_cloud.speech_to_text.v1.websocket.RecognizeCallback;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import dji.common.battery.BatteryState;
import dji.common.flightcontroller.FlightControllerState;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;

/**
 * FPV main control window
 *
 * @author Eddie Wang
 */
public class FPVFullscreenActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, CommandConfirmationDialogFragment.Communicator {
    public static final String TAG = FPVFullscreenActivity.class.getName();

    private Context mContext;

    private CommandInterpreter mCI;

    private FirebaseDatabase mDatabase;
    private DatabaseReference mDBRecog;

    // Map
    private View mMapView;
    private GoogleMap mMap;
    private LatLng mDroneLocation = new LatLng(0, 0);
    private float mDroneHeading = 0;
    private Marker mDroneMarker = null;
    //    private LocationManager mLocationManager;
    //    private LocationListener mLocationListener;
    private GoogleApiClient mGoogleApiClient;
    private LatLng mUserLocation = new LatLng(0, 0);
    private Button mBtnLoacte;
    private boolean mMapLocate_flag = true;
    private Button mBtnTracking;
    private boolean mMapTracking_flag = true;

    private PlaceListFragment mPlaceListFragment;


    // IBM watson varaibles
    private WatsonCommandClassifier cc1;
    private SpeechToText speechService;
    private MicrophoneInputStream capture;
    private String mStrIntention;

    // App button and views
    private TextureView fpvTexture;
    private Button mBtnInput;
    private boolean mBtnInput_flag = true;
    private EditText mTxtCmmand;
    private Button mBtnStop;
    private Button mBtnDummy;
    private Button mBtnDummyMap;
    private boolean mBtnDummyMap_flag = true;
    private Button mBtnShow;
    private Button mBtnHide;

    //Aircraft State
    private TextView mAltitude;
    private TextView mVerSpeed;
    private TextView mHorSpeed;
    private TextView mDistance;
    //    private TextView mDistance;
    private double mAltitudeData;
    private double mvs;
    private double mhs;
    private double mdistToHome;
    //Battery
    BatteryView mBatteryView;
    private TextView mBatteryData;
    private int mBatteryPercent;

    // Retrieve and Rank fragment
    private RARFragment rarFragment;
    private boolean rarFlag;
    private Button mRandR;

    // Test
    private Button mTest;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);

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
                            Manifest.permission.READ_PHONE_STATE, Manifest.permission.RECORD_AUDIO
                    }
                    , 1);
        }

        mContext = this;
        fpvTexture = new TextureView(mContext);
        fpvTexture.setSurfaceTextureListener(new BaseFpvView(mContext));
        setContentView(fpvTexture);

        LayoutInflater layoutInflater = getLayoutInflater();
        View content = layoutInflater.inflate(R.layout.activity_fpvfullscreen, null, false);
        RelativeLayout.LayoutParams rlParam = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        addContentView(content, rlParam);
        initUI();

        speechService = initSpeechToTextService();
        cc1 = new WatsonCommandClassifier();

        mCI = CommandInterpreter.getUniqueInstance(mContext);
        initDrone();

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        // Register the broadcast receiver for receiving the device connection's changes.
        IntentFilter filter = new IntentFilter();
        filter.addAction(DJISimulatorApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        // Set up firebase
        mDatabase = FirebaseDatabase.getInstance();
        mDBRecog = mDatabase.getReference("recog");
        // Set up map
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);

        Log.e(TAG, "onCreate");
    }

    @Override
    public void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        updateConnection();
        initDrone();
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    public void onReturn(View view) {
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        unregisterReceiver(mReceiver);
        //eric command this out, april 3
//        mCI.mDestroy();
        super.onDestroy();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setAllGesturesEnabled(false);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        } else {
            showFpvToast("Permission required for using map");
        }
        mMapView = findViewById(R.id.mapFragment);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        mMapLocate_flag = false;
        updateMapCamera();
        mMapLocate_flag = true;
    }

    @Override
    public void onConnectionSuspended(int cause) {
        showFpvToast("Google Play Suspended: " + cause);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        showFpvToast("Failed to Connect Google Play");
    }

    private void updateUserLocation() {
        try {
            Location loc = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            mUserLocation = new LatLng(loc.getLatitude(), loc.getLongitude());
        } catch (SecurityException e) {
            showFpvToast("Permission required for using map");
        }
    }

    // display command confirmation window
    public void showDialog(View v) {
        // create FragmentManager and CommandConfirmationDialogFragment
        FragmentManager manager = getFragmentManager();
        CommandConfirmationDialogFragment myDialogFragment = new CommandConfirmationDialogFragment();
        // send encoded_string and command into pop up window
        Bundle bundle = new Bundle();
        bundle.putString("encoded_string", cc1.getEncodedString().toString());
        bundle.putString("command", cc1.getCommand());
        myDialogFragment.setArguments(bundle);
        // show pop up window
        myDialogFragment.show(manager, "MyDialogFragment");
    }

    // exectue based on user feedback from command confirmation window
    @Override
    public void onDialogMessage(boolean message) {
        if (message) {
            writeRecogRecord(true, mStrIntention, cc1.getEncodedString().toString(), cc1.getCommand());
//            showFpvToast("Start executing command");
            preCheck(cc1.getEncodedString(), cc1.getGoogleMapSearchString()); // Start execution
        } else {
            writeRecogRecord(false, mStrIntention, cc1.getEncodedString().toString(), cc1.getCommand());
            showFpvToast("Command cancelled");
        }
    }

    //    private class custLocationListener implements LocationListener {
//        @Override
//        public void onLocationChanged(Location loc) {
//            mUserLocation = new LatLng(loc.getLatitude(), loc.getLongitude());
//        }
//
//        @Override
//        public void onProviderDisabled(String provider) {
//        }
//
//        @Override
//        public void onProviderEnabled(String provider) {
//        }
//
//        @Override
//        public void onStatusChanged(String provider, int status, Bundle extras) {
//        }
//    }


//    private void updateUserLocation() {
//        mLocationManager = (LocationManager)
//                getSystemService(Context.LOCATION_SERVICE);
//        mLocationListener = new custLocationListener();
//        try {
//            mLocationManager.requestLocationUpdates(
//                    LocationManager.GPS_PROVIDER, 5000, 0, mLocationListener);
//        } catch (SecurityException e) {
//            showFpvToast("Permission required for using map");
//        }
//    }

    // Update the drone location based on states from MCU.
    private void updateDroneLocation() {

        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(mDroneLocation);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));
        markerOptions.rotation(mDroneHeading);
        markerOptions.anchor(0.5f, 0.618f);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mDroneMarker != null) {
                    mDroneMarker.remove();
                }
                if (checkGpsCoordination(mDroneLocation.latitude, mDroneLocation.longitude)) {
                    mDroneMarker = mMap.addMarker(markerOptions);
                    if (mMapTracking_flag) {
                        updateMapCamera();
                    }
                }
            }
        });
    }


    private void updateMapCamera() {
        if (mMapLocate_flag) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDroneLocation, 15.0f));
        } else {
            updateUserLocation();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mUserLocation, 15.0f));
        }
    }

    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }


    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            updateConnection();
            if (mCI.mFlightController == null) {
                initDrone();
            }
        }
    };

    private void updateConnection() {
//        boolean ret = false;
        BaseProduct product = DJISimulatorApplication.getProductInstance();
        if (product != null) {
            if (product.isConnected()) {
                //The product is connected
                showFpvToast(DJISimulatorApplication.getProductInstance().getModel() + " Connected");
//                ret = true;
            } else {
                if (product instanceof Aircraft) {
                    Aircraft aircraft = (Aircraft) product;
                    if (aircraft.getRemoteController() != null && aircraft.getRemoteController().isConnected()) {
                        // The product is not connected, but the remote controller is connected
                        showFpvToast("only RC Connected");
//                        ret = true;
                    }
                }
            }
        }

//        if (!ret) {
//            // The product or the remote controller are not connected.
//            showFpvToast("Disconnected");
//        }
    }


    /*
    Retrieve and Rank button
 */
    private void RRInputListener() {
        mRandR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rarFlag = !rarFlag;
                if (rarFlag) {
                    mBtnDummy.setVisibility(View.GONE);
                    mBtnInput.setVisibility(View.GONE);
                    mTxtCmmand.setVisibility(View.GONE);
                    rarFragment = new RARFragment();
                    getSupportFragmentManager().beginTransaction().add(R.id.main_layout, rarFragment).commit();
                } else {
                    mBtnDummy.setVisibility(View.VISIBLE);
                    mBtnInput.setVisibility(View.VISIBLE);
                    mTxtCmmand.setVisibility(View.VISIBLE);
                    getSupportFragmentManager().beginTransaction().remove(rarFragment).commit();
                }
            }
        });
    }

//    private double initAltitude = 0;
//    private boolean altiFlag = true;

    private void initDrone() {
        mCI.initFlightController();
        if (mCI.mFlightController != null) {
            mCI.setPhotoMode();
            showFpvToast("Set up call back");
            if (mCI.mFlightController.isVirtualStickControlModeAvailable()) {
                mBtnStop.setVisibility(View.VISIBLE);
            }

            mCI.mFlightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(@NonNull FlightControllerState flightControllerState) {
                    double mDroneLocationLat = flightControllerState.getAircraftLocation().getLatitude();
                    double mDroneLocationLng = flightControllerState.getAircraftLocation().getLongitude();
                    mDroneLocation = new LatLng(mDroneLocationLat, mDroneLocationLng);
                    mDroneHeading = mCI.mFlightController.getCompass().getHeading();
                    updateDroneLocation();
                    // set flight data
                    mAltitudeData = (double) flightControllerState.getAircraftLocation().getAltitude(); // - initAltitude;
//                    if (mAltitudeData < 18) {
//                        mAltitudeData = (double) flightControllerState.getUltrasonicHeightInMeters();
//                    }
                    mhs = Math.sqrt(flightControllerState.getVelocityX() * flightControllerState.getVelocityX()
                            + flightControllerState.getVelocityY() * flightControllerState.getVelocityY());
                    mvs = -1 * flightControllerState.getVelocityZ();
                    mdistToHome = Utils.calcDistance(mUserLocation.latitude, mUserLocation.longitude, mDroneLocation.latitude, mDroneLocation.longitude);
                    updateFlightData();
                }
            });

            mTest.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCI.shootPhoto();
                }
            });

            // set up battery
            mCI.aircraft.getBattery().setStateCallback(new BatteryState.Callback() {
                @Override
                public void onUpdate(BatteryState batteryState) {
                    mBatteryPercent = batteryState.getChargeRemainingInPercent();
                    mBatteryView.setProgress(mBatteryPercent);
                    updateBatteryStatus();
                }
            });
        }

    }

    private void updateFlightData() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDistance.setText("D: " + new DecimalFormat("####").format(mdistToHome) + "m");
                mAltitude.setText("H: " + new DecimalFormat("###.#").format(mAltitudeData) + "m");
                mVerSpeed.setText("V.S: " + new DecimalFormat("##.#").format(mvs) + "m/s");
                mHorSpeed.setText("H.S: " + new DecimalFormat("##.#").format(mhs) + "m/s");
            }
        });
    }

    private void updateBatteryStatus() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBatteryData.setText(Integer.toString(mBatteryPercent) + "%");
            }
        });
    }

    private int counter = 0;

    private void initUI() {
        mTxtCmmand = (EditText) findViewById(R.id.command_text);
        mBtnInput = (Button) findViewById(R.id.input_btn);
        mBtnStop = (Button) findViewById(R.id.stop_btn);
        mBtnDummy = (Button) findViewById(R.id.dummy_btn);
        mBtnDummyMap = (Button) findViewById(R.id.dummy_map_btn);
        mBtnShow = (Button) findViewById(R.id.show_btn);
        mBtnHide = (Button) findViewById(R.id.hide_btn);
        mBtnShow.setVisibility(View.GONE);
        mBtnLoacte = (Button) findViewById(R.id.locate_button);
        mBtnTracking = (Button) findViewById(R.id.tracking_button);
        mBtnLoacte.setVisibility(View.GONE);
        mBtnTracking.setVisibility(View.GONE);
        mBatteryView = (BatteryView) findViewById(R.id.battery_view);
        mBatteryData = (TextView) findViewById(R.id.battery_data);
        mAltitude = (TextView) findViewById(R.id.Altitude);
        mVerSpeed = (TextView) findViewById(R.id.VerticalSpeed);
        mDistance = (TextView) findViewById(R.id.Distance);
        mHorSpeed = (TextView) findViewById(R.id.HorizonSpeed);
//        mDistance = (TextView) findViewById(R.id.Distance);
        mRandR = (Button) findViewById(R.id.RR_Button);
        mTest = (Button) findViewById(R.id.testBtn);

        stopBtnListener();
        voiceInputListener();
        inputBtnListener();
        mapBtnListener();
        showHideBtnListener();
        locateTrackBtnListener();
        RRInputListener();
    }

    private void stopBtnListener() {
        mBtnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                mCI.mDestroy();
                mCI.mStop();
            }
        });
    }

    private void locateTrackBtnListener() {
        mBtnLoacte.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMapLocate_flag) {
                    mMapLocate_flag = false;
                    mBtnLoacte.setBackgroundResource(R.drawable.locateuser);
                } else {
                    mMapLocate_flag = true;
                    mBtnLoacte.setBackgroundResource(R.drawable.locatedrone);
                }
                updateMapCamera();
            }
        });

        mBtnTracking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMapTracking_flag) {
                    mMapTracking_flag = false;
                    mBtnTracking.setBackgroundResource(R.drawable.pause);
                } else {
                    mMapTracking_flag = true;
                    mBtnTracking.setBackgroundResource(R.drawable.refresh);
                }
            }
        });
    }

    private void showHideBtnListener() {
        mBtnShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBtnDummyMap_flag) {
                    mMapView.setVisibility(View.VISIBLE);
                } else {
                    fpvTexture.setVisibility(View.VISIBLE);
                }
                mBtnShow.setVisibility(View.GONE);
                mBtnHide.setVisibility(View.VISIBLE);
                mBtnDummyMap.setVisibility(View.VISIBLE);
            }
        });

        mBtnHide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBtnDummyMap_flag) {
                    mMapView.setVisibility(View.GONE);
                } else {
                    fpvTexture.setVisibility(View.GONE);
                }
                mBtnShow.setVisibility(View.VISIBLE);
                mBtnHide.setVisibility(View.GONE);
                mBtnDummyMap.setVisibility(View.GONE);
            }
        });
    }

    private void mapBtnListener() {
        mBtnDummyMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View mMapView = findViewById(R.id.mapFragment);
                ViewGroup.LayoutParams mapParams = mMapView.getLayoutParams();
                ViewGroup.LayoutParams fpvParams = fpvTexture.getLayoutParams();
                if (mBtnDummyMap_flag) {
                    mapParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    mapParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    mMapView.setLayoutParams(mapParams);
                    fpvParams.height = dpToPix(108);
                    fpvParams.width = dpToPix(192);
                    sendViewToBack(mMapView);
                    fpvTexture.bringToFront();
                    mMap.getUiSettings().setAllGesturesEnabled(true);
                    mBtnLoacte.setVisibility(View.VISIBLE);
                    mBtnTracking.setVisibility(View.VISIBLE);
                    mBtnDummyMap_flag = false;
                } else {
                    mapParams.height = dpToPix(108);
                    mapParams.width = dpToPix(192);
                    mMapView.setLayoutParams(mapParams);
                    fpvParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    fpvParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    sendViewToBack(fpvTexture);
                    mMapView.bringToFront();
                    mMap.getUiSettings().setAllGesturesEnabled(false);
                    mBtnLoacte.setVisibility(View.GONE);
                    mBtnTracking.setVisibility(View.GONE);
                    if (!mMapLocate_flag) {
                        mBtnLoacte.performClick();
                    }
                    if (!mMapTracking_flag) {
                        mBtnTracking.performClick();
                    }
                    mBtnDummyMap_flag = true;
                }
            }
        });
    }

    private int dpToPix(int dps) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (dps * scale + 0.5f);
    }

    private static void sendViewToBack(final View child) {
        final ViewGroup parent = (ViewGroup) child.getParent();
        if (null != parent) {
            parent.removeView(child);
            parent.addView(child, 0);
        }
    }

    private void voiceInputListener() {
        mBtnDummy.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Reset string command_text input to null
                        mStrIntention = null;
                        // Change button back ground color
                        mTxtCmmand.setBackgroundResource(R.drawable.common_google_signin_btn_text_dark_focused);
                        // Init MicrophoneInputStream and start watson speec-to-text websocket
                        capture = new MicrophoneInputStream(true);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    speechService.recognizeUsingWebSocket(capture, getRecognizeOptions(), new MicrophoneRecognizeDelegate());
                                } catch (Exception e) {
                                    showError(e);
                                }
                            }
                        }).start();
                        break;
                    case MotionEvent.ACTION_UP:
                        // Change button back ground color
                        mTxtCmmand.setBackgroundResource(R.drawable.common_google_signin_btn_text_dark_normal);
                        // Close MicrophoneInputStream
                        try {
                            capture.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
    }

    private void inputBtnListener() {
        mBtnInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Reset string command_text input to null
                mStrIntention = null;

                if (mBtnInput_flag) {
                    mBtnInput.setBackgroundResource(R.drawable.keyboard);
                    mTxtCmmand.setHint("Enter Your Command");
                    mTxtCmmand.setEnabled(true);
                    mBtnDummy.setVisibility(View.GONE);
                    mBtnInput_flag = false;
//                    mTxtCmmand.addTextChangedListener(new TextWatcher() {
//                        @Override
//                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//
//                        }
//
//                        @Override
//                        public void onTextChanged(CharSequence s, int start, int before, int count) {
//
//                        }
//
//                        @Override
//                        public void afterTextChanged(Editable s) {
//
//                        }
//                    });
                } else {
                    mBtnInput.setBackgroundResource(R.drawable.mic);
                    mStrIntention = mTxtCmmand.getText().toString();
                    // Tokenize command_in_text
                    StringTokenizer st = new StringTokenizer(mStrIntention);
                    ArrayList<String> tokenedCommand = new ArrayList<>();
                    while (st.hasMoreTokens()) {
                        tokenedCommand.add(st.nextToken());
                    }
                    // Replace mavic similar words
                    tokenedCommand = findMavicSimilar(tokenedCommand);
                    // Change arraylist to string
                    mStrIntention = TextUtils.join(" ", tokenedCommand);
                    // Execute NLC
                    new ClassificationTask().execute(tokenedCommand);
                    mTxtCmmand.setText("");
                    mTxtCmmand.setHint("Hold for Voice Input");
                    mTxtCmmand.setEnabled(false);
                    mBtnDummy.setVisibility(View.VISIBLE);
                    mBtnInput_flag = true;
                }
            }
        });
    }

    private void showMicText(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // input.setText(text);
                mTxtCmmand.setHint(text);
            }
        });
    }

    private void showError(final Exception e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(FPVFullscreenActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
    }

    // IBM watson service init
    private SpeechToText initSpeechToTextService() {
        SpeechToText service = new SpeechToText();
        String username = "23c90b4b-23ee-43cc-b0e9-97f36a0c0cfc";
        String password = "X5zb8Ub0WKsH";
        service.setUsernameAndPassword(username, password);
        service.setEndPoint("https://stream.watsonplatform.net/speech-to-text/api");
        return service;
    }

    private class MicrophoneRecognizeDelegate implements RecognizeCallback {

        @Override
        public void onTranscription(SpeechResults speechResults) {
            System.out.println(speechResults);
            mStrIntention = speechResults.getResults().get(0).getAlternatives().get(0).getTranscript();
            showMicText(mStrIntention);
        }

        @Override
        public void onConnected() {

        }

        @Override
        public void onError(Exception e) {
            showError(e);
            // mTxtCmmand.setEnabled(true);
        }

        @Override
        public void onDisconnected() {
            // Tokenize command_in_text
            StringTokenizer st = new StringTokenizer(mStrIntention);
            ArrayList<String> tokenedCommand = new ArrayList<>();
            while (st.hasMoreTokens()) {
                tokenedCommand.add(st.nextToken());
            }
            // Replace mavic similar words
            tokenedCommand = findMavicSimilar(tokenedCommand);
            // Change arraylist to string
            mStrIntention = TextUtils.join(" ", tokenedCommand);
            // Display command in string format
            showMicText(mStrIntention);
            // Execute NLC
            new ClassificationTask().execute(tokenedCommand);
        }
    }

    private RecognizeOptions getRecognizeOptions() {
        return new RecognizeOptions.Builder()
                .continuous(true)
                .contentType(ContentType.OPUS.toString())
                .model("en-US_BroadbandModel")
                .interimResults(true)
                .customizationId("bf8c3a80-fba6-11e6-a1e7-a139b48a88e5")
                .inactivityTimeout(3000)
                .smartFormatting(true)
                .build();
    }

    private class ClassificationTask extends AsyncTask<ArrayList, Void, String> {
        protected String doInBackground(ArrayList... params) {
            String result = null;
            if (params[0].size() != 0) {
                // call WatsonCommandClassifier to classify into
                cc1.classify(params[0]);
                // show execution confirmation dialog fragment
                showDialog(findViewById(android.R.id.content));

                result = "Did classify";
            } else {
                result = "Not classify";
            }
            return result;

        }
    }

    private static ArrayList<String> findMavicSimilar(ArrayList<String> original) {
        ArrayList<String> mapSimilarList = new ArrayList<String>();
        mapSimilarList.add("maverick");
        mapSimilarList.add("mavericks");
        mapSimilarList.add("magic");
        mapSimilarList.add("eric");

        ArrayList<String> lowcase_original = (ArrayList<String>) original
                .clone();
        for (int i = 0; i < lowcase_original.size(); i++) {
            String tmp = lowcase_original.get(i).toLowerCase();
            lowcase_original.set(i, tmp);
        }

        int i = 0;
        int lastIndexSimilarMavic = -1;
        while (i < mapSimilarList.size() && lastIndexSimilarMavic == -1) {
            lastIndexSimilarMavic = lowcase_original.lastIndexOf(mapSimilarList
                    .get(i));
            i++;
        }
        if ((lastIndexSimilarMavic > 0)
                && lowcase_original.get(lastIndexSimilarMavic - 1)
                .equals("hey")) {
            original.set(lastIndexSimilarMavic, "Mavic");
        }
        return original;
    }

    private ArrayList<Integer> mEncodedStr;

    private void preCheck(ArrayList<Integer> encoded_string, String google_map_string) {
        // Get first and see if it is adnvacce mission
        if (encoded_string.get(0) == 107) {
            searchPlace(google_map_string);
        } else {
            callExecution(encoded_string);
        }
    }

    private void callExecution(ArrayList<Integer> encoded_string) {
        mEncodedStr = encoded_string;
        boolean success = false;
        if (mCI != null) {
            mCI.initFlightController(); //added by keao xu
        }
        if (mCI.mFlightController != null) {
//            if (mCI.mVirtualStickEnabled == false) {
//                mCI.mEnableVS();
//            }
            mCI.executeCmd(mEncodedStr);
            success = true;
        }
        if (success) {
            showFpvToast("Instruction Sent");
        } else {
            showFpvToast("Flight Control Error");
        }
    }

    private void writeRecogRecord(boolean pos, String s2tStr, String encodedStr, String classifiedStr) {
        String group = "neg";
        if (pos) {
            group = "pos";
        }
        String key = mDBRecog.child(group).push().getKey();
        mDBRecog.child(group).child(key).child("s2tStr").setValue(s2tStr);
        mDBRecog.child(group).child(key).child("classifiedStr").setValue(classifiedStr);
        mDBRecog.child(group).child(key).child("encodedStr").setValue(encodedStr);
    }

    private List<Address> addressList = null;
    private LatLng[] locList;
    private boolean addressList_flag = true;

    public void searchPlace(String locationName) {
        Geocoder mGeocoder = new Geocoder(this);
        int maxResults = 20;
        double lowerLeftLatitude = mDroneLocation.latitude - 0.05;
        double lowerLeftLongitude = mDroneLocation.longitude - 0.05;
        double upperRightLatitude = mDroneLocation.latitude + 0.05;
        double upperRightLongitude = mDroneLocation.longitude + 0.05;
//        double lowerLeftLatitude = mUserLocation.latitude - 0.05;
//        double lowerLeftLongitude = mUserLocation.longitude - 0.05;
//        double upperRightLatitude = mUserLocation.latitude + 0.05;
//        double upperRightLongitude = mUserLocation.longitude + 0.05;
        try {
            addressList = mGeocoder.getFromLocationName(locationName, maxResults, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude);
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            addressList_flag = false;
        }
        if (addressList_flag && addressList.size() != 0) {
            Bundle args = new Bundle();
            String[] places = new String[addressList.size()];
            double[] dist = new double[addressList.size()];
            LatLng[] cdArray = new LatLng[addressList.size()];
            for (int i = 0; i < addressList.size(); i++) {
                String sb = "";
                for (int k = 0; k < addressList.get(i).getMaxAddressLineIndex(); k++) {
                    sb += addressList.get(i).getAddressLine(k);
                    sb += "; ";
                }
                double lat = addressList.get(i).getLatitude();
                double lon = addressList.get(i).getLongitude();
                LatLng currentCd = new LatLng(lat, lon);
                double distance = Utils.calcDistance(mDroneLocation.latitude, mDroneLocation.longitude, lat, lon);
                sb += new DecimalFormat("####").format(distance) + "m";
                places[i] = sb;
                dist[i] = distance;
                cdArray[i] = currentCd;
                for (int j = i - 1; j >= 0; j--) {
                    if (dist[j + 1] < dist[j]) {
                        double t1 = dist[j];
                        String t2 = places[j];
                        LatLng t3 = cdArray[j];
                        dist[j] = dist[j + 1];
                        places[j] = places[j + 1];
                        cdArray[j] = cdArray[j + 1];
                        dist[j + 1] = t1;
                        places[j + 1] = t2;
                        cdArray[j + 1] = t3;
                    }
                }
            }
            locList = cdArray;
            args.putStringArray("places", places);
            mPlaceListFragment = new PlaceListFragment();
            mPlaceListFragment.setArguments(args);
            Log.e(TAG, mPlaceListFragment.getArguments().toString());
            getSupportFragmentManager().beginTransaction().add(R.id.main_layout, mPlaceListFragment).commit();
        } else {
            showFpvToast("No result available");
            Log.e(TAG, "No result available");
            addressList_flag = true;
        }
    }

    public void getPlaceCoordinates(int index) {
        getSupportFragmentManager().beginTransaction().remove(mPlaceListFragment).commit();
        double lat = locList[index].latitude;
        double lon = locList[index].longitude;
        LatLng targetLatLng = new LatLng(lat, lon);
//        showFpvToast(targetLatLng.toString());
        addressList = null;
        locList = null;

        int latInt = (int) lat;
        int latDeci = (int) ((lat - latInt) * 100000);
        int lonInt = (int) lon;
        int lonDeci = (int) ((lon - lonInt) * 100000);

        ArrayList<Integer> temp = cc1.getEncodedString();
        temp.add(latInt);
        temp.add(latDeci);
        temp.add(lonInt);
        temp.add(lonDeci);

//        showFpvToast(temp.toString());

        callExecution(temp);
    }

    //
    public void showFpvToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(FPVFullscreenActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
