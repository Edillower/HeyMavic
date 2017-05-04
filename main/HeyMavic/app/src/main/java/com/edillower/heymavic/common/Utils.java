package com.edillower.heymavic.common;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

/**
 *  Math and Display Utils
 *  @author Eddie Wang, Eric Xu
 */


public class Utils{
    public static final double ONE_METER_OFFSET = 0.00000899322;
    private static long lastClickTime;
    private static Handler mUIHandler = new Handler(Looper.getMainLooper());

    /**
     * UI Utils
     * @author Eric Xu
     */
    public static boolean isFastDoubleClick() {
        long time = System.currentTimeMillis();
        long timeD = time - lastClickTime;
        if ( 0 < timeD && timeD < 800) {
            return true;
        }
        lastClickTime = time;
        return false;
    }

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
    /**
     * END of UI Utils
     * @author Eric Xu
     */

    /**
     * Math Utils
     * @author Eddie Wang
     */
    /**
     * Calculate destination coordinate by origin coordinate, bearing and distance
     *
     * @param lati latitude of origin point
     * @param longi longitude of origin point
     * @param bearing initial bearing of the drone
     * @param distance distance from the origin point toward the given bearing
     * @return Destination Coordinate
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

    /**
     * Calculate the bearing between two geolocation
     *
     * @param initLati latitude of origin point
     * @param initLongi longitude of origin point
     * @param destLati latitude of destination point
     * @param destLongi longitude of destination point
     * @return bearing (turning direction)
     */
    public static double calcBearing(double initLati, double initLongi, double destLati, double destLongi){
        initLati=Math.toRadians(initLati);
        destLati=Math.toRadians(destLati);
        initLongi=Math.toRadians(initLongi);
        destLongi=Math.toRadians(destLongi);
        double deltaLongi = destLongi-initLongi;
        double y = Math.sin(deltaLongi)*Math.cos(destLati);
        double x = Math.cos(initLati)*Math.sin(destLati)-Math.sin(initLati)*Math.cos(destLati)*Math.cos(deltaLongi);
        double bearing = Math.toDegrees(Math.atan2(y,x));
        return bearing;
    }

    /**
     * Calculate the distance between two geolocation
     *
     * @param initLati latitude of origin point
     * @param initLongi longitude of origin point
     * @param destLati latitude of destination point
     * @param destLongi longitude of destination point
     * @return distance between origin point and destination point
     */
    public static double calcDistance(double initLati, double initLongi, double destLati, double destLongi){
        double radius = 6371000;
        initLati=Math.toRadians(initLati);
        destLati=Math.toRadians(destLati);
        initLongi=Math.toRadians(initLongi);
        destLongi=Math.toRadians(destLongi);
        double deltaLati = destLati-initLati;
        double deltaLongi = destLongi-initLongi;
        double a = Math.sin(deltaLati/2)*Math.sin(deltaLati/2)+Math.cos(initLati)*Math.cos(destLati)*Math.sin(deltaLongi/2)*Math.sin(deltaLongi/2);
        double c = 2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));
        double distance=radius*c;
        return distance;
    }
    /**
     * END of Math Utils
     * @author Eddie Wang
     */

    /**
     * @unused save for later usage
     */
//    public static boolean checkGpsCoordinate(double latitude, double longitude) {
//        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
//    }
//
//    public static double cosForDegree(double degree) {
//        return Math.cos(degree * Math.PI / 180.0f);
//    }
//
//    public static double calcLongitudeOffset(double latitude) {
//        return ONE_METER_OFFSET / cosForDegree(latitude);
//    }
//
//    public static void addLineToSB(StringBuffer sb, String name, Object value) {
//        if (sb == null) return;
//        sb.
//                append(name == null ? "" : name + ": ").
//                append(value == null ? "" : value + "").
//                append("\n");
//    }
}
