package com.helpfromabove.helpfromabove;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by caleb on 5/9/17.
 * <p>
 * This activity contains a large ImageView that displays images from
 * the UAS in a fullscreen format.
 */

public class FullscreenUasImageActivity extends AppCompatActivity {
    private static final String TAG = "FullscreenUasImageAc...";
    private FullscreenUasImageBroadcastReceiver fUIBR = new FullscreenUasImageBroadcastReceiver();

    private CommandService commandService;
    private ServiceConnection commandServiceConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen_uas_image);
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();

        bindCommandService();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        registerReceiver(fUIBR, new IntentFilter(CommandService.ACTION_NEW_UAS_IMAGE));
        commandService.broadcastIfServicesReady();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();

        unregisterReceiver(fUIBR);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();

        unbindCommandService();
    }

    private void bindCommandService(){
        Intent commandServiceIntent = new Intent(getApplicationContext(), CommandService.class);
        startService(commandServiceIntent);
        commandServiceConnection = new FullscreenUasImageActivityServiceConnection();
        bindService(commandServiceIntent, commandServiceConnection, Context.BIND_NOT_FOREGROUND);
    }

    private void unbindCommandService() {
        Log.d(TAG, "unbindCommandService");

        unbindService(commandServiceConnection);
    }

    private void updateFullscreenUasImageView(String uasImageFileName) {
        Log.d(TAG, "updateFullscreenUasImageView");

        if (uasImageFileName != null) {
            try {
                FileInputStream fis = openFileInput(uasImageFileName);
                FileDescriptor fD = fis.getFD();
                Bitmap imageBitmap = BitmapFactory.decodeFileDescriptor(fD);
                ImageView imageView = (ImageView) findViewById(R.id.fullscreen_uas_image_view);
                imageView.setImageBitmap(imageBitmap);
            } catch (FileNotFoundException fNFE) {
                Log.e(TAG, "setUasImage: FileNotFoundException: " + fNFE.getMessage(), fNFE);
            } catch (IOException iOE) {
                Log.e(TAG, "setUasImage: IOException: " + iOE.getMessage(), iOE);
            } catch (NullPointerException nPE) {
                Log.e(TAG, "setUasImage: NullPointerException: " + nPE.getMessage(), nPE);
            }
        }
    }

    private void setConnectedService(IBinder service) {
        Log.d(TAG, "setConnectedService");

        String serviceClassName = service.getClass().getName();
        if (serviceClassName.equals(CommandService.CommandServiceBinder.class.getName())) {
            commandService = ((CommandService.CommandServiceBinder) service).getService();
        }
    }

    private class FullscreenUasImageActivityServiceConnection implements ServiceConnection {

        private static final String TAG = "FullscreenUasImageAc...";

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            setConnectedService(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
        }
    }


    /*
     * Custom BroadcastReceiver for routing intent actions to their
     * proper methods for the FullscreenUasImageActivity.
     */
    private class FullscreenUasImageBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive");

            String action = intent.getAction();
            if (intent != null && action != null) {
                switch (action) {
                    case CommandService.ACTION_NEW_UAS_IMAGE:
                        Log.d(TAG, "onReceive: ACTION_NEW_UAS_IMAGE");

                        String imageFileName = intent.getStringExtra(CommandService.EXTRA_IMAGE_FILE_NAME);
                        updateFullscreenUasImageView(imageFileName);
                        break;
                    default:
                        Log.w(TAG, "onReceive: default: action=" + action);
                        break;
                }
            }
        }
    }
}
