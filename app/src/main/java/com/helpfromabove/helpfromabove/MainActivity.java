package com.helpfromabove.helpfromabove;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Created by Caleb Smith on 5/4/2017.
 *
 * The MainActivity is the main screen for Help From Above.
 * It contains the user commands, the view for displaying images from
 * the UAS, and holding the menu for accessing the SettingsActivity.
 */

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
    private MainActivityBroadcastReceiver mainActivityBroadcastReceiver = new MainActivityBroadcastReceiver();


    private CommandService commandService;
    private ServiceConnection commandServiceConnection;

    private Button endSessionButton;
    private Button startSessionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        startSessionButton = (Button) findViewById(R.id.session_start_button);
        endSessionButton = (Button) findViewById(R.id.session_end_button);
        endSessionButton.setEnabled(false);
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        bindCommandService();
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        registerReceiver(mainActivityBroadcastReceiver, new IntentFilter(CommandService.ACTION_NEW_UAS_IMAGE));
        sendBroadcast(new Intent(CommandService.ACTION_REQUEST_LAST_IMAGE_FILENAME));
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();

        unregisterReceiver(mainActivityBroadcastReceiver);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected");

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Log.d(TAG, "onOptionsItemSelected: action_settings");

            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void bindCommandService(){
        Intent commandServiceIntent = new Intent(getApplicationContext(), CommandService.class);
        startService(commandServiceIntent);
        commandServiceConnection = new MainActivityServiceConnection();
        bindService(commandServiceIntent, commandServiceConnection, Context.BIND_NOT_FOREGROUND);
    }

    /*
     * OnClick handler methods
     */
    public void uasImageViewOnClick(View view) {
        Log.d(TAG, "uasImageViewOnClick");

        fullscreenUasImage();
    }

    public void emergencyButtonOnClick(View view) {
        Log.d(TAG, "emergencyButtonOnClick");

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int response = getApplicationContext().checkSelfPermission(Manifest.permission.SEND_SMS);
            if (response == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onCreate: permission SEND_SMS is GRANTED");
                sendBroadcast(new Intent(CommandService.COMMAND_HHMD_EMERGENCY));
            } else {
                ActivityCompat.requestPermissions(this,new String[]{android.Manifest.permission.SEND_SMS},0);
            }
        } else {
            sendBroadcast(new Intent(CommandService.COMMAND_HHMD_EMERGENCY));
        }

    }

    public void lightSwitchOnClick(View view) {
        Log.d(TAG, "lightSwitchOnClick");

        boolean isChecked = ((SwitchCompat) view).isChecked();
        Intent lightIntent = new Intent(CommandService.COMMAND_HHMD_LIGHT);
        lightIntent.putExtra(CommandService.EXTRA_LIGHT_ON_OFF, isChecked);
        sendBroadcast(lightIntent);
    }

    public void uasHeightUpButtonOnClick(View view) {
        Log.d(TAG, "uasHeightUpButtonOnClick ");

        sendBroadcast(new Intent(CommandService.COMMAND_HHMD_UAS_HEIGHT_UP));
    }

    public void uasHeightDownButtonOnClick(View view) {
        Log.d(TAG, "uasHeightDownButtonOnClick");

        sendBroadcast(new Intent(CommandService.COMMAND_HHMD_UAS_HEIGHT_DOWN));
    }

    public void sessionStartButtonOnCLick(View view) {
        Log.d(TAG, "sessionStartButtonOnCLick");
        endSessionButton.setEnabled(true);
        startSessionButton.setEnabled(false);
        sendBroadcast(new Intent(CommandService.COMMAND_HHMD_SESSION_START));
    }

    public void sessionEndButtonOnCLick(View view) {
        Log.d(TAG, "sessionEndButtonOnCLick");

        endSessionButton.setEnabled(false);
        startSessionButton.setEnabled(true);

        sendBroadcast(new Intent(CommandService.COMMAND_HHMD_SESSION_END));
    }

    /*
     * Method for starting the fullscreen activity
     */
    private void fullscreenUasImage() {
        Log.d(TAG, "fullscreenUasImage");

        Intent fullscreenUasImageActivityIntent = new Intent(getApplicationContext(), FullscreenUasImageActivity.class);
        startActivity(fullscreenUasImageActivityIntent);
    }

    /*
     * Methods for handling events caused by receiving new broadcasts
     */
    public void updateUasImageView(Bitmap imageBitmap) {
        Log.d(TAG, "updateUasImageView: New uas Image");

        ImageView imageView = (ImageView) findViewById(R.id.uas_image_view);

        imageView.setImageBitmap(imageBitmap);

//        if (uasImageFileName != null) {
//            try {
//                FileInputStream fis = openFileInput(uasImageFileName);
//                Bitmap imageBitmap = BitmapFactory.decodeStream(fis);
//                ImageView imageView = (ImageView) findViewById(R.id.uas_image_view);
//                imageView.setImageBitmap(imageBitmap);
//            } catch (FileNotFoundException fNFE) {
//                Log.e(TAG, "setUasImage: FileNotFoundException: " + fNFE.getMessage(), fNFE);
//            } catch (NullPointerException nPE) {
//                Log.e(TAG, "setUasImage: NullPointerException: " + nPE.getMessage(), nPE);
//            }
//        }
    }
    private void setConnectedService(IBinder service) {
        Log.d(TAG, "setConnectedService");

        String serviceClassName = service.getClass().getName();
        if (serviceClassName.equals(CommandService.CommandServiceBinder.class.getName())) {
            commandService = ((CommandService.CommandServiceBinder) service).getService();
            commandService.startWifiP2pScanning();
        }
    }

    protected class MainActivityServiceConnection implements ServiceConnection{

        private static final String TAG = "MainActivitySe";

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
     * proper methods for the MainActivity
     */
    private class MainActivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive");

            String action = intent.getAction();
            if (intent != null && action != null) {
                switch (action) {
                    case CommandService.ACTION_NEW_UAS_IMAGE:
                        Log.d(TAG, "onReceive: ACTION_NEW_UAS_IMAGE");

                        updateUasImageView(commandService.getNewImage());
                        break;
                    case CommandService.ACTION_NEW_UAS_LOCATION:
                        Log.d(TAG, "onReceive: ACTION_NEW_UAS_LOCATION");

                        /*
                         * If we add the Google maps overlay to the
                         * UAS image, this is where we would get the
                         * UAS location
                         */
                        break;
                    case CommandService.ACTION_NEW_HHMD_LOCATION:
                        Log.d(TAG, "onReceive: ACTION_NEW_HHMD_LOCATION");

                        /*
                         * If we add the Google maps overlay to the
                         * UAS image, this is where we would receive
                         * the HHMD location
                         */
                        break;
                    default:
                        Log.w(TAG, "onReceive: default: action=" + action);
                        break;
                }
            }
        }
    }
}
