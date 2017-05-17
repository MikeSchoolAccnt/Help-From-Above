package com.helpfromabove.helpfromabove;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by caleb on 5/9/17.
 */

public class FullscreenUasImageActivity extends AppCompatActivity {
    private static final String TAG = "FullscreenUasImageAc...";
    private FullscreenUasImageBroadcastReceiver fUIBR = new FullscreenUasImageBroadcastReceiver();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_uas_image);
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

        registerReceiver(fUIBR, new IntentFilter(CommandService.ACTION_NEW_UAS_IMAGE));
        sendBroadcast(new Intent(CommandService.REQUEST_UAS_IMAGE));
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: ");
        super.onPause();
        unregisterReceiver(fUIBR);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop: ");
        super.onStop();
    }

    private void updateFullscreenUasImageView(String uasImageFileName) {
        if (uasImageFileName != null) {
            try {
                FileInputStream fis = openFileInput(uasImageFileName);
                FileDescriptor fD = fis.getFD();
                Bitmap imageBitmap = BitmapFactory.decodeFileDescriptor(fD);
                ImageView imageView = (ImageView) findViewById(R.id.fullscreen_uas_image_view);
                imageView.setImageBitmap(imageBitmap);
            } catch (FileNotFoundException fNFE) {
                Log.e(TAG, "setUasImage: FileNotFoundException: ", fNFE);
            } catch (IOException iOE) {
                Log.e(TAG, "setUasImage: IOException: ", iOE);
            } catch (NullPointerException nPE) {
                Log.e(TAG, "setUasImage: NullPointerException: ", nPE);
            }
        }
    }

    private class FullscreenUasImageBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: ");
            String action = intent.getAction();
            if (intent != null && action != null) {
                switch (action) {
                    case CommandService.ACTION_NEW_UAS_IMAGE:
                        Log.d(TAG, "onReceive: ACTION_NEW_UAS_IMAGE: ");
                        String imageFileName = intent.getStringExtra(CommandService.EXTRA_IMAGE_FILE_NAME);
                        updateFullscreenUasImageView(imageFileName);
                        break;
                }
            }
        }
    }
}
