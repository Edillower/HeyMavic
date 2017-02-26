package com.edillower.heymavic;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
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
import android.widget.Toast;
import android.text.TextUtils;

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
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneInputStream;
import com.ibm.watson.developer_cloud.android.library.audio.utils.ContentType;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechResults;
import com.ibm.watson.developer_cloud.speech_to_text.v1.websocket.RecognizeCallback;

import java.util.ArrayList;
import java.util.StringTokenizer;

import dji.common.flightcontroller.DJIFlightControllerCurrentState;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.flightcontroller.DJIFlightControllerDelegate;
import dji.sdk.products.DJIAircraft;

/**
 * FPV main control window
 *
 * @author Eddie Wang
 */
public class FPVFullscreenActivity extends Activity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public static final String TAG = FPVFullscreenActivity.class.getName();

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
    private boolean mMapLocate_flag=true;
    private Button mBtnTracking;
    private boolean mMapTracking_flag=true;

    // IBM watson varaibles
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
    private Button mBtnDummy;
    private Button mBtnDummyMap;
    private boolean mBtnDummyMap_flag = true;
    private Button mBtnShow;
    private Button mBtnHide;

    private Context mContext;

    private CommandInterpreter mCI;

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

        mCI = new CommandInterpreter(TAG);
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
        mCI.mDestroy();
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
        boolean ret = false;
        DJIBaseProduct product = DJISimulatorApplication.getProductInstance();
        if (product != null) {
            if (product.isConnected()) {
                //The product is connected
                showFpvToast(DJISimulatorApplication.getProductInstance().getModel() + " Connected");
                ret = true;
            } else {
                if (product instanceof DJIAircraft) {
                    DJIAircraft aircraft = (DJIAircraft) product;
                    if (aircraft.getRemoteController() != null && aircraft.getRemoteController().isConnected()) {
                        // The product is not connected, but the remote controller is connected
                        showFpvToast("only RC Connected");
                        ret = true;
                    }
                }
            }
        }

        if (!ret) {
            // The product or the remote controller are not connected.
            showFpvToast("Disconnected");
        }
    }

    private void initDrone() {
        mCI.initFlightController();
        if (mCI.mFlightController != null) {
            showFpvToast("Set up call back");
            mCI.mFlightController.setUpdateSystemStateCallback(new DJIFlightControllerDelegate.FlightControllerUpdateSystemStateCallback() {
                @Override
                public void onResult(DJIFlightControllerCurrentState state) {
                    double mDroneLocationLat = state.getAircraftLocation().getLatitude();
                    double mDroneLocationLng = state.getAircraftLocation().getLongitude();
                    mDroneLocation = new LatLng(mDroneLocationLat, mDroneLocationLng);
                    mDroneHeading = (float) mCI.mFlightController.getCompass().getHeading();
                    updateDroneLocation();
                }
            });
        }
    }

    private void initUI() {
        mTxtCmmand = (EditText) findViewById(R.id.command_text);
        mBtnInput = (Button) findViewById(R.id.input_btn);
        mBtnDummy = (Button) findViewById(R.id.dummy_btn);
        mBtnDummyMap = (Button) findViewById(R.id.dummy_map_btn);
        mBtnShow = (Button) findViewById(R.id.show_btn);
        mBtnHide = (Button) findViewById(R.id.hide_btn);
        mBtnShow.setVisibility(View.GONE);
        mBtnLoacte = (Button) findViewById(R.id.locate_button);
        mBtnTracking = (Button) findViewById(R.id.tracking_button);
        mBtnLoacte.setVisibility(View.GONE);
        mBtnTracking.setVisibility(View.GONE);
        voiceInputListener();
        inputBtnListener();
        mapBtnListener();
        showHideBtnListener();
        locateTrackBtnListener();
    }

    private void locateTrackBtnListener() {
        mBtnLoacte.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMapLocate_flag){
                    mMapLocate_flag = false;
                    mBtnLoacte.setBackgroundResource(R.drawable.locateuser);
                }else{
                    mMapLocate_flag = true;
                    mBtnLoacte.setBackgroundResource(R.drawable.locatedrone);
                }
                updateMapCamera();
            }
        });

        mBtnTracking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMapTracking_flag){
                    mMapTracking_flag = false;
                    mBtnTracking.setBackgroundResource(R.drawable.stop);
                }else{
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
                    mMap.getUiSettings().setAllGesturesEnabled(false);
                    mBtnLoacte.setVisibility(View.GONE);
                    mBtnTracking.setVisibility(View.GONE);
                    if (!mMapLocate_flag){
                        mBtnLoacte.performClick();
                    }
                    if (!mMapTracking_flag){
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
                    mStrIntention=mTxtCmmand.getText().toString();
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
                    mBtnInput_flag=true;
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

    private class ClassificationTask extends AsyncTask<ArrayList , Void, String> {
        protected String doInBackground(ArrayList ... params) {
            String result = null;
            if (params[0].size()!=0){
                // show result
                ArrayList<Integer> encoded_string = cc1.classify(params[0]);
                showFpvToast(encoded_string.toString());
                callExecution(encoded_string);
                result = "Did classify";
            }else{
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


    private void callExecution(ArrayList<Integer> encoded_string) {
        mCI.executeCmd(encoded_string);
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
