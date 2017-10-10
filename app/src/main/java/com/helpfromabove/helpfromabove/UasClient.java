package com.helpfromabove.helpfromabove;

import android.content.ContentResolver;
import android.os.AsyncTask;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;

/**
 * Created by Michael Purcell on 10/5/2017.
 */

public class UasClient extends AsyncTask {

    private static final String TAG = "UasClient";
    private String hostIP;
    private String port;
    HttpURLConnection connection;
    URL connectionURL;

    public UasClient(String hostIP, String port){
        this.hostIP = hostIP;
        this.port = port;
    }

    @Override
    //Currently this only connects and sends the word "Ping" to the server
    protected Object doInBackground(Object[] params) {

        try {

            Log.d("UasClient",hostIP+":"+port);
            connectionURL = new URL("http://"+hostIP+":"+port);

            connection = (HttpURLConnection) connectionURL.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            OutputStream outputStream = connection.getOutputStream();

            OutputStreamWriter osw = new OutputStreamWriter(outputStream);

            String ping = "Ping";

            //Log.d("UasClient","CheckConnected: "+socket.isConnected());

            Log.d("UasClient","Sending Ping");
            osw.write(ping,0,ping.length());
            osw.flush();
            osw.close();
            int code = connection.getResponseCode();
            Log.d("UasClient","Responce Code: " + code);
        } catch (FileNotFoundException e) {
            Log.e("UasClient",e.getMessage());
        } catch (IOException e) {
            Log.e("UasClient",e.getMessage());
        }

/**
 * Clean up any open sockets when done
 * transferring or if an exception occurred.
 */

        return null;
    }

}
