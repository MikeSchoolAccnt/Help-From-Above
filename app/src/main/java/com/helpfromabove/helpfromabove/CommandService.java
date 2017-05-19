package com.helpfromabove.helpfromabove;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Stack;

/**
 * Created by Caleb Smith on 5/13/2017.
 */

public class CommandService extends Service {
    protected static final String ACTION_NEW_UAS_IMAGE = "com.helpfromabove.helpfromabove.action.ACTION_NEW_UAS_IMAGE";
    protected static final String ACTION_NEW_LOCATION = "com.helpfromabove.helpfromabove.action.ACTION_NEW_LOCATION";
    protected static final String REQUEST_UAS_IMAGE = "com.helpfromabove.helpfromabove.action.REQUEST_UAS_IMAGE";
    protected static final String EXTRA_LIGHT_ON_OFF = "com.helpfromabove.helpfromabove.extra.EXTRA_LIGHT_ON_OFF";
    protected static final String EXTRA_IMAGE_FILE_NAME = "com.helpfromabove.helpfromabove.extra.EXTRA_IMAGE_FILE_NAME";
    protected static final String EXTRA_LOCATION = "com.helpfromabove.helpfromabove.extra.EXTRA_LOCATION";
    protected static final String COMMAND_HHMD_EMERGENCY = "com.helpfromabove.helpfromabove.command.COMMAND_HHMD_EMERGENCY";
    protected static final String COMMAND_HHMD_LIGHT = "com.helpfromabove.helpfromabove.command.COMMAND_HHMD_LIGHT";
    protected static final String COMMAND_HHMD_UAS_HEIGHT_UP = "com.helpfromabove.helpfromabove.command.COMMAND_HHMD_UAS_HEIGHT_UP";
    protected static final String COMMAND_HHMD_UAS_HEIGHT_DOWN = "com.helpfromabove.helpfromabove.command.COMMAND_HHMD_UAS_HEIGHT_DOWN";
    protected static final String COMMAND_HHMD_SESSION_START = "com.helpfromabove.helpfromabove.command.COMMAND_HHMD_SESSION_START";
    protected static final String COMMAND_HHMD_SESSION_END = "com.helpfromabove.helpfromabove.command.COMMAND_HHMD_SESSION_END";
    protected static final String COMMAND_HHMD_LOCATION = "com.helpfromabove.helpfromabove.command.COMMAND_HHMD_LOCATION";
    protected static final String COMMAND_UAS_IMAGE = "com.helpfromabove.helpfromabove.command.COMMAND_UAS_IMAGE";
    protected static final String COMMAND_UAS_LOCATION = "com.helpfromabove.helpfromabove.command.COMMAND_UAS_LOCATION";
    private final static String TAG = "CommandService";
    private CommandServiceBroadcastReceiver cSBR = new CommandServiceBroadcastReceiver();
    private IntentFilter cSIF = new IntentFilter();
    private Stack<String> mImageFileNamesStack = new Stack<>();
//    private Stack<Location> mLocationStack = new Stack<>();

    private int imageDebugCounter = 0;
    private ArrayList<byte[]> imageBytes = new ArrayList<>();

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: ");
        super.onCreate();
        mImageFileNamesStack = new Stack<>();
//        mLocationStack = new Stack<>();
//        TODO : add IntentFilter
        cSIF.addAction(REQUEST_UAS_IMAGE);
        cSIF.addAction(COMMAND_HHMD_EMERGENCY);
        cSIF.addAction(COMMAND_HHMD_LIGHT);
        cSIF.addAction(COMMAND_HHMD_UAS_HEIGHT_UP);
        cSIF.addAction(COMMAND_HHMD_UAS_HEIGHT_DOWN);
        cSIF.addAction(COMMAND_HHMD_SESSION_START);
        cSIF.addAction(COMMAND_HHMD_SESSION_END);
        cSIF.addAction(COMMAND_HHMD_LOCATION);
        cSIF.addAction(COMMAND_UAS_IMAGE);
        cSIF.addAction(COMMAND_UAS_LOCATION);
        registerReceiver(cSBR, cSIF);

        Bitmap bm = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.imag4240);
        ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 50, baoStream);
        imageBytes.add(baoStream.toByteArray());
        bm = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.imag4366);
        baoStream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 50, baoStream);
        imageBytes.add(baoStream.toByteArray());
        bm = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.imag4491);
        baoStream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 50, baoStream);
        imageBytes.add(baoStream.toByteArray());
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
        unregisterReceiver(cSBR);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public String getLastSessionImageFileName() {
        String imageFileName;
        if (!mImageFileNamesStack.empty()) {
            imageFileName = mImageFileNamesStack.peek();
        } else {
            Log.d(TAG, "getLastSessionImageFileName: mImageFileNamesStack is empty.");
            imageFileName = null;
        }
        return imageFileName;
    }

    public void addSessionImageFileName(String imageFileName) {
        mImageFileNamesStack.push(imageFileName);
    }

    protected void handleCommandHhmdEmergency() {
        Log.d(TAG, "handleCommandHhmdEmergency: ");
    }

    protected void handleCommandHhmdLight(boolean lightOnOff) {
        Log.d(TAG, "handleCommandHhmdLight: lightOnOff=" + lightOnOff);
        if (imageDebugCounter >= 2) {
            Log.d(TAG, "handleCommandHhmdLight: incoming image example");
            handleCommandUasImage();
        }
        imageDebugCounter++;
    }

    protected void handleCommandHhmdUasHeightUp() {
        Log.d(TAG, "handleCommandHhmdUasHeightUp: ");
    }

    protected void handleCommandHhmdUasHeightDown() {
        Log.d(TAG, "handleCommandHhmdUasHeightDown: ");
    }

    protected void handleCommandHhmdSessionStart() {
        Log.d(TAG, "handleCommandHhmdSessionStart: ");
    }

    protected void handleCommandHhmdSessionEnd() {
        Log.d(TAG, "handleCommandHhmdSessionEnd: ");
    }

    private void handleCommandHhmdLocation(Location location) {
        Log.d(TAG, "handleCommandHhmdLocation: location=" + location);
    }

    private void handleCommandUasLocation(Location location) {
        Log.d(TAG, "handleCommandUasLocation: location=" + location);
    }

    private void handleCommandUasImage() {
        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssZ");
        String filename = df.format(Calendar.getInstance().getTime()) + ".jpg";
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(imageBytes.get(imageDebugCounter % 3));
            outputStream.close();
        } catch (IOException iOE) {
            iOE.printStackTrace();
        }

        Log.d(TAG, "handleCommandUasImage: 5");

        addSessionImageFileName(filename);
        sendNewImageIntent();
    }

    private void sendNewImageIntent() {
        Log.d(TAG, "sendNewImageIntent: ");
        Intent newImageIntent = new Intent(ACTION_NEW_UAS_IMAGE);
        newImageIntent.putExtra(EXTRA_IMAGE_FILE_NAME, getLastSessionImageFileName());
        sendBroadcast(newImageIntent);
    }

    private class CommandServiceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: ");
            String action = intent.getAction();
            if (intent != null && action != null) {
                switch (action) {
                    case REQUEST_UAS_IMAGE:
                        sendNewImageIntent();
                        break;
                    case COMMAND_HHMD_EMERGENCY:
                        handleCommandHhmdEmergency();
                        break;
                    case COMMAND_HHMD_LIGHT:
                        final boolean lightOnOff = intent.getBooleanExtra(EXTRA_LIGHT_ON_OFF, true);
                        handleCommandHhmdLight(lightOnOff);
                        break;
                    case COMMAND_HHMD_UAS_HEIGHT_UP:
                        handleCommandHhmdUasHeightUp();
                        break;
                    case COMMAND_HHMD_UAS_HEIGHT_DOWN:
                        handleCommandHhmdUasHeightDown();
                        break;
                    case COMMAND_HHMD_SESSION_START:
                        handleCommandHhmdSessionStart();
                        break;
                    case COMMAND_HHMD_SESSION_END:
                        handleCommandHhmdSessionEnd();
                        break;
                    case COMMAND_HHMD_LOCATION:
                        final Location hhmdLocation = intent.getExtras().getParcelable(EXTRA_LOCATION);
                        handleCommandHhmdLocation(hhmdLocation);
                        break;
                    case COMMAND_UAS_LOCATION:
                        final Location uasLocation = intent.getExtras().getParcelable(EXTRA_LOCATION);
                        handleCommandUasLocation(uasLocation);
                        break;
                    case COMMAND_UAS_IMAGE:
                        handleCommandUasImage();
                        break;
                    default:
                        Log.d(TAG, "onReceive: default: action=" + action);
                }
            }
        }
    }
}
