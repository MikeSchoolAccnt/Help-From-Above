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

/**
 * Created by caleb on 5/9/17.
 */

public class FullscreenUasImageActivity extends AppCompatActivity {
    private static final String TAG = "FullscreenUasImageAc...";

    private BroadcastReceiver fullscreenUasImageBroadcastReceiver = new FullscreenUasImageBroadcastReceiver();
    private IntentFilter fullscreenUasImageIntentFilter = new IntentFilter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_uas_image);

        byte[] imageByteArray = getIntent().getByteArrayExtra(TempConstants.IMAGE_BYTE_ARRAY);
        setFullscreenImage(imageByteArray);

        fullscreenUasImageIntentFilter.addAction(TempConstants.NEW_UAS_IMAGE);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: ");
        super.onResume();
        registerReceiver(fullscreenUasImageBroadcastReceiver, fullscreenUasImageIntentFilter);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: ");
        super.onPause();
        unregisterReceiver(fullscreenUasImageBroadcastReceiver);
    }

    private void setFullscreenImage(byte[] imageByteArray) {
        Log.d(TAG, "setFullscreenImage: ");
        Bitmap imageBitmap = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.length);
        ImageView imageView = (ImageView) findViewById(R.id.fullscreen_uas_image_view);
        imageView.setImageBitmap(imageBitmap);
    }

    private class FullscreenUasImageBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "FullscreenUasImageBroadcastReceiver.onReceive: ");
            switch (intent.getAction()) {
                case TempConstants.NEW_UAS_IMAGE: {
                    byte[] imageByteArray = intent.getByteArrayExtra(TempConstants.IMAGE_BYTE_ARRAY);
                    setFullscreenImage(imageByteArray);
                }
            }
        }
    }
}
