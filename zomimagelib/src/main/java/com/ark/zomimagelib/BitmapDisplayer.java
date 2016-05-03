package com.ark.zomimagelib;

import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

/**
 * Created by zomguest on 03/05/16.
 */


//Used to display bitmap in the UI thread
class BitmapDisplayer implements Runnable {
    private static final String TAG = "BitmapDisplayer";
    Bitmap bitmap;
    ImageView imageView;
    int defDrawableId;

    public BitmapDisplayer(Bitmap b, ImageView i, int defaultDrawableId) {
        bitmap=b;
        imageView=i;
        defDrawableId = defaultDrawableId;
    }

    public void run() {

        Log.d(TAG,"diaplay Image");

        if(bitmap != null)
            imageView.setImageBitmap(bitmap);
        else
            imageView.setImageResource(defDrawableId);
    }
}
