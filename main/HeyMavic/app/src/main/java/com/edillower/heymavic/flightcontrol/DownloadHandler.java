package com.edillower.heymavic.flightcontrol;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.edillower.heymavic.common.Utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import dji.common.error.DJIError;
import dji.sdk.camera.MediaManager.DownloadListener;

import static dji.sdk.camera.view.FpvLiveView.TAG;


public class DownloadHandler<B> implements DownloadListener<B> {

    String imgDecodableString;
    private Context mContext;
    float[] focusCoordinates;


    CommandInterpreter mCI;


    public DownloadHandler(Context context, String fpn, CommandInterpreter ci){
        mContext = context;
        imgDecodableString = fpn;
        focusCoordinates = new float[2];
        mCI = CommandInterpreter.getUniqueInstance(context);

    }

    @Override
    public void onStart() {

    }

    @Override
    public void onRateUpdate(long total, long current, long arg2) {

    }

    @Override
    public void onProgress(long total, long current) {

    }

    public float[] getCoordinates(){
        return Arrays.copyOf(this.focusCoordinates, this.focusCoordinates.length);
    }

    @Override
    public void onSuccess(B obj) {
//        Utils.setResultToToast(mContext, imgDecodableString);
        if (obj instanceof Bitmap) {
            Bitmap bitmap = (Bitmap) obj;
            // TODO add more class
            // Get query ID
            String query_id = "15";
            // Send image to serve
            try {
//                Utils.setResultToToast(mContext, "Start commute server");
                float [] bbox = new ObjectDetectTask().execute("129.114.109.171", "10097", query_id).get();
                focusCoordinates[0] = (bbox[0] + bbox[2])/2;
                focusCoordinates[1] = (bbox[1] + bbox[3])/2;
//                Utils.setResultToToast(mContext, Arrays.toString(bbox));
                Log.e(TAG, Arrays.toString(bbox));
            }catch (Exception e) {
                Utils.setResultToToast(mContext, "Error in ObjectDetectTask"); //TODO
                Log.e(TAG, "Error in ObjectDetectTask");
            }
            Log.e(TAG, "Success! The bitmap's byte count is: " + bitmap.getByteCount());
        } else if (obj instanceof String) {
            imgDecodableString = obj.toString();
//            Utils.setResultToToast(mContext, "The file has been store, its path is " + imgDecodableString); //TODO
            // TODO add more class
            // Get query ID
            String query_id = "15";
            // Send image to server
            try {
//                Utils.setResultToToast(mContext, "Start commu server"); //TODO
                float [] bbox = new ObjectDetectTask().execute("129.114.109.171", "10097", query_id).get();
                if (bbox == null) {
                    // Object not detected

                }
                else {
                    // Object did Detected

                }
                focusCoordinates[0] = (bbox[0] + bbox[2])/2;
                focusCoordinates[1] = (bbox[1] + bbox[3])/2;


                Log.e(TAG, Arrays.toString(bbox));
//                Utils.setResultToToast(mContext, Arrays.toString(focusCoordinates));
            }catch (Exception e) {
                Utils.setResultToToast(mContext, "Error in ObjectDetectTask"); //TODO
            }
            try{
                    mCI.focusLen(focusCoordinates);
            }catch (Exception e) {
                Utils.setResultToToast(mContext, e.getMessage()); //TODO
            }
            Log.e(TAG, "The file has been store, its path is " + obj.toString());
        }
    }

    @Override
    public void onFailure(DJIError djiError) {
        Utils.setResultToToast(mContext, "download:" + djiError.toString());
    }

    private static float[] stringToFloatArray(String arr){
        String[] items = arr.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\s", "").split(",");
        float[] results = null;
        if (items.length > 4){
            results = new float[items.length];
            for (int i = 0; i < items.length; i++) {
                try {
                    results[i] = Float.parseFloat(items[i]);
                } catch (NumberFormatException nfe) {
                    nfe.printStackTrace();
                }
            }
        }
        return results;
    }



    // TODO: take this out of this code for documentation purpose
    class ObjectDetectTask extends AsyncTask<String, Void, float[]> {

        protected float[] doInBackground(String... param) {
            String bindAddr = param[0];
            int port = Integer.parseInt(param[1]);
            int query_id = Integer.parseInt(param[2]);
            float[] result=null;

            try {
                // Connect to server
                Socket socket=new Socket(InetAddress.getByName(bindAddr),port);
                String cms_return = null;
                String cmr_return = null;

                // Multithread on reciever and sender
                ExecutorService executor = Executors.newCachedThreadPool();
                Callable<String> task_cms = new ClientMessageSender(socket, imgDecodableString, query_id);
                Callable<String> task_cmr = new ClientMessageReceiver(socket);
                Future<String> future0 = executor.submit(task_cms);
                Future<String> future1 = executor.submit(task_cmr);
                executor.shutdown();

                // Get returned value
                try {
                    cms_return = future0.get();
                    cmr_return = future1.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

                // Close socket
                socket.close();

                // Verify output
                //System.out.println(cms_return);
                //System.out.println(cmr_return);
                result = stringToFloatArray(cmr_return);
                //System.out.println(result);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        protected void onPostExecute(Double[] feed) {
//            if (feed != null){
//                // Get img into bitmap
//                Bitmap myBitmap = BitmapFactory.decodeFile(imgDecodableString);
//                // Change relative percentages to coordinates
//                int width = myBitmap.getWidth();
//                int height = myBitmap.getHeight();
//                int left = (int)(feed[0]*width);
//                int top = (int)(feed[1]*height);
//                int right = (int)(feed[2]*width);
//                int bottom = (int)(feed[3]*height);
//                // Get the imgView
//                ImageView imgView = (ImageView) findViewById(R.id.imgView);
//                //Create a new image bitmap and attach a brand new canvas to it
//                Bitmap tempBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
//                Paint myRectPaint = new Paint();
//                myRectPaint.setColor(Color.RED);
//                myRectPaint.setStyle(Paint.Style.STROKE);
//                myRectPaint.setStrokeWidth(5);
//                //Draw the image bitmap into the cavas
//                Canvas tempCanvas = new Canvas(tempBitmap);
//                //Draw the image bitmap into the cavas
//                tempCanvas.drawBitmap(myBitmap, 0, 0, null);
//                tempCanvas.drawRoundRect(new RectF(left,top,right,bottom), 3, 3, myRectPaint);
//                // Set the Image in ImageView after decoding the String
//                imgView.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));
//            }

        }
    }


    class ClientMessageSender implements Callable<String> {
        private Socket socket;
        private String filename;
        private int query_id;

        public ClientMessageSender(Socket socket, String filename, int query_id) {
            this.socket = socket;
            this.filename = filename;
            this.query_id = query_id;
        }

        public String call() throws Exception {
            String content = null;
            try {
                DataOutputStream writer = new DataOutputStream(this.socket.getOutputStream());
                if(this.socket.isClosed()){
                    content = "[===Warning===]Socket is closed, cannot send message!";
                    writer.close();
                }

                writer.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(this.query_id).array(),0,4);

                System.out.println("reading "+this.filename);
                FileInputStream fileReader = new FileInputStream(this.filename);
                int length = fileReader.available();
                writer.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(length).array(),0,4);
                byte[] msg = new byte[length];
                fileReader.read(msg);

                //System.out.println("sending "+this.filename);
                writer.write(msg);

                System.out.println("waiting for response");
                content = "finished";
            } catch (Exception e) {
                e.printStackTrace();
                this.socket.close();
            }
            return content;
        }

    }

    class ClientMessageReceiver implements Callable<String> {
        private Socket socket;

        public ClientMessageReceiver(Socket socket) {
            this.socket=socket;
        }

        public String call() throws Exception {
            String content = null;
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), "UTF-8"));
                if(this.socket.isClosed()){
                    content = "[===Warning===]Socket is closed, cannot receive message!";
                    reader.close();
                }
                content=reader.readLine();
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
                this.socket.close();
            }
            return content;
        }
    }


}
