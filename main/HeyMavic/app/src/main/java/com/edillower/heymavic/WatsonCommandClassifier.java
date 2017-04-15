package com.edillower.heymavic;

import com.ibm.watson.developer_cloud.natural_language_classifier.v1.NaturalLanguageClassifier;

import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by David on 2/26/2017.
 */

public class WatsonCommandClassifier {
    private final String command_classfier_id = "90e7acx197-nlc-35102";
    private final String direction_classfier_id = "90e7b4x199-nlc-18482";
    private final String object_detect_classfier_id = "90e7acx197-nlc-37646";
    private final List<String> object_list = Arrays.asList("aeroplane", "bicycle", "bird", "boat",
            "bottle", "bus", "car", "cat", "chair",
            "cow", "diningtable", "dog", "horse",
            "motorbike", "person", "pottedplant",
            "sheep", "sofa", "train", "tvmonitor");
    private NaturalLanguageClassifier nlpService;
    private String command_direction;

    private ArrayList<Integer> encoded_string;
    private String google_map_search_string;

    public WatsonCommandClassifier(){
        nlpService = new NaturalLanguageClassifier();
        String username = "892a7e25-f38a-4d04-a725-028871966429";
        String password = "1rFfpEEdA2k3";
        nlpService.setUsernameAndPassword(username, password);
        nlpService.setEndPoint("https://gateway.watsonplatform.net/natural-language-classifier/api");
        google_map_search_string = null;
    }

    public ArrayList<Integer> classify(ArrayList<String> tokenedCommand){
        String command = "stop";
        String direction = null;
        String unit = null;
        String map_search_string = null;
        String object_detect_class_string = null;

        String commandInText = TextUtils.join(" ", tokenedCommand);
        if (tokenedCommand != null) {
            ExecutorService executor = Executors.newCachedThreadPool();
            Callable<String> task0 = new ExactProcessCallableService(tokenedCommand);
            Callable<String> task1 = new NLPCallableService(nlpService, command_classfier_id, commandInText);
            Callable<String> task2 = new NLPCallableService(nlpService, direction_classfier_id, commandInText);
            Callable<String> task3 = new NLUCallableService(commandInText);
            Callable<String> task4 = new NLPCallableService(nlpService, object_detect_classfier_id, commandInText);

            Future<String> future0 = executor.submit(task0);
            Future<String> future1 = executor.submit(task1);
            Future<String> future2 = executor.submit(task2);
            Future<String> future3 = executor.submit(task3);
            Future<String> future4 = executor.submit(task4);
            executor.shutdown();

            try {
                unit = future0.get();
                command = future1.get();
                direction = future2.get();
                map_search_string = future3.get();
                object_detect_class_string = future4.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

            if (map_search_string.equals("")){
                this.google_map_search_string = commandInText;
            }else{
                this.google_map_search_string = map_search_string;
            }
        }

        // parse into decimal encoded string
        ArrayList<Integer> result = encode_string(command, direction, unit, object_detect_class_string);
        int switch_num = result.remove(result.size()-1);

        // set encoded_string
        switch (switch_num) {
            case 0:
                this.command_direction = command;
                break;
            case 1:
            case 2:
                this.command_direction = command + ' ' + direction;
                break;
            case 3:
            case 4:
                this.command_direction = command + ' ' + direction + ' ' + unit;
                break;
            case 5:
                this.command_direction = "Advance Mission: " + this.google_map_search_string;
                break;
            case 7:
                this.command_direction = "Take photo: "+object_detect_class_string;
                break;
            default:
                this.command_direction = "Unrecognize command in WatsonCommandClassifier";
                break;
        }
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

    public String getGoogleMapSearchString(){
        return this.google_map_search_string;
    }

    private ArrayList<Integer> encode_string (String command, String direction, String unit, String object_detect_class_string){
        ArrayList<Integer> encoded_string = new ArrayList<Integer>();
        int switch_num = 0; // 0 for null, 1 for move, 2 for turn, 3 for move unit, 4 for turn unit, 5 for advance mission, 6 for setting, 7 for image recong
        switch (command) {
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
                switch (direction) {
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
                switch (direction) {
                    case "left":
                        encoded_string.add(303);
                        break;
                    case "right":
                        encoded_string.add(304);
                        break;
                    case "forward":
                        encoded_string.add(303);
                        encoded_string.add(204);
                        encoded_string.add(180);
                        break;
                    case "backward":
                        encoded_string.add(303);
                        encoded_string.add(204);
                        encoded_string.add(180);
                        break;
                    default:
                }
                break;
            case "location":
                encoded_string.add(107);
                encoded_string.add(205);
                switch_num = 5;
                break;
            case "photo":
                encoded_string.add(109);
                int id = this.object_list.indexOf(object_detect_class_string)+1;
                encoded_string.add(id);
                switch_num = 7;
                break;
            default:
                // code to be executed if all cases are not matched;
        }
        if (unit != null) {
            //move
            if (switch_num == 1) {
                encoded_string.add(202);
                switch_num = 3;
            }
            //turn
            else if (switch_num == 2) {
                encoded_string.add(204);
                switch_num = 4;
            }
            encoded_string.add(Integer.parseInt(unit));
        }

        encoded_string.add(switch_num);
        return encoded_string;
    }

}

