package com.edillower.heymavic;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.concurrent.Callable;


/**
 * Created by paulyang on 3/23/17.
 */

public class NLUCallableService implements Callable<String> {
    private static final String USERNAME = "78c17032-a44b-4bd7-b905-4a00bbf0b352";
    private static final String PASSWORD = "eZVEhsdWlucG";
    private String sentence;

    NLUCallableService(String sentence) {
        this.sentence = sentence;
    }

    public String call() throws Exception {
        String json_response = "";
        HttpURLConnection connection;
        try{
            // Set up post connection
            sentence = sentence.replace(" ", "%20");
            String url_str = "https://gateway.watsonplatform.net/natural-language-understanding/api/v1/analyze?version=2017-02-27&text="
                    + sentence + "&features=entities";
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
                StringBuilder text = new StringBuilder();
                while ((json_response = br.readLine()) != null) {
                    text.append(json_response);
                }
                json_response = text.toString();
            }else{
                System.out.printf("ERROR retrieving response from IBM service, code %d\n" + connection.getResponseCode());
            }
        }catch (MalformedURLException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }

        String result_str = "";
        HashSet<String> types = new HashSet<>();
        types.add("Organization");
        types.add("Facility");
        types.add("Location");
        types.add("Company");

        try {
            JSONObject response = new JSONObject(json_response);
            JSONArray entities = response.getJSONArray("entities");
            for (int i = 0; i < entities.length(); i++) {
                JSONObject entity = entities.getJSONObject(i);
                if (types.contains(entity.getString("type"))) {
                    result_str += entity.getString("text") + " ";
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        /*
        if (json_response.length() == 0) {
            result_str = sentence.substring(sentence.indexOf(" to ") + 4);
        }
        */
        return result_str;
    }
}