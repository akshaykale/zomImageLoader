package com.ark.zomimagelib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by akshay on 03/05/16.
 */
public class ImageManager implements AbsListView.OnScrollListener {

    private static final String TAG = "ImageManager";

    private long cacheDuration;
    private SimpleDateFormat mDateFormatter;

    private LruCache<String, Bitmap> lruCacheMap; //= new HashMap<>();
    public static LruCache<String, byte[]> lruChunkCacheMap; //= new HashMap<>();

    public static File cacheDir;

    private ImageQueue imageQueue = new ImageQueue();

    private Thread imageLoaderThread;

    public Context context;

    public ListView listView;

    ExecutorService executorService;

    public ScrollState scrollState;

    public int STATE_FLING;
    public boolean scrollUSED = false;

    public ImageRef imageToLoad;

    public LinkedList<ImageRef> imageRefsQueue;


    //Constructor
    public ImageManager(Context context, ListView lv, long _cacheDuration) {

        this.context = context;
        this.listView = lv;
        this.listView.setOnScrollListener(this);

        imageRefsQueue = new LinkedList<>();

        cacheDuration = _cacheDuration;
        mDateFormatter = new SimpleDateFormat("EEE',' dd MMM yyyy HH:mm:ss zzz");

        STATE_FLING = 0;

        scrollUSED = false;

        imageLoaderThread = new Thread(new ImageQueueManager());//(listView));

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;
        lruCacheMap = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };

        executorService = Executors.newFixedThreadPool(5);

        scrollState = new ScrollState(context);
        // Make background thread low priority, to avoid affecting UI performance
        imageLoaderThread.setPriority(Thread.NORM_PRIORITY - 1);


        // Find/Create directory for cache images
        String sdState = android.os.Environment.getExternalStorageState();
        if (sdState.equals(android.os.Environment.MEDIA_MOUNTED)) {
            File sdDir = android.os.Environment.getExternalStorageDirectory();
            cacheDir = new File(sdDir, "data/zomImages");
        } else {
            cacheDir = context.getCacheDir();
        }
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
    }

    public void displayImage(String url, ImageView imageView, int defaultDrawableId) {
        /** get actual bitmapsize to be displayed in the image view*/
        imageView.setImageResource(R.drawable.place_holder);

        int[] imgInfo = Utils.getBitmapPositionInsideImageView(imageView);

        //Check if cache map already has the imageBitmap
        Bitmap bmp = getBitmapFromMemCache(url);
        Log.d(TAG, "FLING : " + String.valueOf(STATE_FLING));

        if (bmp != null)
            imageView.setImageBitmap(bmp);
        else {
            //add image to queue

            //if (imageQueue.imageRefs.size() > 3)
            //    imageQueue.imageRefs.remove(3);
            //queueImage(url, imageView, defaultDrawableId, imgInfo[2], imgInfo[3]);

            //set temporary drawable
            imageView.setImageResource(defaultDrawableId);

            ImageRef imageRef = new ImageRef(url,imageView,defaultDrawableId,imgInfo[2], imgInfo[3]);
            if (imageRefsQueue.size() > 3)
                imageRefsQueue.remove(3);
            imageRefsQueue.addFirst(imageRef);

            if(!scrollUSED){
                requestDownload(imageRef);
            }

        }
    }

    private void queueImage(String url, ImageView imageView, int defaultDrawableId, int w, int h) {
        //In case the imageview is used for other images, we clear the queue of old tasks before starting
        imageQueue.Clean(imageView);
        ImageRef p = new ImageRef(url, imageView, defaultDrawableId, w, h);

        synchronized (imageQueue.imageRefs) {
            imageQueue.imageRefs.push(p);
            imageQueue.imageRefs.notifyAll();
        }
        if (imageLoaderThread.getState() == Thread.State.NEW) {
            imageLoaderThread.start();
        }
    }

    private Bitmap getBitmap(ImageRef imgRef) {
        try {
            String filename = String.valueOf(imgRef.url.hashCode());

            //get bitmap from cache dir
            File bitmapFile = new File(cacheDir, filename);
            Bitmap bitmap = null;
            if (bitmapFile.exists() && imgRef.view_width >= 0 && imgRef.view_height >= 0)
                bitmap = Utils.decodeBitmapFromFile(bitmapFile.getPath(), imgRef.view_width, imgRef.view_height);//BitmapFactory.decodeFile(bitmapFile.getPath());

            // Check if the bitmap is present in the cache
            if (bitmap != null) {
                // Check if it is Expired
                long currentTimeMillis = System.currentTimeMillis();
                long bitmapTimeMillis = bitmapFile.lastModified();
                if ((currentTimeMillis - bitmapTimeMillis) < cacheDuration) {
                    Log.d(TAG, "bitmap found in file");
                    return bitmap;
                }

                /*// Check also if it was modified on the server before downloading it
                String lastMod = openConnection.getHeaderField("Last-Modified");
                long lastModTimeMillis = mDateFormatter.parse(lastMod).getTime();

                if (lastModTimeMillis <= bitmapTimeMillis) {

                    //Discard the connection and return the cached version
                    return bitmap;
                }*/
            }
            return bitmap;

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

/*
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

        STATE_FLING = scrollState;
        Log.d(TAG, "FLING && " + STATE_FLING);
        switch (scrollState) {
            case 0:
                mBusy = false;
                int first = view.getFirstVisiblePosition();
                for (int i = first; i < (first+3); i++) {
                    requestDownload(imageToLoad);
                }
                break;
            case 1:
                mBusy = true;
                requestDownload(imageToLoad);
                break;
            case 2:
                mBusy = true;
                break;
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

    }
*/

    @Override
    public void onScrollStateChanged(AbsListView view, int _scrollState) {
        STATE_FLING = _scrollState;
        scrollUSED = true;
        Log.d(TAG,"SCROLL   --  "+_scrollState);
        switch (_scrollState){
            case 0:
                for(ImageRef imageRef : imageRefsQueue){
                    if(imageRef!=null){
                        requestDownload(imageRef);
                    }
                }
                break;
            case 1:
                for(ImageRef imageRef : imageRefsQueue){
                    if(imageRef!=null){
                        requestDownload(imageRef);
                    }
                }
                break;
            case 2:
                break;
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

    }



    public void requestDownload(final ImageRef imageToLoad) {
        final Bitmap bmp = getBitmap(imageToLoad);
        if (bmp != null) {
            addBitmapToMemoryCache(imageToLoad.url, bmp);
            Object tag = imageToLoad.imageView.getTag();

            // Make sure we have the right view - thread safety defender
            if (tag != null && ((String) tag).equals(imageToLoad.url)) {
                BitmapDisplayer bmpDisplayer =
                        new BitmapDisplayer(bmp, imageToLoad.imageView, imageToLoad.defDrawableId);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        imageToLoad.imageView.setImageBitmap(bmp);
                    }
                });
            }
        } else {
            if (listView != null && imageToLoad != null) {
                if (STATE_FLING == 2) {
                    Log.d(TAG, "STATEE FLING    DONT DOWNLOAD");
                } else {
                    Log.d(TAG, "STATEE CAN DOWNLOAD");
                    Rect scrollBounds = new Rect();
                    listView.getHitRect(scrollBounds);
                    //if (imageToLoad.imageView.getLocalVisibleRect(scrollBounds)) {
                    executorService.execute(new downloadThread(context, imageToLoad));
                    //}
                }
            } else { //single image no listview directly download it
                executorService.execute(new downloadThread(context, imageToLoad));
            }
        }
    }


    class ImageQueueManager implements Runnable{//, AbsListView.OnScrollListener {

        /*ListView listView;

        public ImageQueueManager(ListView listView) {
            this.listView = listView;
            if(listView!=null)
                this.listView.setOnScrollListener(this);
        }*/

        //public ImageRef imageToLoad;

        @Override
        public void run() {
            try {
                while (true) {
                    // Thread waits until there are images in the
                    // queue to be retrieved
                    if (imageQueue.imageRefs.size() == 0) {
                        synchronized (imageQueue.imageRefs) {
                            imageQueue.imageRefs.wait();
                        }
                    }

                    // When we have images to be loaded
                    if (imageQueue.imageRefs.size() != 0) {
                        int cState;
                        synchronized (imageQueue.imageRefs) {
                            imageToLoad = imageQueue.imageRefs.pop();
                        }

                        final Bitmap bmp = getBitmap(imageToLoad);
                        if (bmp != null) {
                            addBitmapToMemoryCache(imageToLoad.url, bmp);
                            Object tag = imageToLoad.imageView.getTag();

                            // Make sure we have the right view - thread safety defender
                            if (tag != null && ((String) tag).equals(imageToLoad.url)) {
                                BitmapDisplayer bmpDisplayer =
                                        new BitmapDisplayer(bmp, imageToLoad.imageView, imageToLoad.defDrawableId);
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        imageToLoad.imageView.setImageBitmap(bmp);
                                    }
                                });
                            }
                        } else {

                            if (listView != null) {
                                if (STATE_FLING == 2) {
                                    Log.d(TAG, "STATEE FLING    DONT DOWNLOAD");
                                } else {
                                    Log.d(TAG, "STATEE CAN DOWNLOAD");
                                    Rect scrollBounds = new Rect();
                                    listView.getHitRect(scrollBounds);
                                    //if (imageToLoad.imageView.getLocalVisibleRect(scrollBounds)) {
                                    executorService.execute(new downloadThread(context, imageToLoad));
                                    //}
                                }
                            } else { //single image no listview directly download it
                                executorService.execute(new downloadThread(context, imageToLoad));
                            }


                        }
                    }

                    if (Thread.interrupted())
                        break;
                }
            } catch (InterruptedException e) {
            }
        }


        /*@Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            STATE_FLING = scrollState;
            Log.d(TAG, "FLING && " + STATE_FLING);
            switch (scrollState) {
                case 0:
                    mBusy = false;
                    int first = view.getFirstVisiblePosition();
                    for (int i = first; i < (first+3); i++) {
                        requestDownload();
                    }
                    break;
                case 1:
                    mBusy = true;
                    requestDownload();
                    break;
                case 2:
                    mBusy = true;
                    break;
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            Log.d(TAG, "SCROLL&& " + firstVisibleItem +"    "+visibleItemCount+"      "+totalItemCount);
        }*/


    }


    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            lruCacheMap.put(key, bitmap);
        }
    }
    public Bitmap getBitmapFromMemCache(String key) {
        return lruCacheMap.get(key);
    }



    public static void addByteArrToMemCache(String key, byte[] arr) {
        if (getByteArrFromMemCache(key) == null) {
            lruChunkCacheMap.put(key, arr);
        }
    }
    public static byte[] getByteArrFromMemCache(String key) {
        return lruChunkCacheMap.get(key);
    }


    class downloadThread implements Runnable, IntFileDownloadListener {

        private static final String TAG = "aaa";
        Context context;
        public int total_len, chunk_len, num_chunk;

        ImageRef imageRef;

        public downloadThread(Context context, ImageRef imgRef) {
            this.context = context;
            this.imageRef = imgRef;
        }

        @Override
        public void run() {
            try {
                /** STEP 1 --  URL*/
                URL url = new URL(imageRef.url);
                /** STEP 2 --  Open Connection*/
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                /** STEP 3 --  get content length*/
                total_len = connection.getContentLength();
                chunk_len = LibConstants.CHUNK_LENGTH;
                if (total_len <= chunk_len) {
                    chunk_len = total_len;
                    num_chunk = 1;
                } else {
                    num_chunk = total_len / chunk_len;
                    int last_chunk = chunk_len % total_len;
                    num_chunk = (last_chunk > 0) ? num_chunk++ : num_chunk;
                }
                /** STEP 4 --  disconnect*/
                connection.disconnect();
                Log.d(TAG, "total_len:" + total_len + "  " + imageRef.url + "  chunk_len:" + chunk_len + "   num_chunk: " + num_chunk);

                ImageDownloader imageDownloader = new ImageDownloader(context, imageRef, listView, total_len, chunk_len, num_chunk, this);
                imageDownloader.execute(imageRef.url);


            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDownloadComplete(final Bitmap bitmap) {
            Log.d(TAG, "@@@@@@@@");

            if (bitmap != null) {
                String filename = String.valueOf(imageRef.url.hashCode());
                File bitmapFile = new File(cacheDir, filename);
                Utils.writeFile(bitmap, bitmapFile);

                addBitmapToMemoryCache(imageRef.url, bitmap);
                Object tag = imageRef.imageView.getTag();

                // Make sure we have the right view - thread safety defender
                if (tag != null && ((String) tag).equals(imageRef.url)) {
                    BitmapDisplayer bmpDisplayer =
                            new BitmapDisplayer(bitmap, imageRef.imageView, imageRef.defDrawableId);

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            imageRef.imageView.setImageBitmap(bitmap);
                            listView.deferNotifyDataSetChanged();
                        }
                    });
                }
            }
        }
    }

}



