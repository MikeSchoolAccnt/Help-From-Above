package com.helpfromabove.helpfromabove;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Switch;

/**
 * Created by Caleb Smithcs on 5/4/2017.
 */

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Log.d(TAG, "onOptionsItemSelected: action_settings");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void emergencyButtonOnClick(View view) {
        Log.d(TAG, "emergencyButtonOnClick: ");
        emergency();
    }

    public void lightSwitchOnClick(View view) {
        Log.d(TAG, "lightSwitchOnClick: ");
        boolean isChecked = ((SwitchCompat) view).isChecked();
        light(isChecked);
    }

    public void uasHeightUpButtonOnClick(View view) {
        Log.d(TAG, "uasHeightUpButtonOnClick: ");
        uasHeightUp();
    }

    public void uasHeightDownOnClick(View view) {
        Log.d(TAG, "uasHeightDownOnClick: ");
        uasHeightDown();
    }
    
    public void sessionStartButtonOnCLick(View view) {
        Log.d(TAG, "sessionStartButtonOnCLick: ");
        sessionStart();
    }


    public void sessionEndButtonOnCLick(View view) {
        Log.d(TAG, "sessionEndButtonOnCLick: ");
        sessionEnd();
    }
    
    private void emergency() {
        Log.d(TAG, "emergency: ");
    }


    private void light(boolean isChecked) {
        Log.d(TAG, "light: isChecked=" + isChecked);
    }


    private void uasHeightUp() {
        Log.d(TAG, "uasHeightUp: ");
    }
    
    private void uasHeightDown() {
        Log.d(TAG, "uasHeightDown: ");
    }
    
    private void sessionStart() {
        Log.d(TAG, "sessionStart: ");
    }
    
    private void sessionEnd() {
        Log.d(TAG, "sessionEnd: ");
    }
}
