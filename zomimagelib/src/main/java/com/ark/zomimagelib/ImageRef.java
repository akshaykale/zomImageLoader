package com.ark.zomimagelib;

import android.widget.ImageView;

/**
 * Created by akshaykale on 03/05/16.
 */

class ImageRef {
    public String url;
    public ImageView imageView;
    public int defDrawableId;

    public int view_height = 512,view_width = 512;

    public ImageRef(String u, ImageView i, int defaultDrawableId, int w, int h) {
        url=u;
        imageView=i;
        defDrawableId = defaultDrawableId;
        view_height = h;
        view_width = w;
    }
}
