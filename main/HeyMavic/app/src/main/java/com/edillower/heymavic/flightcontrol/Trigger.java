package com.edillower.heymavic.flightcontrol;

/**
 * Trigger, supporting class of Photo Shooting Module
 * @author Melody Cai
 */

public class Trigger {

    public static Trigger instance = null;

    private static boolean flag;

    private Trigger(){
        flag = false;
    }

    public synchronized static Trigger getInstance(){
        if(instance == null){
            return new Trigger();
        }else{
            return instance;
        }
    }

    public synchronized String trig(){
        flag = ! flag;
        return String.valueOf(flag);
    }

    public boolean value(){
        return flag;
    }
}
