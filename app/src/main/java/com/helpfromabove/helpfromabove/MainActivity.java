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
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
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

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
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
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationView = (NavigationView) findViewById(R.id.navigation);
        navigationView.setNavigationItemSelectedListener(this);

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
        intentFilter.addAction(CommandService.ERROR_SAVING_LOCAL_IMAGE);
        intentFilter.addAction(CommandService.ACTION_NEW_UAS_LOCATION);
        intentFilter.addAction(CommandService.ACTION_NEW_HHMD_LOCATION);
        registerReceiver(mainActivityBroadcastReceiver, intentFilter);

        updateUiState();
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
    public void onBackPressed() {

        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        Log.d(TAG, "onNavigationItemSelected: menuItem.toString()=" + menuItem.toString());

        if ((commandService != null) && (commandService.getState().getSessionState() == CommandService.SessionState.SESSION_STOPPED)) {
            int id = menuItem.getItemId();
            switch (id) {
                case R.id.cloud_storage_settings:
                    Log.d(TAG, "onNavigationItemSelected: cloud_storage_settings");
                    startActivity(new Intent(this, CloudPreferencesActivity.class));
                    return true;
                case R.id.emergency_settings:
                    Log.d(TAG, "onNavigationItemSelected: emergency_settings");
                    startActivity(new Intent(this, EmergencyPreferencesActivity.class));
                    return true;
                case R.id.session_settings:
                    Log.d(TAG, "onNavigationItemSelected: session_settings");
                    startActivity(new Intent(this, SessionPreferencesActivity.class));
                    return true;
                default:
                    Log.e(TAG, "onNavigationItemSelected: default: " + id);
            }
        } else {
            displaySettingsDisabled();
        }

        return false;
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

    private void updateButtons() {
        Log.d(TAG, "updateButtons");

        if (commandService != null) {
            CommandService.SessionState sessionState = commandService.getState().getSessionState();
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
                    //This can be fixed by adding in a session state for if the messages have been delivered.
                    //The command service now knows when the messages have all been delivered.
                    // TODO : Maybe add some sort of delay since messages take a while before being received
                    emergencyButton.setEnabled(true);
                    break;
                default:
                    Log.e(TAG, "updateButtons: default: " + sessionState);
                    break;
            }
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

            updateButtons();
            updateImageView();

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

    private void updateImageView() {
        Log.d(TAG, "updateImageView");

        if (commandService != null) {
            CommandService.SessionState sessionState = commandService.getState().getSessionState();
            ImageView imageView = (ImageView) findViewById(R.id.uas_image_view);
            if ((sessionState != null) && (imageView != null)) {
                switch (sessionState) {
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
    private void displayEmergencyMessagesDelivered(){

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
                        updateImageView();
                        break;
                    case CommandService.ERROR_SAVING_LOCAL_IMAGE:
                        handleErrorSavingLocalImage();
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
