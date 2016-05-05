package com.ark.zomimagelib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ListView;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by akshay on 03/05/16.
 */
public class ImageManager {

    private static final String TAG = "ImageManager";

    private long cacheDuration;
    private SimpleDateFormat mDateFormatter;

    private LruCache<String, Bitmap> lruCacheMap; //= new HashMap<>();
    private File cacheDir;

    private ImageQueue imageQueue = new ImageQueue();

    private Thread imageLoaderThread ;

    public Context context;

    public ListView listView;

    ExecutorService executorService;


    //Constructor
    public ImageManager(Context context, ListView lv , long _cacheDuration) {

        this.context = context;
        this.listView = lv;

        cacheDuration = _cacheDuration;
        mDateFormatter = new SimpleDateFormat("EEE',' dd MMM yyyy HH:mm:ss zzz");

        imageLoaderThread = new Thread(new ImageQueueManager());

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
        int [] imgInfo = Utils.getBitmapPositionInsideImageView(imageView);

        //Check if cache map already has the imageBitmap
        Bitmap bmp = getBitmapFromMemCache(url);
        if(bmp !=null)
            imageView.setImageBitmap(bmp);
        else {
            //add image to queue
            queueImage(url, imageView, defaultDrawableId, imgInfo[2], imgInfo[3]);
            //set temporary drawable
            imageView.setImageResource(defaultDrawableId);
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
    /**
     *  For normal ImageView
     * */
    private Bitmap getBitmap(ImageRef imgRef) {
        try {
            String filename = String.valueOf(imgRef.url.hashCode());

            //get bitmap from cache dir
            File bitmapFile = new File(cacheDir, filename);
            Bitmap bitmap = null;
            if(bitmapFile.exists() && imgRef.view_width >=0 && imgRef.view_height >=0)
                bitmap = Utils.decodeBitmapFromFile(bitmapFile.getPath(),imgRef.view_width,imgRef.view_height);//BitmapFactory.decodeFile(bitmapFile.getPath());

            // Check if the bitmap is present in the cache
            if (bitmap != null) {
                // Check if it is Expired
                long currentTimeMillis = System.currentTimeMillis();
                long bitmapTimeMillis = bitmapFile.lastModified();
                if ((currentTimeMillis - bitmapTimeMillis) < cacheDuration) {
                    Log.d(TAG,"bitmap found in file");
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


    class ImageQueueManager implements Runnable {

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
                        final ImageRef imageToLoad;

                        synchronized (imageQueue.imageRefs) {
                            imageToLoad = imageQueue.imageRefs.pop();
                        }

                        final Bitmap bmp = getBitmap(imageToLoad);
                        if(bmp!=null) {
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
                        }else {
                            if(listView!=null) {
                                Rect scrollBounds = new Rect();
                                listView.getHitRect(scrollBounds);
                                if (imageToLoad.imageView.getLocalVisibleRect(scrollBounds)) {
                                    executorService.execute(new downloadThread(context, imageToLoad));
                                }
                            }else {
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
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            lruCacheMap.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return lruCacheMap.get(key);
    }








    class downloadThread implements Runnable, IntFileDownloadListener {

        private static final String TAG = "aaa";
        Context context;
        public int total_len, chunk_len, num_chunk ;

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
                Log.d(TAG, "total_len:" + total_len +"  "+ imageRef.url +"  chunk_len:" + chunk_len + "   num_chunk: " + num_chunk);

                ImageDownloader imageDownloader = new ImageDownloader(context, total_len, chunk_len, num_chunk, this);
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
                        }
                    });
                }
            }
        }
    }

}



