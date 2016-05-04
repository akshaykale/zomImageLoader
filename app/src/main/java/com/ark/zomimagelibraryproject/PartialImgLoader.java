package com.ark.zomimagelibraryproject;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.akshaykale.partialimageloading.ProgressingImageLoader;


/**
 * Created by zomguest on 03/05/16.
 */
public class PartialImgLoader extends AppCompatActivity{

     ImageView imageView;

    ProgressingImageLoader imageLoader;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imageView = new ImageView(getApplicationContext());

        setContentView(imageView);

        imageLoader = new ProgressingImageLoader(getApplicationContext());

        //imageView.setImageResource(R.drawable.simple);

        imageLoader.loadImage(Constants.IMG14,imageView);//2,4,5,9,11,12,13,15

        //imageLoader.partition(imageView);

    }
}
