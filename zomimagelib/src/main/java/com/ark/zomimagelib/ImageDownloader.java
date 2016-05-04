package com.ark.zomimagelib;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



/**
 * Created by zomguest on 02/05/16.
 */
public class ImageDownloader implements DownloadListener{

    ImageView imageView;

    public Context context;

    public int THREAD_POOL_NUM = 10;

    public int[] download_status;

    ExecutorService executorService;

    public LruCache<Integer, byte[]> mCacheMap;

    int total_length, chunck_length, num_chuncks;

    IntFileDownloadListener intFileDownloadListener = null;

    private static final String TAG = "ImageDownloader";

    public ImageDownloader(Context con, int total_len, int chunck_len, int num_chunks) {
        this.context = con;
        this.total_length = total_len;
        this.chunck_length = chunck_len;
        this.num_chuncks = num_chunks;

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;
        mCacheMap = new LruCache<Integer, byte[]>(cacheSize) {
            @Override
            protected int sizeOf(Integer key, byte[] value) {
                return value.length / 1024;
            }
        };

        download_status = new int[num_chuncks];
        for(int i=0;i<download_status.length;i++)
            download_status[i] = 0;

        executorService = Executors.newFixedThreadPool(THREAD_POOL_NUM);
    }

    public ImageDownloader(Context con, int total_len, int chunck_len, int num_chunks, IntFileDownloadListener intFileDownloadListener) {
        this.context = con;
        this.total_length = total_len;
        this.chunck_length = chunck_len;
        this.num_chuncks = num_chunks;

        this.imageView = imageView;

        this.intFileDownloadListener = intFileDownloadListener;

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;
        mCacheMap = new LruCache<Integer, byte[]>(cacheSize) {
            @Override
            protected int sizeOf(Integer key, byte[] value) {
                return value.length / 1024;
            }
        };

        download_status = new int[num_chuncks];
        for(int i=0;i<download_status.length;i++)
            download_status[i] = 0;

        executorService = Executors.newFixedThreadPool(THREAD_POOL_NUM);
    }

    public void execute(String img_url) {
        int start = 0, end = chunck_length-1;
        for (int i = 0; i < num_chuncks; i++) {
            start = i * chunck_length;

            end = ((i + 1) >= num_chuncks) ? total_length-1 : ((i+1)*chunck_length)-1;

            Log.d(TAG,"start:  "+start+"      end: "+end);
            executorService.execute(new ImagePartDownloadRunnable(img_url,
                    start,
                    end,
                    i,  //current chunk
                    this));
        }
    }

    public void addDataToMemoryCache(int key, byte[] data) {
        if (getDataFromMemCache(key) == null) {
            mCacheMap.put(key, data);
        }
    }
    public byte[] getDataFromMemCache(int key) {
        return mCacheMap.get(new Integer(key));
    }


    @Override
    public void downloadComplete(int current_chunk, byte[] data) {

        addDataToMemoryCache(current_chunk,data);
        download_status[current_chunk] = 1;
        Log.d(TAG,"DOWNLOAD CHUNK  -->> "+current_chunk);
        int status = 1;
        for(int i=0;i<download_status.length;i++){
            if(download_status[i] == 0){
                status = 0;
                break;
            }
        }
        if(status == 1){
            downloadCompleted();
        }
    }

    @Override
    public void downloadFailed(int current_chunk, ImagePartDownloadRunnable runnable) {

        Log.d(TAG,"DOWNLOAD FAILED  -->> "+current_chunk);
        download_status[current_chunk ] = 0;
        //try again
        executorService.execute(new ImagePartDownloadRunnable(runnable.img_url,
                                                                runnable.start,
                                                                runnable.end,
                                                                runnable.current_chunk,
                                                                this));
        download_status[current_chunk ] = 0;
    }



    private void downloadCompleted(){

        int index = 0;
        for(int i=0;i<num_chuncks;i++){
            byte[] x = mCacheMap.get(i);
            for(int j=0;j<x.length;j++){
                index++;
            }
        }

        byte[] finalData = new byte[index];
        index = 0;
        for(int i=0;i<num_chuncks;i++){

            byte[] x = mCacheMap.get(i);
            for(int j=0;j<x.length;j++){
                finalData[index] = x[j];
                //Log.d(TAG,"Reading data from chunk "+ i+"   data: "+x[j]);
                index++;
            }
        }
        final Bitmap bitmap = Utils.decodeBitmapFromByteArray(finalData,finalData.length,200,200);//BitmapFactory.decodeByteArray(finalData , 0, finalData .length);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();



        if(intFileDownloadListener!=null)
            intFileDownloadListener.DownloadComplete(bitmap);
        if (bitmap!=null)
        Log.d(TAG,finalData.length+"     DOWNLOAD COMPLETE    "+byteArray.length);
    }
}