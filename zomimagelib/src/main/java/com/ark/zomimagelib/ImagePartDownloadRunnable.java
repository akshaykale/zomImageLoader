package com.ark.zomimagelib;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by zomguest on 04/05/16.
 */


class ImagePartDownloadRunnable implements Runnable {


    private static final String TAG = "ImagePartDownload";

    public ImagePartDownloadRunnable(String img_url, int start, int end, int current_chunk, IntFileChunksDownloadListener fileChunksDownloadListener) {
        this.img_url = img_url;
        this.start = start;
        this.end = end;
        this.current_chunk = current_chunk;
        this.fileChunksDownloadListener = fileChunksDownloadListener;
    }

    IntFileChunksDownloadListener fileChunksDownloadListener;

    String img_url;
    int start,end;

    int current_chunk;

    BufferedInputStream in ;
    HttpURLConnection connection;
    @Override
    public void run() {

        try {
            /** STEP 1 --  URL*/
            URL url = new URL(img_url);
            /** STEP 2 --  Open Connection*/
            connection = (HttpURLConnection) url.openConnection();

            Log.d(TAG,"Start: "+start+"     end: "+end);
            connection.setRequestProperty("Range", "bytes=" + start+ "-" + end);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            in = new BufferedInputStream(connection.getInputStream());

            byte[] part = Utils.getBytes(in);

            if(part !=null){
                fileChunksDownloadListener.downloadComplete(current_chunk,part);
            }else {
                fileChunksDownloadListener.downloadFailed(current_chunk, this);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            fileChunksDownloadListener.downloadFailed(current_chunk, this);
        } finally {
            try {
                if(in!=null)
                    in.close();
                if(connection!=null)
                    connection.disconnect();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
