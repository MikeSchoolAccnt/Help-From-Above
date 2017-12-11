package com.helpfromabove.helpfromabove;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

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
    private static final String TAG = "UASCClient";

    //Used for JSON Keys don't make consistent with other Strings
    private static final String ALTITUDE = "ALTITUDE";
    private static final String LONGITUDE = "LONGITUDE";
    private static final String LATITUDE = "LATITUDE";
    private static final String STATUS = "STATUS";

    private Context context;
    private String hostIP;
    private String port;

    private HandlerThread heartbeatHandlerThread;
    private HandlerThread imageAccessHandlerThread;
    private HandlerThread gpsAccessHandlerThread;
    private HandlerThread waypointSendHandlerThread;
    private HandlerThread toggleLightHandlerThread;
    private HandlerThread sessionHandlerThread;


    private Handler heartbeatHandler;
    private Handler imageAccessHandler;
    private Handler gpsAccessHandler;
    private Handler waypointSendHandler;
    private Handler toggleLightHandler;
    private Handler sessionHandler;


    private Runnable heartbeatRunnable;
    private Runnable accessServerImageRunnable;
    private Runnable accessGPSRunnable;
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
    private boolean debugging = false;

    public UASCClient(Context context, String hostIP, String port){
        this.context = context;
        this.hostIP = hostIP;
        this.port = port;

        testURLs.add("http://www.androidbegin.com/wp-content/uploads/2013/07/HD-Logo.gif");
        testURLs.add("https://pbs.twimg.com/profile_images/875749462957670400/T0lwiBK8.jpg");
        testURLs.add("http://mathworld.wolfram.com/images/gifs/SmallTriambicIcosahedron.gif");

        startHeartbeatHandlerThread();
        startImageAccessHandlerThread();
        startGpsAccessHandlerThread();
        startWaypointSendHandlerThread();
        startToggleLightHandlerThread();
        startSessionHandlerThread();

        heartbeatHandler = new Handler(heartbeatHandlerThread.getLooper());
        imageAccessHandler = new Handler(imageAccessHandlerThread.getLooper());
        gpsAccessHandler = new Handler (gpsAccessHandlerThread.getLooper());
        waypointSendHandler = new Handler(waypointSendHandlerThread.getLooper());
        toggleLightHandler = new Handler(toggleLightHandlerThread.getLooper());
        sessionHandler = new Handler(sessionHandlerThread.getLooper());
    }

    public void startHeartbeat(int heartbeatDelay){

        this.heartbeatDelay = heartbeatDelay;
        initializeHeartbeat();
        heartbeatHandler.postDelayed(heartbeatRunnable,0);

    }

    public void stopHeartbeat(){
        if(heartbeatRunnable != null){
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
        }
    }

    public void startImageAccess(String imageEndpoint, int imageAccessDelay){

        this.imageEndpoint = imageEndpoint;
        this.imageAccessDelay = imageAccessDelay;
        initializeAccessServerImage();

        //Need to wait at least a second before grabbing images so that
        //the session folder has time to be made in the users cloud service.
        imageAccessHandler.postDelayed(accessServerImageRunnable,3000);

    }

    public void stopImageAccess(){
        if(accessServerImageRunnable != null){
            imageAccessHandler.removeCallbacks(accessServerImageRunnable);
        }
    }

    public Bitmap getImageBitmap(){
        return this.imageBitmap;
    }

    protected Location getNewUasLocation() {
        return latestUASLocation;
    }

    public void startGPSAccess(String gpsEndpoint, int gpsAccessDelay){
        this.gpsEndpoint = gpsEndpoint;
        this.gpsAccessDelay = gpsAccessDelay;
        initializeAccessGPS();
        gpsAccessHandler.postDelayed(accessGPSRunnable,gpsAccessDelay);
    }

    public void stopGPSAccess(){
        if(accessGPSRunnable != null){
            gpsAccessHandler.removeCallbacks(accessGPSRunnable);
        }
    }

    public void toogleLight(String lightEndpoint, boolean on){
        this.lightEndpoint = lightEndpoint;
        initializeLight(on);
        toggleLightHandler.post(lightRunnable);
    }

    public void sendStartSession(String startSessionEndpoint){
        this.startSessionEndpoint = startSessionEndpoint;
        initializeStartSession();
        sessionHandler.post(startSessionRunnable);
    }

    public void sendEndSession(String endSessionEndpoint){
        sessionActive = false;
        this.endSessionEndpoint = endSessionEndpoint;
        initializeEndSession();
        sessionHandler.post(endSessionRunnable);
    }

    public void sendNewWaypoint(String newWaypointEndpoint, Location waypoint){
        this.newWaypointEndpoint = newWaypointEndpoint;
        initializeNewWaypoint(waypoint);
        waypointSendHandler.post(newWaypointRunnable);
    }

    //All of the methods for starting and stopping handler threads
    //region HandlerThreads
    public void startHeartbeatHandlerThread(){
        heartbeatHandlerThread = new HandlerThread("UASC-Heartbeat");
        heartbeatHandlerThread.start();
    }
    public void stopHeartBeatHandlerThread(){
        heartbeatHandlerThread.quitSafely();
    }

    public void startImageAccessHandlerThread(){
        imageAccessHandlerThread = new HandlerThread("UASC-Image_Access");
        imageAccessHandlerThread.start();
    }
    public void stopImageAccessHandlerThread(){
        imageAccessHandlerThread.quitSafely();
    }

    public void startGpsAccessHandlerThread() {
        gpsAccessHandlerThread = new HandlerThread("UASC-GPS_Access");
        gpsAccessHandlerThread.start();
    }
    public void stopGpsAccessHandlerThread(){
        gpsAccessHandlerThread.quitSafely();
    }

    public void startWaypointSendHandlerThread(){
        waypointSendHandlerThread = new HandlerThread("UASC-Waypoint_Send");
        waypointSendHandlerThread.start();
    }
    public void stopWaypointSendHandlerThread(){
        waypointSendHandlerThread.quitSafely();
    }

    public void startToggleLightHandlerThread(){
        toggleLightHandlerThread = new HandlerThread("UASC-Toggle_Light");
        toggleLightHandlerThread.start();
    }
    public void stopToggleLightHandlerThread(){
        toggleLightHandlerThread.quitSafely();
    }

    public void startSessionHandlerThread(){
        sessionHandlerThread = new HandlerThread("UASC-Session");
        sessionHandlerThread.start();
    }
    public void stopSessionHandlerThread(){
        sessionHandlerThread.quitSafely();
    }
    //endregion


    private String readStreamToString(InputStream inputStream) throws IOException {

        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader reader = new BufferedReader(inputStreamReader);
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while((line = reader.readLine()) != null){
            stringBuilder.append(line);
        }
        reader.close();

        return stringBuilder.toString();

    }

    private void jsonWriteToOutputSteam(JSONObject message, OutputStream outputStream) throws IOException {
        String msg = message.toString();

        //Send the json message to the server (UASC)
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
        outputStreamWriter.write(msg);
        outputStreamWriter.flush();
        outputStreamWriter.close();
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

                heartbeatHandler.postDelayed(this,heartbeatDelay);
            }
        };
    }

    private void initializeAccessServerImage() {
        accessServerImageRunnable = new Runnable() {

            public void run() {
                try {

                    URL tempUrl;
                    if(!debugging) {
                        tempUrl = new URL("http://" + hostIP + ":" + port + "/" + imageEndpoint);
                    }
                    else {
                        //Testing code needed for until the server is set up
                        //Start of testing code
                        tempUrl = new URL(testURLs.get(currentImageNumber));
                        if (currentImageNumber == testURLs.size() - 1) {
                            currentImageNumber = 0;
                        } else {
                            currentImageNumber++;
                        }
                        //End of testing code
                    }

                    imageBitmap = BitmapFactory.decodeStream(tempUrl.openStream());

                    //Only broadcast new image if there is one.
                    if (imageBitmap != null)
                        CommandService.notifyNewUasImageAvailable(context);

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(sessionActive)
                    imageAccessHandler.postDelayed(this, imageAccessDelay);
            }

        };
    }

    private void initializeAccessGPS(){
        accessGPSRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("http://"+hostIP+":"+port+"/"+gpsEndpoint);

                    String serverMessage = readStreamToString(url.openStream());

                    JSONObject jsonObject = new JSONObject(serverMessage);

                    //String status = jsonObject.getString(STATUS);

                    double altitude = jsonObject.getDouble(ALTITUDE);
                    double latitude = jsonObject.getDouble(LATITUDE);
                    double longitude = jsonObject.getDouble(LONGITUDE);

                    //This works for storing the location values in a location object
                    latestUASLocation = new Location("");

                    latestUASLocation.setAltitude(altitude);
                    latestUASLocation.setLatitude(latitude);
                    latestUASLocation.setLongitude(longitude);

                    Log.d(TAG,"Received UAS Location.");
                    CommandService.notifyNewUasLocationAvailable(context);


                } catch (MalformedURLException e) {
                    Log.e(TAG,e.getMessage());
                } catch (IOException e) {
                    Log.e(TAG,e.getMessage());
                } catch (JSONException e) {
                    Log.e(TAG,e.getMessage());
                }
                gpsAccessHandler.postDelayed(this,gpsAccessDelay);
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

                    Log.d(TAG, "Sending light toggle.");

                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(connection.getOutputStream());
                    outputStreamWriter.write(msg);
                    outputStreamWriter.flush();
                    outputStreamWriter.close();

                    connection.getResponseCode();

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

                    String serverMessage = readStreamToString(url.openStream());

                    JSONObject jsonObject = new JSONObject(serverMessage);

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
                        sessionHandler.postDelayed(this,3000);
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



                try {

                    URL url = new URL("http://"+hostIP+":"+port+"/"+newWaypointEndpoint);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json");

                    //Construct the json message
                    String latitude = Location.convert(waypoint.getLatitude(),Location.FORMAT_DEGREES);
                    String longitude = Location.convert(waypoint.getLongitude(),Location.FORMAT_DEGREES);
                    double altitude = waypoint.getAltitude();

                    JSONObject object = new JSONObject();
                    object.put(ALTITUDE,altitude);
                    object.put(LONGITUDE,longitude);
                    object.put(LATITUDE,latitude);

                    String msg = object.toString();

                    Log.d(TAG,"Sending waypoint:" + msg);

                    //Send the json message to the server (UASC)
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(connection.getOutputStream());
                    outputStreamWriter.write(msg);
                    outputStreamWriter.flush();
                    outputStreamWriter.close();

                    Log.d(TAG,"WaypointEndpoint Responce Code: " +connection.getResponseCode());

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
