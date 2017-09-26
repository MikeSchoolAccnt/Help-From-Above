package com.helpfromabove.helpfromabove;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

/**
 * Created by Michael Purcell on 5/5/2017.
 */

public class SplashActivity extends Activity {
    private static final String TAG = "SplashActivity";
    private BroadcastReceiver splashActivityBroadcastReceiver;
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
                transition();
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
        startService(new Intent(this, CommandService.class));
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        registerReceiver(splashActivityBroadcastReceiver, new IntentFilter(CommandService.ACTION_UI_SERVICES_READY));
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        unregisterReceiver(splashActivityBroadcastReceiver);
    }

    public void transition() {
        Log.d(TAG, "transition");

        if (servicesReady && animationComplete) {
            Intent i = new Intent(getApplicationContext(), WifiP2pConnectActivity.class);
            startActivity(i);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
        else {
            Log.d(TAG, "transition: not ready to start WifiP2pConnectActivity");
        }
    }

    private class SplashActivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive");

            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case CommandService.ACTION_UI_SERVICES_READY:
                        servicesReady = true;
                        transition();
                        break;
                    default:
                        Log.w(TAG, "onReceive: default: action=" + action);
                        break;
                }
            }
        }
    }
}
