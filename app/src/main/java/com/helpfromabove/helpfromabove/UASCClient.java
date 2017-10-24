package com.helpfromabove.helpfromabove;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
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

    //Use for runnable methods that run every x seconds
    private HandlerThread constantHandlerThread;
    private Handler constantHandler;

    //Use for one time runnable methods
    private HandlerThread oneTimeHandlerThread;
    private Handler oneTimeHandler;

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

    //One time runnables
    //Used for commands that don't need to get information from the server
    //Note: lightRunnable, startSessionRunnable, and endSessionRunnable could
    //      be combined to one runnable that takes a type but is not so that
    //      it is more readable.
    private Runnable lightRunnable;
    private Runnable startSessionRunnable;
    private Runnable endSessionRunnable;
    private Runnable newWaypointRunnable;


    //The delays are how often this should happen in milliseconds
    private int heartbeatDelay;
    private int imageAccessDelay;
    private int gpsAccessDelay;

    private String imageEndpoint;
    private String gpsEndpoint;
    private String newWaypointEndpoint;
    private String lightEndpoint;
    private String startSessionEndpoint;
    private String endSessionEndpoint;
    private Bitmap imageBitmap;
    private Location lastWaypoint;


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

        constantHandlerThread = new HandlerThread("Constant-UASCClient");
        constantHandlerThread.start();
        constantHandler = new Handler(constantHandlerThread.getLooper());

        oneTimeHandlerThread = new HandlerThread("OneTime-UASCClient");
        oneTimeHandlerThread.start();
        oneTimeHandler = new Handler(oneTimeHandlerThread.getLooper());
    }

    public void startHeartbeat(int heartbeatDelay){

        this.heartbeatDelay = heartbeatDelay;
        initializeHeartbeat();
        constantHandler.postDelayed(heartbeatRunnable,0);

    }

    public void stopHeartbeat(){
        if(heartbeatRunnable != null){
            constantHandler.removeCallbacks(heartbeatRunnable);
        }
    }

    public void startImageAccess(String imageEndpoint, int imageAccessDelay){

        this.imageEndpoint = imageEndpoint;
        this.imageAccessDelay = imageAccessDelay;
        initializeAccessServerImage();
        constantHandler.postDelayed(accessServerImageRunnable,0);

    }

    public void stopImageAccess(){
        if(accessServerImageRunnable != null){
            constantHandler.removeCallbacks(accessServerImageRunnable);
        }
    }

    public Bitmap getImageBitmap(){
        return this.imageBitmap;
    }

    protected Location getNewUasLocation() {
        Log.d(TAG, "getNewUasLocation: NOT YET IMPLEMENTED!");
        return null;
    }

    public void startGPSAccess(String gpsEndpoint, int gpsAccessDelay){
        this.gpsEndpoint = gpsEndpoint;
        this.gpsAccessDelay = gpsAccessDelay;
        initializeAccessGPS();
        constantHandler.postDelayed(accessGPSRunnable,gpsAccessDelay);
    }

    public void stopGPSAccess(){
        if(accessGPSRunnable != null){
            constantHandler.removeCallbacks(accessGPSRunnable);
        }
    }

    public void toogleLight(String lightEndpoint){
        this.lightEndpoint = lightEndpoint;
        initializeLight();
        oneTimeHandler.post(lightRunnable);
    }

    public void sendStartSession(String startSessionEndpoint){
        this.startSessionEndpoint = startSessionEndpoint;
        initializeStartSession();
        oneTimeHandler.post(startSessionRunnable);
    }

    public void sendEndSession(String endSessionEndpoint){
        this.endSessionEndpoint = endSessionEndpoint;
        initializeEndSession();
        oneTimeHandler.post(endSessionRunnable);
    }

    public void sendNewWaypoint(String newWaypointEndpoint, Location location){
        this.newWaypointEndpoint = newWaypointEndpoint;
        this.lastWaypoint = location;
        initializeNewWaypoint();
        oneTimeHandler.post(newWaypointRunnable);
    }

    public void stopAllConstant(){
        constantHandlerThread.quitSafely();
    }

    public void stopAllOneTime(){
        oneTimeHandlerThread.quitSafely();
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

                constantHandler.postDelayed(this,heartbeatDelay);
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
                        CommandService.notifyNewUasImageAvailable(context);

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                constantHandler.postDelayed(this,imageAccessDelay);
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
                constantHandler.postDelayed(this,gpsAccessDelay);
            }

        };
    }

    private void initializeLight(){
        lightRunnable = new Runnable() {
            @Override
            public void run() {

                try {

                    URL tempURL = new URL("http://"+hostIP+":"+port+"/"+lightEndpoint);
                    //Server can detect this and act off of it.
                    tempURL.openConnection();

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        };
    }

    private void initializeStartSession(){
        startSessionRunnable = new Runnable() {
            @Override
            public void run() {

                try {

                    URL tempURL = new URL("http://"+hostIP+":"+port+"/"+startSessionEndpoint);
                    //Server can detect this and act off of it.
                    tempURL.openConnection();

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        };
    }

    private void initializeEndSession(){
        endSessionRunnable = new Runnable() {
            @Override
            public void run() {

                try {

                    URL tempURL = new URL("http://"+hostIP+":"+port+"/"+endSessionEndpoint);
                    //Server can detect this and act off of it.
                    tempURL.openConnection();

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        };
    }

    private void initializeNewWaypoint(){
        newWaypointRunnable = new Runnable() {
            @Override
            public void run() {

                try {

                    URL tempURL = new URL("http://"+hostIP+":"+port+"/"+newWaypointEndpoint);

                    String latitude = Location.convert(lastWaypoint.getLatitude(),Location.FORMAT_DEGREES);
                    String longitide = Location.convert(lastWaypoint.getLongitude(),Location.FORMAT_DEGREES);
                    String altitude = Location.convert(lastWaypoint.getAltitude(),Location.FORMAT_DEGREES);

                    //TODO: Send the latest waypoint to the server


                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        };
    }
}
