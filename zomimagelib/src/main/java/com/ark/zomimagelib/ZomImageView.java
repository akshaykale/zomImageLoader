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
    }

    public void setImage(int img) {
        setImageResource(img);
    }
}
