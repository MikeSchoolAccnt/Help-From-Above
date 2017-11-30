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
import android.widget.Toast;

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

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CommandService.ACTION_NEW_UAS_IMAGE);
        intentFilter.addAction(CommandService.ERROR_SAVING_LOCAL_IMAGE);
        registerReceiver(fullscreenUasImageBroadcastReceiver, intentFilter);
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

    private void bindCommandService() {
        Intent commandServiceIntent = new Intent(getApplicationContext(), CommandService.class);
        startService(commandServiceIntent);
        commandServiceConnection = new FullscreenUasImageActivityServiceConnection();
        bindService(commandServiceIntent, commandServiceConnection, Context.BIND_NOT_FOREGROUND);
    }

    private void unbindCommandService() {
        Log.d(TAG, "unbindCommandService");

        unbindService(commandServiceConnection);
    }

    private void updateImageView() {
        Log.d(TAG, "updateImageView");

        if (commandService != null) {
            CommandService.SessionState sessionState = commandService.getState().getSessionState();
            ImageView imageView = (ImageView) findViewById(R.id.fullscreen_uas_image_view);
            if ((sessionState != null) && (imageView != null)) {
                switch (sessionState) {
                    case SESSION_PREPARING:
                    case SESSION_READY:
                    case SESSION_STARTING:
                    case SESSION_STOPPED:
                    case SESSION_STOPPING:
                        imageView.setImageResource(R.drawable.image_placeholder);
                        break;
                    case SESSION_RUNNING:
                    case SESSION_EMERGENCY_STARTED:
                    case SESSION_EMERGENCY_MESSAGES_SENT:
                        Bitmap bitmap = commandService.getNewImage();
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap);
                        }
                        break;
                    default:
                        Log.e(TAG, "updateImageView: default: " + sessionState);
                }
            }
        }
    }

    private void handleErrorSavingLocalImage() {
        Toast.makeText(getApplicationContext(), R.string.error_saving_local_image, Toast.LENGTH_LONG).show();
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
            updateImageView();
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
                        updateImageView();
                        break;
                    case CommandService.ERROR_SAVING_LOCAL_IMAGE:
                        handleErrorSavingLocalImage();
                        break;
                    default:
                        Log.w(TAG, "onReceive: default: action=" + action);
                        break;
                }
            }
        }
    }
}
