package com.ark.zomimagelib;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;

/**
 * Created by akshaykale on 02/05/16.
 */


public class ZomImageView extends ImageView {

    private static final String TAG = "ZomImageView";
    Context mContext;

    int height = 300, width = 300;

    public ZomImageView(Context context) {
        super(context);
        mContext = context;
    }

    public ZomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public ZomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
       // setImageResource(R.drawable.simple);
    }

    public void setImage(int img) {
        setImageResource(img);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        Log.d(TAG, "onMeasureImageView");

        // Get image matrix values and place them in an array
        float[] f = new float[9];
        getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = getDrawable();
        if (d != null) {
            final int origW = d.getIntrinsicWidth();
            final int origH = d.getIntrinsicHeight();

            // Calculate the actual dimensions
            final int actW = Math.round(origW * scaleX);
            final int actH = Math.round(origH * scaleY);
            Log.e("DBG", "[" + origW + "," + origH + "] -> [" + actW + "," + actH + "] & scales: x=" + scaleX + " y=" + scaleY);
            height = actH;
            width = actW;
        }

    }
}
