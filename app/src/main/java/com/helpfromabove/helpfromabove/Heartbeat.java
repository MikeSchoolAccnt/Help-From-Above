package com.helpfromabove.helpfromabove;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Michael Purcell on 10/16/2017.
 */

public class Heartbeat {
    private static final String TAG = "Heartbeat";
    private String hostIP;
    private String port;
    private HttpURLConnection connection;
    private URL connectionURL;
    private int delay;

    private HandlerThread handlerThread;
    private Handler mHandler;


    public Heartbeat(String hostIP, String port, int delay){
        this.hostIP = hostIP;
        this.port = port;
        this.delay = delay;

        handlerThread = new HandlerThread("Heartbeat");
        handlerThread.start();

        mHandler = new Handler(handlerThread.getLooper());
    }

    public void startHeartbeat(){

        mHandler.postDelayed(new Runnable() {
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
                    Log.d(TAG,"Responce Code: " + code);
                } catch (FileNotFoundException e) {
                    Log.e(TAG,e.getMessage());
                } catch (IOException e) {
                    Log.e(TAG,e.getMessage());
                }

                mHandler.postDelayed(this,delay);
            }
        },delay);

    }

    public void stopHeartbeat(){
        handlerThread.quitSafely();
    }

}
