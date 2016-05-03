package com.ark.zomimagelib;

import android.widget.ImageView;

import java.util.Stack;

/**
 * Created by akshaykale on 03/05/16.
 */


//stores list of images to download
class ImageQueue {
    public Stack<ImageRef> imageRefs =
            new Stack<>();

    //removes all instances of this ImageView
    public void Clean(ImageView view) {

        for(int i = 0 ;i < imageRefs.size();) {
            if(imageRefs.get(i).imageView == view)
                imageRefs.remove(i);
            else ++i;
        }
    }
}