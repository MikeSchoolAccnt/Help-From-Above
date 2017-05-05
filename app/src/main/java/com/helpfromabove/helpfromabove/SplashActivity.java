package com.helpfromabove.helpfromabove;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.ImageView;

/**
 * Created by Michael on 5/5/2017.
 */

public class SplashActivity extends Activity{

    AnimationDrawable splashAnimation;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);


        ImageView splash = (ImageView) findViewById(R.id.splashView);
        splash.setBackgroundResource(R.drawable.splash_anim);
        splashAnimation = (AnimationDrawable) splash.getBackground();

    }

    @Override
    protected void onStart(){
        super.onStart();
        splashAnimation.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            //Don't allow skipping the splash screen until its finished
            //If this isn't wanted such as for testing comment this if out.
            if(splashAnimation.getCurrent() == splashAnimation.getFrame(splashAnimation.getNumberOfFrames()-1)) {
                transition();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    public void transition(){
        Intent i = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(i);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
