package com.akshaykale.partialimageloading;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import com.ark.zomimagelib.ImageDownloader;
import com.ark.zomimagelib.IntFileDownloadListener;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;



/**
 * Created by akshaykale on 03/05/16.
 */
public class ProgressingImageLoader {

    private static final String TAG = "ProgressingImageLoader";
    Context context;

    public int rows, cols;

    public static int CHUNK_SIZE = 80000;

    public ProgressingImageLoader(Context con) {
        this.context = con;
    }


    public void loadImage(String url, ImageView iv) {
        downloadImg(url, iv);
        //partition();
    }

    public void partition(ImageView image) {

        int imgHeight, imgWidth;

        //For the number of rows and columns of the grid to be displayed

        //For height and width of the small image chunks
        int chunkHeight, chunkWidth;

        //To store all the small image chunks in bitmap format in this list
        int chunkNumbers = 16;
        ArrayList<Bitmap> chunkedImages = new ArrayList<Bitmap>(chunkNumbers);

        //Getting the scaled bitmap of the source image
        BitmapDrawable drawable = (BitmapDrawable) image.getDrawable();
        Bitmap bitmap = drawable.getBitmap();
        imgHeight = bitmap.getHeight();
        imgWidth = bitmap.getWidth();
    /*ByteArrayOutputStream stream = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);*/
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);

        rows = cols = (int) Math.sqrt(chunkNumbers);
        chunkHeight = bitmap.getHeight() / rows;
        chunkWidth = bitmap.getWidth() / cols;
    /*chunkHeight = 300/rows;
    chunkWidth = 300/cols;*/

        //xCoord and yCoord are the pixel positions of the image chunks
        int yCoord = 0;
        for (int x = 0; x < rows; x++) {
            int xCoord = 0;
            for (int y = 0; y < cols; y++) {
                chunkedImages.add(Bitmap.createBitmap(scaledBitmap, xCoord, yCoord, chunkWidth, chunkHeight));
                xCoord += chunkWidth;
            }
            yCoord += chunkHeight;
        }

        chunkedImages.remove(0);
        Bitmap bb = Bitmap.createBitmap(chunkWidth, chunkHeight, Bitmap.Config.ARGB_4444);
        chunkedImages.add(bb);

        chunkedImages.remove(0);
        Bitmap bb1 = Bitmap.createBitmap(chunkWidth, chunkHeight, Bitmap.Config.ARGB_4444);
        chunkedImages.add(bb1);

        mergeImage(image, chunkedImages, imgWidth, imgHeight);

    }

    void mergeImage(ImageView imageView, ArrayList<Bitmap> imageChunks, int width, int height) {

        // create a bitmap of a size which can hold the complete image after merging
        Bitmap bitmap = Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_4444);

        // create a canvas for drawing all those small images
        Canvas canvas = new Canvas(bitmap);
        int count = 0;
        Bitmap currentChunk;//= imageChunks.get(0);

        //Array of previous row chunks bottom y coordinates
        int[] yCoordinates = new int[cols];
        Arrays.fill(yCoordinates, 0);

        for (int y = 0; y < rows; ++y) {
            int xCoord = 0;
            for (int x = 0; x < cols; ++x) {
                currentChunk = imageChunks.get(count);
                if (currentChunk == null)
                    currentChunk.eraseColor(Color.GRAY);
                canvas.drawBitmap(currentChunk, xCoord, yCoordinates[x], null);
                xCoord += currentChunk.getWidth();
                yCoordinates[x] += currentChunk.getHeight();
                count++;
            }
        }

    /*
     * The result image is shown in a new Activity
     */


        imageView.setImageBitmap(bitmap);
    }


    private void downloadImg(String str_url, ImageView iv) {
        Thread t1 = new Thread(new downloadThread(context, str_url,iv));
        t1.start();
    }
}

class downloadThread implements Runnable, IntFileDownloadListener {

    private static final String TAG = "aaa";
    String str_url;

    Context context;

    public int total_len, chunk_len, num_chunk = 36;

    ImageView iv;

    public downloadThread(Context context, String url,ImageView iv) {
        this.context = context;
        this.str_url = url;
        this.iv = iv;
    }

    @Override
    public void run() {
        try {
            /** STEP 1 --  URL*/
            URL url = new URL(str_url);
            /** STEP 2 --  Open Connection*/
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            /** STEP 3 --  get content length*/
            total_len = connection.getContentLength();
            chunk_len = ProgressingImageLoader.CHUNK_SIZE;
            if(total_len<=chunk_len){
                chunk_len = total_len;
                num_chunk = 1;
            }else {
                num_chunk = total_len / chunk_len;
                int last_chunk = chunk_len % total_len;
                num_chunk = (last_chunk > 0) ? num_chunk++ : num_chunk;
            }
            /** STEP 4 --  disconnect*/
            connection.disconnect();
            Log.d(TAG, "total_len:" + total_len + "  chunk_len:" + chunk_len + "   num_chunk: " + num_chunk);

            ImageDownloader imageDownloader = new ImageDownloader(context, total_len, chunk_len, num_chunk, this);
            imageDownloader.execute(str_url);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDownloadComplete(final Bitmap bitmap) {
        Log.d(TAG, "@@@@@@@@");
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                iv.setImageBitmap(bitmap);
            }
        });
    }
}

