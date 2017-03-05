package com.edillower.heymavic;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.ibm.watson.developer_cloud.natural_language_classifier.v1.NaturalLanguageClassifier;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by David on 2/26/2017.
 */

public class WatsonCommandClassifier {
    private final String command_classfier_id = "715f86x192-nlc-300";
    private final String direction_classfier_id = "716034x193-nlc-295";
    private NaturalLanguageClassifier nlpService;
    private String command_direction;
    private ArrayList<Integer> encoded_string;

    public WatsonCommandClassifier(){
        nlpService = new NaturalLanguageClassifier();
        String username = "892a7e25-f38a-4d04-a725-028871966429";
        String password = "1rFfpEEdA2k3";
        nlpService.setUsernameAndPassword(username, password);
        nlpService.setEndPoint("https://gateway.watsonplatform.net/natural-language-classifier/api");
    }

    public ArrayList<Integer> classify(ArrayList<String> tokenedCommand){
        String command = "stop";
        String direction = null;
        String unit = null;

        if (tokenedCommand != null) {
            ExecutorService executor = Executors.newCachedThreadPool();
            Callable<String> task0 = new ExactProcessCallableService(tokenedCommand);
            Callable<String> task1 = new NLPCallableService(nlpService, command_classfier_id, TextUtils.join(" ", tokenedCommand));
            Callable<String> task2 = new NLPCallableService(nlpService, direction_classfier_id, TextUtils.join(" ", tokenedCommand));

            Future<String> future0 = executor.submit(task0);
            Future<String> future1 = executor.submit(task1);
            Future<String> future2 = executor.submit(task2);
            executor.shutdown();

            try {
                unit = future0.get();
                command = future1.get();
                direction = future2.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        // parse into decimal encoded string
        ArrayList<Integer> result = encode_string(command, direction, unit);

        // set encoded_string
        this.command_direction = command + ' ' + direction;
        this.encoded_string = result;

        // show result TODO final comment out
        System.out.println(command + ' ' + direction);
        System.out.println(result.toString());

        return result;
    }

    public String getCommand(){
        return this.command_direction;
    }

    public ArrayList<Integer> getEncodedString(){
        return this.encoded_string;
    }

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
                switch_num = 1;
                switch(direction){
                    case "left":
                        encoded_string.add(103);
                        encoded_string.add(201);
                        encoded_string.add(303);
                        break;
                    case "right":
                        encoded_string.add(103);
                        encoded_string.add(201);
                        encoded_string.add(304);
                        break;
                    case "forward":
                        encoded_string.add(103);
                        encoded_string.add(201);
                        encoded_string.add(301);
                        break;
                    case "backward":
                        encoded_string.add(103);
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

}
