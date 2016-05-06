package com.ark.zomimagelib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Created by zomguest on 02/05/16.
 */
public class ImageDownloader implements IntFileChunksDownloadListener {//, AbsListView.OnScrollListener {

    ImageRef imageToLoad;

    public Context context;

    public int THREAD_POOL_NUM = 5;

    public int[] download_status;

    ExecutorService executorService;

    public LruCache<Integer, byte[]> mCacheMap;

    int total_length, chunck_length, num_chuncks;

    IntFileDownloadListener intFileDownloadListener = null;

    private static final String TAG = "ImageDownloader";

    ListView view;

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
        for (int i = 0; i < download_status.length; i++)
            download_status[i] = 0;

        executorService = Executors.newFixedThreadPool(THREAD_POOL_NUM);
    }

    public ImageDownloader(Context con, ImageRef imageToLoad, View view, int total_len, int chunck_len, int num_chunks, IntFileDownloadListener intFileDownloadListener) {
        this.context = con;
        this.total_length = total_len;
        this.chunck_length = chunck_len;
        this.num_chuncks = num_chunks;

        this.imageToLoad = imageToLoad;

        this.intFileDownloadListener = intFileDownloadListener;

        this.view = (ListView) view;
        // ((ListView) view).setOnScrollListener(this);

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
        for (int i = 0; i < download_status.length; i++)
            download_status[i] = 0;

        executorService = Executors.newFixedThreadPool(THREAD_POOL_NUM);
    }

    public void execute(String img_url) {
        int start = 0, end = chunck_length - 1;
        for (int i = 0; i < num_chuncks; i++) {

            if (imageToLoad.imageView.getVisibility() == View.VISIBLE) {
                start = i * chunck_length;
                end = ((i + 1) >= num_chuncks) ? total_length - 1 : ((i + 1) * chunck_length) - 1;
                //Log.d(TAG,"start:  "+start+"      end: "+end);

                byte[] data = getByteArr(imageToLoad, i);
                if (data != null) {
                    downloadComplete(i, data);
                } else {
                    executorService.execute(new ImagePartDownloadRunnable(img_url,
                            start,
                            end,
                            i,  //current chunk
                            this));
                }
            } else {
                Log.d(TAG, "INVISIBLE");
            }
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

        addDataToMemoryCache(current_chunk, data);
        download_status[current_chunk] = 1;
        Log.d(TAG, "DOWNLOAD CHUNK  -->> " + current_chunk);
        int status = 1;
        for (int i = 0; i < download_status.length; i++) {
            if (download_status[i] == 0) {
                status = 0;
                break;
            }
        }

        if (status == 1) {
            downloadCompleted();
        } else {
            Rect scrollBounds = new Rect();
            view.getHitRect(scrollBounds);
            if (imageToLoad.imageView.getLocalVisibleRect(scrollBounds)) {
                //continue downloading chunks
            } else if (imageToLoad.imageView.getVisibility() != View.VISIBLE) {
                try {
                    if (executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                        System.out.println("task completed");
                    } else {
                        System.out.println("Forcing shutdown...");
                        mCacheMap.evictAll();
                        executorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    @Override
    public void downloadFailed(int current_chunk, ImagePartDownloadRunnable runnable) {

        Log.d(TAG, "DOWNLOAD FAILED  -->> " + current_chunk);
        download_status[current_chunk] = 0;
        //try again
        executorService.execute(new ImagePartDownloadRunnable(runnable.img_url,
                runnable.start,
                runnable.end,
                runnable.current_chunk,
                this));
        download_status[current_chunk] = 0;
    }


    private void downloadCompleted() {

        int index = 0;
        for (int i = 0; i < num_chuncks; i++) {
            byte[] x = mCacheMap.get(i);
            for (int j = 0; j < x.length; j++) {
                index++;
            }
        }

        byte[] finalData = new byte[index];
        index = 0;
        for (int i = 0; i < num_chuncks; i++) {

            byte[] x = mCacheMap.get(i);
            for (int j = 0; j < x.length; j++) {
                finalData[index] = x[j];
                //Log.d(TAG,"Reading data from chunk "+ i+"   data: "+x[j]);
                index++;
            }
        }
        final Bitmap bitmap = Utils.decodeBitmapFromByteArray(finalData, finalData.length, 200, 200);//BitmapFactory.decodeByteArray(finalData , 0, finalData .length);

        if (intFileDownloadListener != null)
            intFileDownloadListener.onDownloadComplete(bitmap);
        if (bitmap != null)
            Log.d(TAG, finalData.length + "     DOWNLOAD COMPLETE    ");
    }


/*    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        switch (scrollState){
            case 1://fling
                *//*try {
                    if (executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                        System.out.println("task completed");
                    } else {
                        System.out.println("Forcing shutdown...");
                        executorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }*//*
                break;
            default:
        }

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
    }*/


    private byte[] getByteArr(ImageRef imgRef, int chunk_num) {
        try {
            String filename = String.valueOf(imgRef.url.hashCode());
            filename += "_" + chunk_num;
            File dataFile = new File(ImageManager.cacheDir, filename);
            int size = (int) dataFile.length();
            byte[] bytes = new byte[size];
            if (dataFile.exists()) {
                //get the data to bytes
                BufferedInputStream buf = new BufferedInputStream(new FileInputStream(dataFile));
                buf.read(bytes, 0, bytes.length);
                buf.close();

            }
            //bitmap = Utils.decodeBitmapFromFile(dataFile.getPath(), imgRef.view_width, imgRef.view_height);//BitmapFactory.decodeFile(bitmapFile.getPath());

            // Check if the bitmap is present in the cache
            if (bytes != null && bytes.length != 0) {
                Log.d(TAG, "chunk found in file  " + filename);
                return bytes;
            }
            return null;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}