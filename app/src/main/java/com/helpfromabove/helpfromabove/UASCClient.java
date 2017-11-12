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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Created by Michael Purcell on 10/16/2017.
 */

public class UASCClient {
    private static final String TAG = "Heartbeat";

    //Used for JSON Keys don't make consistent with other Strings
    public static final String ALTITUDE = "ALTITUDE";
    public static final String LONGITUDE = "LONGITUDE";
    public static final String LATITUDE = "LATITUDE";
    public static final String STATUS = "STATUS";

    private Context context;
    private String hostIP;
    private String port;

    //Use for runnable methods that run every x seconds
    private HandlerThread constantHandlerThread;
    private Handler constantHandler;

    //Use for one time runnable methods
    private HandlerThread oneTimeHandlerThread;
    private Handler oneTimeHandler;


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
    private Location latestUASLocation;

    private boolean sessionActive = false;

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

    public void startImageAccess(String imageEndpoint, String advanceImageEndpoint , int imageAccessDelay){

        this.imageEndpoint = imageEndpoint;
        this.imageAccessDelay = imageAccessDelay;
        initializeAccessServerImage(advanceImageEndpoint);

        //Need to wait at least a second before grabbing images so that
        //the session folder has time to be made in the users cloud service.
        constantHandler.postDelayed(accessServerImageRunnable,3000);

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
        return latestUASLocation;
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

    public void toogleLight(String lightEndpoint, boolean on){
        this.lightEndpoint = lightEndpoint;
        initializeLight(on);
        oneTimeHandler.post(lightRunnable);
    }

    public void sendStartSession(String startSessionEndpoint){
        this.startSessionEndpoint = startSessionEndpoint;
        initializeStartSession();
        oneTimeHandler.post(startSessionRunnable);
    }

    public void sendEndSession(String endSessionEndpoint){
        sessionActive = false;
        this.endSessionEndpoint = endSessionEndpoint;
        initializeEndSession();
        oneTimeHandler.post(endSessionRunnable);
    }

    public void sendNewWaypoint(String newWaypointEndpoint, Location waypoint){
        this.newWaypointEndpoint = newWaypointEndpoint;
        initializeNewWaypoint(waypoint);
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
                    URL url = new URL("http://"+hostIP+":"+port);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
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

    private void initializeAccessServerImage(final String advanceImageEndpoint) {
        accessServerImageRunnable = new Runnable() {

            public void run() {
                try {
                    URL tempUrl = new URL("http://" + hostIP + ":" + port + "/" + imageEndpoint);

                    //Testing code needed for until the server is set up
                    //Start of testing code
//                    URL tempUrl = new URL(testURLs.get(currentImageNumber));
//                    if (currentImageNumber == testURLs.size() - 1) {
//                        currentImageNumber = 0;
//                    } else {
//                        currentImageNumber++;
//                    }
                    //End of testing code

                    InputStream inputStream = tempUrl.openStream();

                    imageBitmap = BitmapFactory.decodeStream(inputStream);

                    //Only broadcast new image if there is one.
                    if (imageBitmap != null)
                        CommandService.notifyNewUasImageAvailable(context);

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(sessionActive)
                    constantHandler.postDelayed(this, imageAccessDelay);
            }

        };
    }

    private void initializeAccessGPS(){
        accessGPSRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("http://"+hostIP+":"+port+"/"+gpsEndpoint);

                    InputStreamReader inputStreamReader = new InputStreamReader(url.openStream());
                    BufferedReader reader = new BufferedReader(inputStreamReader);
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while((line = reader.readLine()) != null){
                        stringBuilder.append(line);
                    }
                    reader.close();

                    JSONObject jsonObject = new JSONObject(stringBuilder.toString());

                    String status = jsonObject.getString(STATUS);

                    double altitude = jsonObject.getDouble(ALTITUDE);
                    double latitude = jsonObject.getDouble(LATITUDE);
                    double longitude = jsonObject.getDouble(LONGITUDE);

                    //This works for storing the location values in a location object
                    latestUASLocation = new Location("");

                    latestUASLocation.setAltitude(altitude);
                    latestUASLocation.setLatitude(latitude);
                    latestUASLocation.setLongitude(longitude);

                    CommandService.notifyNewWaypointAvailable(context);


                } catch (MalformedURLException e) {
                    Log.e(TAG,e.getMessage());
                } catch (IOException e) {
                    Log.e(TAG,e.getMessage());
                } catch (JSONException e) {
                    Log.e(TAG,e.getMessage());
                }
                constantHandler.postDelayed(this,gpsAccessDelay);
            }

        };
    }

    private void initializeLight(final boolean on){
        lightRunnable = new Runnable() {
            @Override
            public void run() {

                try {

                    URL url = new URL("http://"+hostIP+":"+port+"/"+lightEndpoint);
                    //Server can detect this and act off of it.
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");

                    JSONObject jsonObject = new JSONObject();

                    jsonObject.put("LIGHT_CONTROL",on);

                    String msg = jsonObject.toString();

                    //Send the json message to the server (UASC)
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(connection.getOutputStream());
                    outputStreamWriter.write(msg);
                    outputStreamWriter.flush();
                    outputStreamWriter.close();
                    connection.disconnect();


                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
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

                    URL url = new URL("http://"+hostIP+":"+port+"/"+startSessionEndpoint);

                    InputStreamReader inputStreamReader = new InputStreamReader(url.openStream());
                    BufferedReader reader = new BufferedReader(inputStreamReader);
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while((line = reader.readLine()) != null){
                        stringBuilder.append(line);
                    }
                    reader.close();

                    JSONObject jsonObject = new JSONObject(stringBuilder.toString());

                    String status = jsonObject.getString(STATUS);

                    if(status.equals("OK"))
                    {
                        sessionActive = true;
                        //When the uasc is ready for gps access,image access and sending new waypoints
                        CommandService.notifyLocationUascCalibrationComplete(context);
                    }
                    else
                    {
                        //If the uasc is not read try again in x seconds
                        oneTimeHandler.postDelayed(this,3000);
                    }

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
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

                    URL url = new URL("http://"+hostIP+":"+port+"/"+endSessionEndpoint);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                    //Sever detects this as hitting the endpoint
                    connection.getResponseCode();

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        };
    }

    private void initializeNewWaypoint(final Location waypoint){
        newWaypointRunnable = new Runnable() {
            @Override
            public void run() {

                Log.d(TAG,"Sending test Location");

                try {

                    URL url = new URL("http://"+hostIP+":"+port+"/"+newWaypointEndpoint);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setUseCaches(false);

                    //Construct the json message
                    String latitude = Location.convert(waypoint.getLatitude(),Location.FORMAT_DEGREES);
                    String longitude = Location.convert(waypoint.getLongitude(),Location.FORMAT_DEGREES);
                    String altitude = Location.convert(waypoint.getAltitude(),Location.FORMAT_DEGREES);

                    JSONObject object = new JSONObject();
                    object.put(ALTITUDE,altitude);
                    object.put(LONGITUDE,longitude);
                    object.put(LATITUDE,latitude);

                    String msg = object.toString();

                    //Send the json message to the server (UASC)
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(connection.getOutputStream());
                    outputStreamWriter.write(msg);
                    outputStreamWriter.flush();
                    outputStreamWriter.close();
                    Log.d(TAG,"Waypoint "+connection.getResponseCode());
                    connection.disconnect();



                } catch (MalformedURLException e) {
                    Log.e(TAG,e.getMessage());
                } catch (IOException e) {
                    Log.e(TAG,e.getMessage());
                } catch (JSONException e) {
                    Log.e(TAG,e.getMessage());
                }

            }
        };
    }
}
