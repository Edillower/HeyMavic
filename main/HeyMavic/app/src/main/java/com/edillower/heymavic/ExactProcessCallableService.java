package com.edillower.heymavic;

/**
 * Created by David on 2/9/2017.
 */
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;

public class ExactProcessCallableService implements Callable<String>{
    private String command_text;

    ExactProcessCallableService(String command_text) {
        this.command_text = command_text;
    }

    public String call() throws Exception {
        String result=null;
        StringTokenizer st = new StringTokenizer(this.command_text);
        ArrayList<String> ars = new ArrayList<>();
        while (st.hasMoreTokens()) {
            ars.add(st.nextToken());
        }
        for (int i = 0; i < ars.size(); i++) {
            if (isNumeric(ars.get(i))) {
                result = ars.get(i);
                break;
            }
        }
        return result;
    }

    private static boolean isNumeric(String s) {
        return s.matches("[-+]?\\d*\\.?\\d+");
    }

    private static int changeUnit(){
        return 0; //TODO
    }
}
