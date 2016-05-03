package com.ark.zomimagelib;

import android.os.AsyncTask;
import android.widget.ImageView;

import static com.ark.zomimagelib.LVState.*;

/**
 * Created by zomguest on 02/05/16.
 */
public class ImgDownloadAsync extends AsyncTask<String, String, String> {

    ImageView imageView;
    LVState lvState;

    public ImgDownloadAsync(ImageView imageView, LVState lvState) {
        this.imageView = imageView;
        this.lvState = lvState;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... params) {



        switch (lvState){
            case SCROLL_STATE_FLING:

                break;
            case SCROLL_STATE_IDLE:


                break;
        }

        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
    }
}
