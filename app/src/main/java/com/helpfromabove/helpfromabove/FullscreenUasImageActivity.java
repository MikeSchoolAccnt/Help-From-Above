package com.helpfromabove.helpfromabove;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

/**
 * Created by caleb on 5/9/17.
 * <p>
 * This activity contains a large ImageView that displays images from
 * the UAS in a fullscreen format.
 */

public class FullscreenUasImageActivity extends AppCompatActivity {
    private static final String TAG = "FullscreenUasImageAc...";
    private FullscreenUasImageBroadcastReceiver fullscreenUasImageBroadcastReceiver = new FullscreenUasImageBroadcastReceiver();

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

        IntentFilter intentFilter = new IntentFilter(CommandService.ACTION_NEW_UAS_IMAGE);
        registerReceiver(fullscreenUasImageBroadcastReceiver, intentFilter);
        //handleNewImage();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();

        unregisterReceiver(fullscreenUasImageBroadcastReceiver);
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

    private void handleNewImage() {
        Log.d(TAG, "handleNewImage");

        ImageView imageView = (ImageView) findViewById(R.id.fullscreen_uas_image_view);
        Bitmap bitmap = commandService.getNewImage();

        if ((imageView != null) && (bitmap != null)) {
            imageView.setImageBitmap(bitmap);
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
            if (action != null) {
                switch (action) {
                    case CommandService.ACTION_NEW_UAS_IMAGE:
                        handleNewImage();
                        break;
                    default:
                        Log.w(TAG, "onReceive: default: action=" + action);
                        break;
                }
            }
        }
    }
}
