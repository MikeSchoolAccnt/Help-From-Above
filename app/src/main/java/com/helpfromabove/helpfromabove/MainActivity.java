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
 * the UAS, and holding the navigation menu to access the preference
 * activities for app settings.
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
        super.onStart();
        bindCommandService();
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CommandService.ACTION_SESSION_STATE_CHANGED);
        intentFilter.addAction(CommandService.ACTION_LOCATION_STATE_CHANGED);
        intentFilter.addAction(CommandService.ACTION_SESSION_EMERGENCY_MESSAGES_SENT);
        intentFilter.addAction(CommandService.ACTION_SESSION_EMERGENCY_MESSAGES_DELIVERED);
        intentFilter.addAction(CommandService.ACTION_NEW_UAS_IMAGE);
        intentFilter.addAction(CommandService.ERROR_SAVING_LOCAL_IMAGE);
        registerReceiver(mainActivityBroadcastReceiver, intentFilter);

        updateUiState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mainActivityBroadcastReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindCommandService();
    }

    @Override
    protected void onDestroy() {
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
        if (commandService != null) {
            CommandService.SessionState sessionState = commandService.getState().getSessionState();
            switch (sessionState) {
                case SESSION_NOT_PREPARED:
                case SESSION_READY:
                case SESSION_PREPARING:
                case SESSION_STOPPED:
                    int id = menuItem.getItemId();
                    switch (id) {
                        case R.id.cloud_storage_settings:
                            startActivity(new Intent(this, CloudPreferencesActivity.class));
                            return true;
                        case R.id.emergency_settings:
                            startActivity(new Intent(this, EmergencyPreferencesActivity.class));
                            return true;
                        case R.id.session_settings:
                            startActivity(new Intent(this, SessionPreferencesActivity.class));
                            return true;
                        case R.id.app_credits:
                            startActivity(new Intent(this, CreditsActivity.class));
                        default:
                            Log.e(TAG, "onNavigationItemSelected: default: " + id);
                    }
                    break;
                case SESSION_STARTING:
                case SESSION_RUNNING:
                case SESSION_EMERGENCY_STARTED:
                case SESSION_EMERGENCY_END:
                case SESSION_STOPPING:
                    displaySettingsDisabled();
                    break;
                default:
                    Log.w(TAG, "onNavigationItemSelected: default: sessionState=" + sessionState);
                    break;
            }
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
        unbindService(commandServiceConnection);
    }

    /*
     * OnClick handler methods
     */
    public void uasImageViewOnClick(View view) {
        fullscreenUasImage();
    }

    public void emergencyButtonOnClick(View view) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int response = getApplicationContext().checkSelfPermission(Manifest.permission.SEND_SMS);
            if (response == PackageManager.PERMISSION_GRANTED) {
                commandService.handleCommandHhmdEmergency();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.SEND_SMS}, 0);
            }
        } else {
            commandService.handleCommandHhmdEmergency();
        }
    }

    public void lightSwitchOnClick(View view) {
        boolean isChecked = ((SwitchCompat) view).isChecked();
        commandService.handleCommandHhmdLight(isChecked);
    }

    public void uasHeightUpButtonOnClick(View view) {
        commandService.handleCommandHhmdUasHeightUp();
    }

    public void uasHeightDownButtonOnClick(View view) {
        commandService.handleCommandHhmdUasHeightDown();
    }

    public void sessionStartButtonOnCLick(View view) {
        commandService.handleCommandHhmdSessionStart();
    }

    public void sessionEndButtonOnCLick(View view) {
        commandService.handleCommandHhmdSessionEnd();
    }

    private void updateButtons() {
        if (commandService != null) {
            CommandService.SessionState sessionState = commandService.getState().getSessionState();
            switch (sessionState) {
                case SESSION_NOT_PREPARED:
                case SESSION_PREPARING:
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
                case SESSION_READY:
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
                case SESSION_EMERGENCY_END:
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
        Intent fullscreenUasImageActivityIntent = new Intent(getApplicationContext(), FullscreenUasImageActivity.class);
        startActivity(fullscreenUasImageActivityIntent);
    }

    /*
     * Methods for handling events caused by receiving new broadcasts
     */
    private void handleSessionStateChanged() {
        if (commandService != null) {
            updateButtons();
            updateImageView();
        }
    }

    private void handleLocationStateChanged() {
        if (commandService != null) {
            CommandService.LocationState locationState = commandService.getState().getLocationState();
            switch (locationState) {
                case LOCATION_CALIBRATING:
                    setCalibratingDialogTitle(R.string.location_calibrating_dialog_title);
                    showCalibratingDialog();
                    break;
                case LOCATION_HHMD_CALIBRATED:
                    setCalibratingDialogTitle(R.string.location_hhmd_calibrated_dialog_title);
                    showCalibratingDialog();
                    break;
                case LOCATION_UASC_CALIBRATED:
                    setCalibratingDialogTitle(R.string.location_uasc_calibrated_dialog_title);
                    showCalibratingDialog();
                    break;
                case LOCATION_CALIBRATED:
                    dismissCalibratingDialog();
                    break;
                default:
                    Log.w(TAG, "handleLocationStateChanged: default: locationState=" + locationState);
            }
        }
    }

    private void updateImageView() {
        if (commandService != null) {
            CommandService.SessionState sessionState = commandService.getState().getSessionState();
            ImageView imageView = (ImageView) findViewById(R.id.uas_image_view);
            if ((sessionState != null) && (imageView != null)) {
                switch (sessionState) {
                    case SESSION_NOT_PREPARED:
                    case SESSION_PREPARING:
                    case SESSION_READY:
                    case SESSION_STARTING:
                    case SESSION_STOPPED:
                    case SESSION_STOPPING:
                        imageView.setImageResource(R.drawable.image_placeholder);
                        break;
                    case SESSION_RUNNING:
                    case SESSION_EMERGENCY_STARTED:
                    case SESSION_EMERGENCY_END:
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

    private void prepareSessionIfNotPrepared() {
        CommandService.SessionState sessionState = commandService.getState().getSessionState();
        switch (sessionState) {
            case SESSION_PREPARING:
            case SESSION_READY:
            case SESSION_STARTING:
            case SESSION_RUNNING:
            case SESSION_EMERGENCY_STARTED:
            case SESSION_EMERGENCY_END:
            case SESSION_STOPPING:
            case SESSION_STOPPED:
                break;
            case SESSION_NOT_PREPARED:
                commandService.prepareSession();
                break;
            default:
                Log.w(TAG, "prepareSessionIfNotPrepared: default: sessionState=" + sessionState);
                break;
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

    private void displayEmergencyMessagesDelivered() {
        Toast.makeText(getApplicationContext(), R.string.emergency_message_delivered_text, Toast.LENGTH_LONG).show();
    }

    private void displaySettingsDisabled() {
        Toast.makeText(getApplicationContext(), R.string.settings_disabled, Toast.LENGTH_LONG).show();
    }

    private void setConnectedService(IBinder service) {
        String serviceClassName = service.getClass().getName();
        if (serviceClassName.equals(CommandService.CommandServiceBinder.class.getName())) {
            commandService = ((CommandService.CommandServiceBinder) service).getService();
        }
    }

    private class MainActivityServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            setConnectedService(service);
            updateUiState();
            prepareSessionIfNotPrepared();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
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
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case CommandService.ACTION_SESSION_STATE_CHANGED:
                        handleSessionStateChanged();
                        break;
                    case CommandService.ACTION_LOCATION_STATE_CHANGED:
                        handleLocationStateChanged();
                        break;
                    case CommandService.ACTION_SESSION_EMERGENCY_MESSAGES_SENT:
                        displayEmergencyMessagesSent();
                        break;
                    case CommandService.ACTION_SESSION_EMERGENCY_MESSAGES_DELIVERED:
                        displayEmergencyMessagesDelivered();
                        break;
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
