package com.edillower.heymavic;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Service;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextWatcher;
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

import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneInputStream;
import com.ibm.watson.developer_cloud.android.library.audio.utils.ContentType;
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.NaturalLanguageClassifier;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechResults;
import com.ibm.watson.developer_cloud.speech_to_text.v1.websocket.RecognizeCallback;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * FPV main control window
 * @author Eddie Wang
 */
public class FPVFullscreenActivity extends Activity{
    // IBM watson varaibles
    private final String command_classfier_id = "f5b42fx173-nlc-2075";
    private final String direction_classfier_id = "f5b42fx173-nlc-2077";
    private SpeechToText speechService;
    private NaturalLanguageClassifier nlpService;

    // App button and views
    private TextureView fpvTexture;
    private Button mBtnInput;
    private boolean mBtnInput_flag=true;
    private EditText mTxtCmmand;
    private Button mBtmDummy;
    private MicrophoneInputStream capture;

    private String mStrIntention="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);

        fpvTexture=new TextureView(this);
        fpvTexture.setSurfaceTextureListener(new BaseFpvView(this));
        setContentView(fpvTexture);

        LayoutInflater layoutInflater = getLayoutInflater();
        View content = layoutInflater.inflate(R.layout.activity_fpvfullscreen, null, false);
        RelativeLayout.LayoutParams rlParam = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        addContentView(content,rlParam);
        initUI();

        speechService = initSpeechToTextService();
        nlpService = initNaturalLanguageClassifierService();

    }

    private void initUI(){
        mTxtCmmand=(EditText) findViewById(R.id.command_text);
        mBtnInput=(Button) findViewById(R.id.input_btn);
        mBtmDummy=(Button) findViewById(R.id.dummy_btn);
        voiceInputListener();
        inputBtnListener();
    }

    private void voiceInputListener(){
        mBtmDummy.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mTxtCmmand.setBackgroundResource(R.drawable.common_google_signin_btn_text_dark_pressed);
                        capture = new MicrophoneInputStream(true);
                        new Thread(new Runnable() {
                            @Override public void run() {
                                try {
                                    speechService.recognizeUsingWebSocket(capture, getRecognizeOptions(), new MicrophoneRecognizeDelegate());
                                } catch (Exception e) {
                                    showError(e);
                                }
                            }
                        }).start();
                        break;
                    case MotionEvent.ACTION_UP:
                        mTxtCmmand.setBackgroundResource(R.drawable.common_google_signin_btn_text_dark_normal);
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

    private void inputBtnListener(){
        mBtnInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBtnInput_flag){
                    mBtnInput.setBackgroundResource(R.drawable.keyboard);
                    mTxtCmmand.setHint("Enter Your Command");
                    mTxtCmmand.setEnabled(true);
                    mBtmDummy.setVisibility(View.GONE);
                    mBtnInput_flag=false;
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
                }else{
                    mBtnInput.setBackgroundResource(R.drawable.mic);
                    mStrIntention=mTxtCmmand.getText().toString();
                    new ClassificationTask().execute(mStrIntention);
                    mTxtCmmand.setText("");
                    mTxtCmmand.setHint("Hold for Voice Input");
                    mTxtCmmand.setEnabled(false);
                    mBtmDummy.setVisibility(View.VISIBLE);
                    mBtnInput_flag=true;
                }
            }
        });
    }

    private void showMicText(final String text) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                // input.setText(text);
                mTxtCmmand.setHint(text);
            }
        });
    }

    private void showError(final Exception e) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
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
    private NaturalLanguageClassifier initNaturalLanguageClassifierService() {
        NaturalLanguageClassifier service = new NaturalLanguageClassifier();
        String username = "892a7e25-f38a-4d04-a725-028871966429";
        String password = "1rFfpEEdA2k3";
        service.setUsernameAndPassword(username, password);
        service.setEndPoint("https://gateway.watsonplatform.net/natural-language-classifier/api");
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
            // Add classification task here
            new ClassificationTask().execute(mStrIntention);
        }
    }
    private RecognizeOptions getRecognizeOptions() {
        return new RecognizeOptions.Builder()
                .continuous(true)
                .contentType(ContentType.OPUS.toString())
                .model("en-US_BroadbandModel")
                .interimResults(true)
                .inactivityTimeout(2000)
                .smartFormatting(true)
                .build();
    }
    private class ClassificationTask extends AsyncTask<String, Void, String> {
        // Parse classified command into decimal encoded string
        private ArrayList<Integer> encode_string (String command, String direction, String unit){
            ArrayList<Integer> encoded_string = new ArrayList<Integer>();
            int switch_num = 0; // 0 for null, 1 for move, 2 for turn
            switch(command){
                case "takeoff":
                    encoded_string.add(100);
                    break;
                case "landing":
                    encoded_string.add(101);
                    break;
                case "stop":
                    encoded_string.add(102);
                    break;
                case "move":
                    encoded_string.add(103);
                    switch_num = 1;
                    switch(direction){
                        case "left":
                            encoded_string.add(201);
                            encoded_string.add(303);
                            break;
                        case "right":
                            encoded_string.add(201);
                            encoded_string.add(304);
                            break;
                        case "forward":
                            encoded_string.add(201);
                            encoded_string.add(301);
                            break;
                        case "backward":
                            encoded_string.add(201);
                            encoded_string.add(302);
                            break;
                        case "up":
                            encoded_string.add(105);
                            break;
                        case "down":
                            encoded_string.add(106);
                            break;
                        default:

                    }
                    break;
                case "turn":
                    encoded_string.add(104);
                    encoded_string.add(203);
                    switch_num = 2;
                    switch(direction) {
                        case "left":
                            encoded_string.add(303);
                            break;
                        case "right":
                            encoded_string.add(304);
                            break;
                        case "forward":
                            encoded_string.add(202);
                            encoded_string.add(180);
                            break;
                        case "backward":
                            encoded_string.add(202);
                            encoded_string.add(180);
                            break;
                        default:
                    }
                    break;
                default:
                    // code to be executed if all cases are not matched;
            }
            if (unit != null){
                //move
                if (switch_num == 1){
                    encoded_string.add(202);
                }
                //turn
                else if (switch_num == 2){
                    encoded_string.add(204);
                }
                encoded_string.add(Integer.parseInt(unit));
            }
            return encoded_string;
        }

        @Override protected String doInBackground(String... params) {
            // Classify command and direction
//            String command = nlpService.classify(command_classfier_id,params[0]).execute().getTopClass();
//            String direction = nlpService.classify(direction_classfier_id,params[0]).execute().getTopClass();

            String command = "stop";
            String direction = null;
            String unit = null;

            ExecutorService executor = Executors.newCachedThreadPool();
            Callable<String> task0 = new ExactProcessCallableService(params[0]);
            Callable<String> task1 = new NLPCallableService(nlpService, command_classfier_id, params[0]);
            Callable<String> task2 = new NLPCallableService(nlpService, direction_classfier_id, params[0]);

            Future<String> future0 = executor.submit(task0);
            Future<String> future1 = executor.submit(task1);
            Future<String> future2 = executor.submit(task2);
            executor.shutdown();

            try {
                unit = future0.get();
                command = future1.get();
                direction = future2.get();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

            // parse into decimal encoded string
            ArrayList<Integer> encoded_string = encode_string(command, direction, unit);

            // show result
            //showResponse(command + ' ' + direction);
            //showEncode(encoded_string.toString());
            System.out.println(command + ' ' + direction + ' ' + unit);
            System.out.println(encoded_string.toString());
            showFpvToast(encoded_string.toString());
            return "Did classify";
        }
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
