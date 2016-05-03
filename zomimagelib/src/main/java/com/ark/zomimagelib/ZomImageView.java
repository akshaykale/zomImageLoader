package com.ark.zomimagelib;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;

/**
 * Created by akshaykale on 02/05/16.
 */

enum LVState {
    SCROLL_STATE_IDLE,
    SCROLL_STATE_FLING,
    SCROLL_STATE_TOUCH_SCROLL
}

public class ZomImageView extends ImageView implements AbsListView.OnScrollListener {

    private static final String TAG = "ZomImageView";
    private Context mContext;

    public String url;

    ListView listView;

    public static final int SCROLL_STATE_IDLE = 0,
            SCROLL_STATE_FLING = 2,
            SCROLL_STATE_TOUCH_SCROLL = 1;


    /*public static void init(ListView lv) {
        listView = lv;
    }
*/
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


    /**
     * Used by developers for passing various parameters
     * <p/>
     * 1> URL
     */
    public void setImage(int img) {

        setImageResource(img);

    }


    public void setImage(ViewGroup view, String url) {
        this.url = url;

        listView = (ListView) view;

        listView.setOnScrollListener(this);

    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

        Log.d(TAG, "STATE : " + scrollState);

        switch (scrollState) {
            case SCROLL_STATE_FLING:
                //Ignore
                break;
            case SCROLL_STATE_IDLE:
                ImgDownloadAsync imgDownloadAsync = new ImgDownloadAsync(this, LVState.SCROLL_STATE_IDLE);
                imgDownloadAsync.execute(url);
                break;
            case SCROLL_STATE_TOUCH_SCROLL:
                ImgDownloadAsync imgDownloadAsync_ = new ImgDownloadAsync(this, LVState.SCROLL_STATE_TOUCH_SCROLL);
                imgDownloadAsync_.execute(url);
                break;

            default:

        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

    }
}
