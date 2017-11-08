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
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

/**
 * Created by Caleb Smith on 5/4/2017.
 * <p>
 * The MainActivity is the main screen for Help From Above.
 * It contains the user commands, the view for displaying images from
 * the UAS, and holding the menu for accessing the SettingsActivity.
 */

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
    private MainActivityBroadcastReceiver mainActivityBroadcastReceiver = new MainActivityBroadcastReceiver();

    private CommandService commandService;
    private ServiceConnection commandServiceConnection;

    private AlertDialog calibratingAlertDialog;
    private Button endSessionButton;
    private Button startSessionButton;
    private Button emergencyButton;
    private Button uasHeightDownButton;
    private Button uasHeightUpButton;
    private SwitchCompat lightSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        startSessionButton = (Button) findViewById(R.id.session_start_button);
        endSessionButton = (Button) findViewById(R.id.session_end_button);
        emergencyButton = (Button) findViewById(R.id.emergency_button);
        uasHeightDownButton = (Button) findViewById(R.id.uas_height_down_button);
        uasHeightUpButton = (Button) findViewById(R.id.uas_height_up_button);
        lightSwitch = (SwitchCompat) findViewById(R.id.light_switch);
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
        intentFilter.addAction(CommandService.ACTION_SESSION_STATE_CHANGED);
        intentFilter.addAction(CommandService.ACTION_LOCATION_STATE_CHANGED);
        intentFilter.addAction(CommandService.ACTION_NEW_UAS_IMAGE);
        intentFilter.addAction(CommandService.ACTION_NEW_UAS_LOCATION);
        intentFilter.addAction(CommandService.ACTION_NEW_HHMD_LOCATION);
        registerReceiver(mainActivityBroadcastReceiver, intentFilter);

        updateUiState();
        //handleNewImage();
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

        unbindCommandService();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();

        dismissCalibratingDialog();
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

            if ((commandService != null) && (commandService.getState().getSessionState() == CommandService.SessionState.SESSION_STOPPED)) {
                startActivity(new Intent(this, SettingsActivity.class));
            } else {
                displaySettingsDisabled();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateUiState() {
        handleSessionStateChanged();
        handleLocationStateChanged();
    }

    private void bindCommandService() {
        Intent commandServiceIntent = new Intent(getApplicationContext(), CommandService.class);
        startService(commandServiceIntent);
        commandServiceConnection = new MainActivityServiceConnection();
        bindService(commandServiceIntent, commandServiceConnection, Context.BIND_NOT_FOREGROUND);
    }

    private void unbindCommandService() {
        Log.d(TAG, "unbindCommandService");

        unbindService(commandServiceConnection);
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
                commandService.handleCommandHhmdEmergency();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.SEND_SMS}, 0);
            }
        } else {
            commandService.handleCommandHhmdEmergency();
        }
    }

    public void lightSwitchOnClick(View view) {
        Log.d(TAG, "lightSwitchOnClick");

        boolean isChecked = ((SwitchCompat) view).isChecked();
        commandService.handleCommandHhmdLight(isChecked);
    }

    public void uasHeightUpButtonOnClick(View view) {
        Log.d(TAG, "uasHeightUpButtonOnClick ");

        commandService.handleCommandHhmdUasHeightUp();
    }

    public void uasHeightDownButtonOnClick(View view) {
        Log.d(TAG, "uasHeightDownButtonOnClick");

        commandService.handleCommandHhmdUasHeightDown();
    }

    public void sessionStartButtonOnCLick(View view) {
        Log.d(TAG, "sessionStartButtonOnCLick");

        commandService.handleCommandHhmdSessionStart();
    }

    public void sessionEndButtonOnCLick(View view) {
        Log.d(TAG, "sessionEndButtonOnCLick");

        commandService.handleCommandHhmdSessionEnd();
    }


    private void enableButtons(CommandService.SessionState sessionState) {
        Log.d(TAG, "enableButtons: sessionState=" + sessionState);
        switch (sessionState) {
            case SESSION_STARTING:
            case SESSION_STOPPING:
                emergencyButton.setEnabled(false);
                lightSwitch.setEnabled(false);
                uasHeightUpButton.setEnabled(false);
                uasHeightDownButton.setEnabled(false);
                startSessionButton.setEnabled(false);
                endSessionButton.setEnabled(false);
                break;
            case SESSION_RUNNING:
                emergencyButton.setEnabled(true);
                lightSwitch.setEnabled(true);
                uasHeightUpButton.setEnabled(true);
                uasHeightDownButton.setEnabled(true);
                startSessionButton.setEnabled(false);
                endSessionButton.setEnabled(true);
                break;
            case SESSION_STOPPED:
                emergencyButton.setEnabled(false);
                lightSwitch.setEnabled(false);
                uasHeightUpButton.setEnabled(false);
                uasHeightDownButton.setEnabled(false);
                startSessionButton.setEnabled(true);
                endSessionButton.setEnabled(false);
                break;
            case SESSION_EMERGENCY_STARTED:
                emergencyButton.setEnabled(false);
                break;
            case SESSION_EMERGENCY_MESSAGES_SENT:
                // TODO : Maybe add some sort of delay since messages take a while before received by contacts
                emergencyButton.setEnabled(true);
                break;
            default:
                Log.e(TAG, "enableButtons: default");
                break;
        }
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
    private void handleSessionStateChanged() {
        if (commandService != null) {
            CommandService.SessionState state = commandService.getState().getSessionState();

            enableButtons(state);

            switch (state) {
                case SESSION_EMERGENCY_MESSAGES_SENT:
                    displayEmergencyMessagesSent();
                    break;
                default:
                    Log.w(TAG, "handleSessionStateChanged: default: " + state);
            }
        }
    }

    private void handleLocationStateChanged() {
        if (commandService != null) {
            switch (commandService.getState().getLocationState()) {
                case LOCATION_CALIBRATING:
                    setCalibratingDialogTitle(R.string.location_calibrating_dialog_title);
                    showCalibratingDialog();
                    break;
                case LOCATION_HHMD_CALIBRATED:
                    setCalibratingDialogTitle(R.string.location_hhmd_calibrated_dialog_title);
                    showCalibratingDialog();
                    break;
                case LOCATION_UASC_CALIBRATED:
                    // This never gets displayed (or if it does, it's extremely brief)
                    // this is because when once the hhmd and uasc are calibrated, it
                    // immediately goes to the LOCATION_CALIBRATED state.
                    // TODO : Maybe delay time between state change?
                    setCalibratingDialogTitle(R.string.location_uasc_calibrated_dialog_title);
                    showCalibratingDialog();
                    break;
                case LOCATION_CALIBRATED:
                    dismissCalibratingDialog();
                    break;
                default:
                    Log.w(TAG, "handleLocationStateChanged: default");
            }
        }
    }

    private void handleNewImage() {
        Log.d(TAG, "handleNewImage");

        ImageView imageView = (ImageView) findViewById(R.id.uas_image_view);
        Bitmap bitmap = commandService.getNewImage();

        if ((imageView != null) && (bitmap != null)) {
            imageView.setImageBitmap(bitmap);
        }
    }

    private void createCalibratingDialog() {
        if (calibratingAlertDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(new ProgressBar(getApplicationContext()))
                    .setCancelable(false);
            calibratingAlertDialog = builder.create();
        }
    }

    private void setCalibratingDialogTitle(int id) {
        if (calibratingAlertDialog == null) {
            createCalibratingDialog();
        }

        calibratingAlertDialog.setTitle(id);
    }

    private void showCalibratingDialog() {
        if (calibratingAlertDialog == null) {
            createCalibratingDialog();
        }

        if (!calibratingAlertDialog.isShowing()) {
            calibratingAlertDialog.show();
        }
    }


    private void dismissCalibratingDialog() {
        if (calibratingAlertDialog == null) {
            createCalibratingDialog();
        }

        if (calibratingAlertDialog.isShowing()) {
            calibratingAlertDialog.dismiss();
        }
    }

    private void displayEmergencyMessagesSent() {
        Toast.makeText(getApplicationContext(), R.string.emergency_message_sent_text, Toast.LENGTH_LONG).show();
    }

    private void displaySettingsDisabled() {
        Toast.makeText(getApplicationContext(), R.string.settings_disabled, Toast.LENGTH_LONG).show();
    }

    private void setConnectedService(IBinder service) {
        Log.d(TAG, "setConnectedService");

        String serviceClassName = service.getClass().getName();
        if (serviceClassName.equals(CommandService.CommandServiceBinder.class.getName())) {
            commandService = ((CommandService.CommandServiceBinder) service).getService();
        }
    }

    private class MainActivityServiceConnection implements ServiceConnection {

        private static final String TAG = "MainActivityServiceC...";

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            setConnectedService(service);
            updateUiState();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            updateUiState();
            handleNewImage();
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
            if (action != null) {
                switch (action) {
                    case CommandService.ACTION_SESSION_STATE_CHANGED:
                        handleSessionStateChanged();
                        break;
                    case CommandService.ACTION_LOCATION_STATE_CHANGED:
                        handleLocationStateChanged();
                        break;
                    case CommandService.ACTION_NEW_UAS_IMAGE:
                        handleNewImage();
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
