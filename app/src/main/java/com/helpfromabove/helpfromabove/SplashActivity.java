package com.helpfromabove.helpfromabove;

import android.app.Activity;
import android.content.Intent;
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

    Animation tempAnim;
    ImageView splash;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);


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
                //splash.setImageResource(R.drawable.splash_background);
                transition();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart: ");
        super.onStart();
        splash.startAnimation(tempAnim);

        startService(new Intent(this, CommandService.class));
    }

     /* Allows touching the screen to skip the splash screen
      * NEEDS TO BE FIXED : Causes two instances of the MainActivity to launch
      */
//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        if(event.getAction() == MotionEvent.ACTION_DOWN) {
//            tempAnim.cancel();
//            return true;
//        }
//        return super.onTouchEvent(event);
//    }

    public void transition() {
        Intent i = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(i);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
