package com.helpfromabove.helpfromabove;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.cloudrail.si.CloudRail;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Dropbox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Created by Caleb Smith on 5/4/2017.
 */

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";

    /*
     * These keys will change when we decide a group account. As of now they are
     * my keys (Michael Purcell)
     */
    private final static String CLOUDRAIL_LICENSE_KEY = "591cadfabac9e94ae79c9711";
    private final static String DROPBOX_APP_KEY = "5a95or0lhqau6y1";
    private final static String DROPBOX_APP_SECRET = "g31z4opqpzpklri";

    private MainActivityBroadcastReceiver mABR = new MainActivityBroadcastReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        CloudRail.setAppKey(CLOUDRAIL_LICENSE_KEY);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart: ");
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: ");
        super.onResume();

        registerReceiver(mABR, new IntentFilter(CommandService.ACTION_NEW_UAS_IMAGE));
        sendBroadcast(new Intent(CommandService.REQUEST_UAS_IMAGE));
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: ");
        super.onPause();

        unregisterReceiver(mABR);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop: ");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
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
        } else if(id == R.id.action_cloud) {
            final CloudStorage DROPBOX = new Dropbox(this, DROPBOX_APP_KEY,DROPBOX_APP_SECRET);
            createFolder(DROPBOX);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * OnClick handler methods
     */
    public void uasImageViewOnClick(View view) {
        Log.d(TAG, "uasImageViewOnClick: ");
        fullscreenUasImage();
    }

    public void emergencyButtonOnClick(View view) {
        Log.d(TAG, "emergencyButtonOnClick: ");
        sendBroadcast(new Intent(CommandService.COMMAND_HHMD_EMERGENCY));
    }

    public void lightSwitchOnClick(View view) {
        Log.d(TAG, "lightSwitchOnClick: ");
        boolean isChecked = ((SwitchCompat) view).isChecked();
        Intent lightIntent = new Intent(CommandService.COMMAND_HHMD_LIGHT);
        lightIntent.putExtra(CommandService.EXTRA_LIGHT_ON_OFF, isChecked);
        sendBroadcast(lightIntent);
    }

    public void uasHeightUpButtonOnClick(View view) {
        Log.d(TAG, "uasHeightUpButtonOnClick: ");
        sendBroadcast(new Intent(CommandService.COMMAND_HHMD_UAS_HEIGHT_UP));
    }

    public void uasHeightDownButtonOnClick(View view) {
        Log.d(TAG, "uasHeightDownButtonOnClick: ");
        sendBroadcast(new Intent(CommandService.COMMAND_HHMD_UAS_HEIGHT_DOWN));
    }

    public void sessionStartButtonOnCLick(View view) {
        Log.d(TAG, "sessionStartButtonOnCLick: ");
        sendBroadcast(new Intent(CommandService.COMMAND_HHMD_SESSION_START));
    }

    public void sessionEndButtonOnCLick(View view) {
        Log.d(TAG, "sessionEndButtonOnCLick: ");
        sendBroadcast(new Intent(CommandService.COMMAND_HHMD_SESSION_END));
    }

    private void fullscreenUasImage() {
        Log.d(TAG, "fullscreenUasImage: ");
        Intent i = new Intent(getApplicationContext(), FullscreenUasImageActivity.class);
        startActivity(i);
    }

    private void createFolder (final CloudStorage cs) {
        new Thread() {
            @Override
            public void run() {
                try {
                    cs.createFolder("/TestFolder");
                } catch (Exception e) {
                    Log.d(TAG, "ERROR: Folder already Created");
                }
            }
        }.start();
    }

    private void updateUasImageView(String uasImageFileName) {
        if (uasImageFileName != null) {
            Log.d(TAG, "updateUasImageView: uasImageFileName=" + uasImageFileName);
            try {
                FileInputStream fis = openFileInput(uasImageFileName);
                Bitmap imageBitmap = BitmapFactory.decodeStream(fis);

                Log.d(TAG, "updateUasImageView: (" + imageBitmap.getWidth() + "," + imageBitmap.getHeight() + ")");

                ImageView imageView = (ImageView) findViewById(R.id.uas_image_view);
                imageView.setImageBitmap(imageBitmap);
            } catch (FileNotFoundException fNFE) {
                Log.e(TAG, "setUasImage: FileNotFoundException: ", fNFE);
            } catch (NullPointerException nPE) {
                Log.e(TAG, "setUasImage: NullPointerException: ", nPE);
            }
            Log.d(TAG, "updateUasImageView: image set");
        }
    }

    private class MainActivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: ");
            String action = intent.getAction();
            if (intent != null && action != null) {
                switch (action) {
                    case CommandService.ACTION_NEW_UAS_IMAGE:
                        Log.d(TAG, "onReceive: ACTION_NEW_UAS_IMAGE: ");
                        String uasImageFileName = intent.getStringExtra(CommandService.EXTRA_IMAGE_FILE_NAME);
                        updateUasImageView(uasImageFileName);
                        break;
                    case CommandService.ACTION_NEW_LOCATION:
                        Log.d(TAG, "onReceive: ACTION_NEW_LOCATION: ");
                        break;
                    default:
                        Log.d(TAG, "onReceive: default: ");
                        break;
                }
            }
        }
    }
}
