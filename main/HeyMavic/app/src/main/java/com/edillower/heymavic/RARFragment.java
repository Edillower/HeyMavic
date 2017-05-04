package com.edillower.heymavic;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneInputStream;
import com.ibm.watson.developer_cloud.android.library.audio.utils.ContentType;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechResults;
import com.ibm.watson.developer_cloud.speech_to_text.v1.websocket.RecognizeCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;

import fr.arnaudguyon.xmltojsonlib.XmlToJson;

/**
 * class for the display of Retrieve and Rank service (interactive user manual)
 * @author Paul Yang, Melody Cai, Eddie Wang
 */
public class RARFragment extends Fragment {
    private static final String USERNAME = "3a92a1a2-6d59-4ae0-9a38-cc2dc0785d80";
    private static final String PASSWORD = "Je8iOi18gSGY";
    private static final String SOLR_CLUSTER_ID = "sc1afc2fba_ebc2_4630_a1df_d0ec76078c26";
    private static final String RANKDER_ID = "1eec7cx29-rank-4228";
    private static final String COLLECTION_NAME = "V2";
    private static final String ERROR = "No answer available.";

    private Button micro;
    private Button voice;
    private EditText input;
    private TextView output;

    private String mStrIntention;
    private MicrophoneInputStream capture;
    private SpeechToText speechService;

    private boolean mBtnInput_flag = true;

    private String TAG = RARFragment.class.getName();
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View res = inflater.inflate(R.layout.fragment_retrive_and_rank, container, false);
        micro = (Button) res.findViewById(R.id.input_btn2);
        voice = (Button) res.findViewById(R.id.dummy_btn2);
        input = (EditText) res.findViewById(R.id.command_text2);

        output = (TextView) res.findViewById(R.id.RRView);
        output.setMovementMethod(new ScrollingMovementMethod());

        speechService = initSpeechToTextService();

        inputBtnListener();
        voiceInputListener();

        return res;


    }


    private class RetrieveAndRankTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        @Override
        protected String doInBackground(String... params) {
            String query = params[0];

            String json_response = "";
            HttpURLConnection connection;
            try{
                // Set up post connection
                query = query.replace(" ", "%20");
                String url_str = "https://gateway.watsonplatform.net/retrieve-and-rank/api/v1/solr_clusters/"
                        + SOLR_CLUSTER_ID + "/solr/" + COLLECTION_NAME + "/fcselect?ranker_id=" + RANKDER_ID + "&q=" + query;
                System.out.println(url_str);
                URL url = new URL(url_str);
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");

                // Login credentials
                String creds = USERNAME + ":" + PASSWORD;
                String header = "Basic " + new String(android.util.Base64.encode(creds.getBytes(), android.util.Base64.NO_WRAP));
                connection.addRequestProperty("Authorization", header);

                // Connect
                connection.connect();

                // Get response if success
                if(connection.getResponseCode()==201 || connection.getResponseCode()==200){
                    InputStreamReader in = new InputStreamReader(connection.getInputStream());
                    BufferedReader br = new BufferedReader(in);
                    String text = "";
                    while ((text = br.readLine()) != null) {
                        json_response += text;
                    }
                }
            }catch (MalformedURLException e) {
                e.printStackTrace();
            }catch (IOException e) {
                e.printStackTrace();
            }catch (Exception e) {
                e.printStackTrace();
            }

            XmlToJson xmlToJson = new XmlToJson.Builder(json_response).build();
            JSONObject jsonObject = xmlToJson.toJson();
            String result_str = "";
            try {
                JSONObject response = jsonObject.getJSONObject("response");
                JSONObject result = response.getJSONObject("result");
                JSONArray docs = result.getJSONArray("doc");
                System.out.println(docs.toString(4));

                for (int i = 0; i < docs.length() && i < 3; i++) {
                    JSONObject doc = docs.getJSONObject(i);
                    // Get score
                    JSONObject dou = doc.getJSONObject("double");
                    // Get Answer
                    JSONObject arr = doc.getJSONObject("arr");
                    result_str += "Answer No. " + (i + 1) + " Score: "
                                + String.format("%.5f",dou.getDouble("content")) + "\n";
                    result_str += arr.getJSONArray("str").getJSONObject(1).getString("content") + "\n\n";
                }
                System.out.println(result_str);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return result_str;
        }


        @Override
        protected void onPostExecute(String strings) {
            super.onPostExecute(strings);
            if(strings.length() == 0){
                Toast.makeText(getActivity(), ERROR, Toast.LENGTH_SHORT).show();
            }
            output.setText(strings);
        }

    }

    private void inputBtnListener() {
        micro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Reset string command_text input to null
                mStrIntention = null;

                if (mBtnInput_flag) {
                    micro.setBackgroundResource(R.drawable.keyboard);
                    input.setHint("Enter Your Command");
                    input.setEnabled(true);
                    voice.setVisibility(View.GONE);
                    mBtnInput_flag = false;
                } else {
                    micro.setBackgroundResource(R.drawable.mic);
                    mStrIntention = input.getText().toString();
                    // R and R
                    new RetrieveAndRankTask().execute(mStrIntention);


                    input.setText("");
                    input.setHint("Hold for Voice Input");
                    input.setEnabled(false);
                    voice.setVisibility(View.VISIBLE);
                    mBtnInput_flag = true;
                }
            }
        });
    }

    private void voiceInputListener() {
        voice.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Reset string command_text input to null
                        mStrIntention = null;
                        // Change button back ground color
                        input.setBackgroundResource(R.drawable.common_google_signin_btn_text_dark_focused);
                        // Init MicrophoneInputStream and start watson speec-to-text websocket
                        capture = new MicrophoneInputStream(true);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    speechService.recognizeUsingWebSocket(capture, getRecognizeOptions(), new RARFragment.MicrophoneRecognizeDelegate());
                                } catch (Exception e) {
                                    Log.e(TAG, e.toString());
                                }
                            }
                        }).start();
                        break;
                    case MotionEvent.ACTION_UP:
                        // Change button back ground color
                        input.setBackgroundResource(R.drawable.common_google_signin_btn_text_dark_normal);
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

            // TODO
            showMicText(mStrIntention);
        }

        @Override
        public void onConnected() {

        }

        @Override
        public void onError(Exception e) {
            Log.e(TAG,e.toString());
            // mTxtCmmand.setEnabled(true);
        }

        @Override
        public void onDisconnected() {

            // Display command in string format
            // TODO
            showMicText(mStrIntention);
            new RetrieveAndRankTask().execute(mStrIntention);

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

    private void showMicText(final String text) {

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // input.setText(text);
                input.setHint(text);
            }
        });

    }


    }
