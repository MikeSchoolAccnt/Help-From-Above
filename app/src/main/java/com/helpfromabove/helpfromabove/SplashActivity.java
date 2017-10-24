package com.helpfromabove.helpfromabove;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import java.util.ArrayList;

/**
 * Created by Michael Purcell on 5/5/2017.
 */

public class SplashActivity extends Activity {
    protected static int PERMISSION_READ_CONTACTS;
    protected static int PERMISSION_ACCESS_LOCATION;
    protected static int PERMISSION_SEND_SMS;
    private static final int PERMISSIONS_CODE = 0;

    private static final String TAG = "SplashActivity";
    private BroadcastReceiver splashActivityBroadcastReceiver;

    private CommandService commandService;
    private ServiceConnection commandServiceConnection;

    private Animation tempAnim;
    private ImageView splash;
    private boolean servicesReady = false;
    private boolean animationComplete = false;

    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        servicesReady = false;
        animationComplete = false;

        splash = (ImageView) findViewById(R.id.splashView);
        tempAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.splash_anim);
        tempAnim.setFillAfter(true);

        tempAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                animation.start();
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                animationComplete = true;
                requestServicesReadyBroadcast();
                askForPermissions();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        splashActivityBroadcastReceiver = new SplashActivityBroadcastReceiver();
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart: ");
        super.onStart();

        splash.startAnimation(tempAnim);
        bindCommandService();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        registerReceiver(splashActivityBroadcastReceiver, new IntentFilter(CommandService.ACTION_SERVICES_READY));
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();

        unregisterReceiver(splashActivityBroadcastReceiver);
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
        commandServiceConnection = new SplashActivityServiceConnection();
        bindService(commandServiceIntent, commandServiceConnection, Context.BIND_NOT_FOREGROUND);
    }

    private void unbindCommandService() {
        Log.d(TAG, "unbindCommandService");

        unbindService(commandServiceConnection);
    }

    private void requestServicesReadyBroadcast() {
        Log.d(TAG, "requestServicesReadyBroadcast");

        if (commandService != null) {
            commandService.broadcastIfServicesReady();
        }
        else {
            Log.e(TAG, "commandService is null!!!");
        }
    }

    private void askForPermissions() {
        Log.d(TAG, "askForPermissions");

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            PERMISSION_ACCESS_LOCATION = getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            PERMISSION_READ_CONTACTS = getApplicationContext().checkSelfPermission(Manifest.permission.READ_CONTACTS);
            PERMISSION_SEND_SMS = getApplicationContext().checkSelfPermission(Manifest.permission.SEND_SMS);

            ArrayList<String> permissions = new ArrayList<>();

            if(PERMISSION_ACCESS_LOCATION == PackageManager.PERMISSION_DENIED){
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if(PERMISSION_READ_CONTACTS == PackageManager.PERMISSION_DENIED){
                permissions.add(Manifest.permission.READ_CONTACTS);
            }
            if(PERMISSION_SEND_SMS == PackageManager.PERMISSION_DENIED){
                permissions.add(Manifest.permission.SEND_SMS);
            }

            if (permissions.size() != 0) {
                requestPermissions(permissions.toArray(new String[permissions.size()]), PERMISSIONS_CODE);
            } else {
                transitionIfReady();
            }

        } else {
            transitionIfReady();
        }

    }

    private void transitionIfReady() {
        Log.d(TAG, "transitionIfReady");

        if(servicesReady && animationComplete) {
            Intent i = new Intent(getApplicationContext(), WifiP2pConnectActivity.class);
            startActivity(i);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } else {
            Log.d(TAG, "transition: not ready to start WifiP2pConnectActivity");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_CODE:
                if (grantResults.length > 0) {
                    boolean[] permissionAcceptance = new boolean[grantResults.length];
                    for (int i = 0; i < grantResults.length; i++) {
                        permissionAcceptance[i] = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                    }

                    for (int i = 0; i < permissionAcceptance.length; i++) {
                        if (permissionAcceptance[i]) {
                            Log.d(TAG, "Permission Accepted: " + permissions[i]);
                        } else {
                            Log.d(TAG, "Permission Declined: " + permissions[i]);
                        }
                    }

                }
        }

        transitionIfReady();
    }

    private void setConnectedService(IBinder service) {
        Log.d(TAG, "setConnectedService");

        String serviceClassName = service.getClass().getName();
        if (serviceClassName.equals(CommandService.CommandServiceBinder.class.getName())) {
            commandService = ((CommandService.CommandServiceBinder) service).getService();
        }
    }

    private class SplashActivityServiceConnection implements ServiceConnection{

        private static final String TAG = "SplashActivityServic...";

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

    private class SplashActivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive");

            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case CommandService.ACTION_SERVICES_READY:
                        servicesReady = true;
                        transitionIfReady();
                        break;
                    default:
                        Log.w(TAG, "onReceive: default: action=" + action);
                        break;
                }
            }
        }
    }
}
