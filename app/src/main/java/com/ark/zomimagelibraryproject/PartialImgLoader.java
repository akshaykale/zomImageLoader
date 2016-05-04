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

        imageLoader.loadImage(Constants.IMG10,imageView);

        //imageLoader.partition(imageView);

    }
}
