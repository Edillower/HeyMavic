package com.edillower.heymavic.common;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import dji.common.error.DJIError;

/**
 * Created by Eric on 3/5/17.
 *
 * This class has Utils for scree display and math problem
 */
public class Utils{
    public static final double ONE_METER_OFFSET = 0.00000899322;
    private static long lastClickTime;
    private static Handler mUIHandler = new Handler(Looper.getMainLooper());
    public static boolean isFastDoubleClick() {
        long time = System.currentTimeMillis();
        long timeD = time - lastClickTime;
        if ( 0 < timeD && timeD < 800) {
            return true;
        }
        lastClickTime = time;
        return false;
    }

//    public static void showDialogBasedOnError(Context ctx, DJIError djiError) {
//        if (null == djiError)
//            DJIDialog.showDialog(ctx, R.string.success);
//        else
//            DJIDialog.showDialog(ctx, djiError.getDescription());
//    }

    public static void setResultToToast(final Context context, final String string) {
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void setResultToText(final Context context, final TextView tv, final String s) {
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                if (tv == null) {
                    Toast.makeText(context, "tv is null", Toast.LENGTH_SHORT).show();
                } else {
                    tv.setText(s);
                }
            }
        });
    }

    public static boolean checkGpsCoordinate(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    public static double Radian(double x){
        return  x * Math.PI / 180.0;
    }

    public static double Degree(double x){
        return  x * 180 / Math.PI ;
    }

    public static double cosForDegree(double degree) {
        return Math.cos(degree * Math.PI / 180.0f);
    }

    public static double calcLongitudeOffset(double latitude) {
        return ONE_METER_OFFSET / cosForDegree(latitude);
    }

    public static void addLineToSB(StringBuffer sb, String name, Object value) {
        if (sb == null) return;
        sb.
                append(name == null ? "" : name + ": ").
                append(value == null ? "" : value + "").
                append("\n");
    }

    /**
     * added by Eric on april 4
     *
     * This is completed ! pass test!
     *
     * @param lati
     * @param longi
     * @param bearing [-180, 180]
     * @param distance
     * @return
     */
    public static double[] calcDestination(double lati, double longi,
                                           double bearing, double distance) {
        double[] destination = new double[2]; // double[0]=latitude double[1]=longitude

        // Setup parameters
        double radius = 6371000; // Earth radius in meters
        double ber = bearing; // Heading direction, clockwise from north
        if (bearing < 0) {
            ber += 360;
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
}
