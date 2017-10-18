package com.helpfromabove.helpfromabove;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.helpfromabove.helpfromabove.CommandService;
import com.helpfromabove.helpfromabove.UasCommunicationService;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Michael Purcell on 10/16/2017.
 */

public class UASCClient {
    private static final String TAG = "Heartbeat";

    private Context context;
    private String hostIP;
    private String port;

    private HandlerThread handlerThread;
    private Handler mHandler;

    //Need a separate set of these for each one because how handlers work
    private URL connectionURL;
    private HttpURLConnection connection;
    private URL imageConnectionURL;
    private HttpURLConnection imageConnection;
    private URL gpsConnectionURL;
    private HttpURLConnection gpsConneciton;


    //Time based runnables
    private Runnable heartbeatRunnable;
    private Runnable accessServerImageRunnable;
    private Runnable accessGPSRunnable;

    //The delays are how often this should happen in milliseconds
    private int heartbeatDelay;
    private int imageAccessDelay;
    private int gpsAccessDelay;

    private String imageEndpoint;
    private String gpsEndpoint;
    private Bitmap imageBitmap;
    private Drawable drawable;


    //These are only for testing
    private ArrayList<String> testURLs = new ArrayList<>();
    private int currentImageNumber = 0;

    public UASCClient(Context context, String hostIP, String port){
        this.context = context;
        this.hostIP = hostIP;
        this.port = port;

        testURLs.add("http://www.androidbegin.com/wp-content/uploads/2013/07/HD-Logo.gif");
        testURLs.add("https://pbs.twimg.com/profile_images/875749462957670400/T0lwiBK8.jpg");
        testURLs.add("http://mathworld.wolfram.com/images/gifs/SmallTriambicIcosahedron.gif");

        handlerThread = new HandlerThread("Heartbeat");
        handlerThread.start();

        mHandler = new Handler(handlerThread.getLooper());
    }

    public void startHeartbeat(int heartbeatDelay){

        this.heartbeatDelay = heartbeatDelay;
        initializeHeartbeat();
        mHandler.postDelayed(heartbeatRunnable,0);

    }

    public void stopHeartbeat(){
        if(heartbeatRunnable != null){
            mHandler.removeCallbacks(heartbeatRunnable);
        }
    }

    public void startImageAccess(String imageEndpoint, int imageAccessDelay){

        this.imageEndpoint = imageEndpoint;
        this.imageAccessDelay = imageAccessDelay;
        initializeAccessServerImage();
        mHandler.postDelayed(accessServerImageRunnable,0);

    }

    public void stopImageAccess(){
        if(accessServerImageRunnable != null){
            mHandler.removeCallbacks(accessServerImageRunnable);
        }
    }

    public Bitmap getImageBitmap(){
        return this.imageBitmap;
    }


    public void startGPSAccess(String gpsEndpoint, int gpsAccessDelay){
        this.gpsEndpoint = gpsEndpoint;
        this.gpsAccessDelay = gpsAccessDelay;
        initializeAccessGPS();
        mHandler.postDelayed(accessGPSRunnable,gpsAccessDelay);
    }

    public void stopGPSAccess(){
        if(accessGPSRunnable != null){
            mHandler.removeCallbacks(accessGPSRunnable);
        }
    }

    public void stopAll(){
        handlerThread.quitSafely();
    }

    private void initializeHeartbeat(){

        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                try {

                    Log.d(TAG,hostIP+":"+port);
                    connectionURL = new URL("http://"+hostIP+":"+port);

                    connection = (HttpURLConnection) connectionURL.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);

                    OutputStream outputStream = connection.getOutputStream();

                    OutputStreamWriter osw = new OutputStreamWriter(outputStream);

                    String ping = "Ping";


                    Log.d(TAG,"Sending Ping");
                    osw.write(ping,0,ping.length());
                    osw.flush();
                    osw.close();
                    int code = connection.getResponseCode();

                    connection.disconnect();
                    Log.d(TAG,"Responce Code: " + code);
                } catch (FileNotFoundException e) {
                    Log.e(TAG,e.getMessage());
                } catch (IOException e) {
                    Log.e(TAG,e.getMessage());
                }

                mHandler.postDelayed(this,heartbeatDelay);
            }
        };
    }

    private void initializeAccessServerImage(){
        accessServerImageRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    //imageConnectionURL = new URL("http://"+hostIP+":"+port+"/"+imageEndpoint);

                    //Testing code needed for until the server is set up
                    //Start of testing code
                    imageConnectionURL = new URL(testURLs.get(currentImageNumber));
                    if(currentImageNumber == testURLs.size()-1) {
                        currentImageNumber = 0;
                    }
                    else {
                        currentImageNumber++;
                    }
                    //End of testing code

                    InputStream inputStream = imageConnectionURL.openStream();

                    imageBitmap = BitmapFactory.decodeStream(inputStream);

                    //Only broadcast new image if there is one.
                    if(imageBitmap != null)
                        context.sendBroadcast(new Intent(CommandService.ACTION_NEW_UAS_IMAGE));

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mHandler.postDelayed(this,imageAccessDelay);
            }
        };
    }

    private void initializeAccessGPS(){
        accessGPSRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    gpsConnectionURL = new URL("http://"+hostIP+":"+port+"/"+gpsEndpoint);
                    gpsConneciton = (HttpURLConnection) gpsConnectionURL.openConnection();

                    //TODO implement getting the gps form the server

                    //TODO implement how the gps information is sent to the CommandService
                    //This should be done by broadcasting it in someway.


                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mHandler.postDelayed(this,gpsAccessDelay);
            }

        };
    }

}
