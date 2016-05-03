package com.ark.zomimagelib;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ListView;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.HashMap;

/**
 * Created by zomguest on 03/05/16.
 */
public class ImageManager {


    private static final String TAG = "ImageManager";

    private long cacheDuration;
    private SimpleDateFormat mDateFormatter;

    private HashMap<String, SoftReference<Bitmap>> imageMap = new HashMap<>();

    private File cacheDir;
    private ImageQueue imageQueue = new ImageQueue();
    private Thread imageLoaderThread = new Thread(new ImageQueueManager());

    public Context context;

    public ListView listView;

    public ImageManager(Context context, ListView lv , long _cacheDuration) {

        this.context = context;
        this.listView = lv;

        cacheDuration = _cacheDuration;
        mDateFormatter = new SimpleDateFormat("EEE',' dd MMM yyyy HH:mm:ss zzz");

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

    public void displayImage(String url, ImageView imageView, int defaultDrawableId, int w, int h) {

        //Check if hashmap already has the imageBitmap
        if (imageMap.containsKey(url)) {
            imageView.setImageBitmap(imageMap.get(url).get());
            listView.deferNotifyDataSetChanged();

        } else {
            //add image to queue
            queueImage(url, imageView, defaultDrawableId, w, h);
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
        //thread start
        if (imageLoaderThread.getState() == Thread.State.NEW) {
            imageLoaderThread.start();
        }
    }

    private Bitmap getBitmap(ImageRef imgRef) {
        try {
            String filename = String.valueOf(imgRef.url.hashCode());

            //get bitmap from cache dir
            File bitmapFile = new File(cacheDir, filename);
            Bitmap bitmap = Utils.decodeBitmapFromFile(bitmapFile.getPath(),imgRef.view_width,imgRef.view_height);//BitmapFactory.decodeFile(bitmapFile.getPath());

            long currentTimeMillis = System.currentTimeMillis();

            // Check if the bitmap is present in the cache
            if (bitmap != null) {
                // Check if it is Expired
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

            // Need to download bitmap

            /**
             * parallel image download
             * */

            bitmap = Utils.decodeBitmapFromInputStream(imgRef.url,200,200);
            // save bitmap to cache for later

            writeFile(bitmap, bitmapFile);

            return bitmap;

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private void writeFile(Bitmap bmp, File f) {
        FileOutputStream out = null;

        try {
            out = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.PNG, 80, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) out.close();
            } catch (Exception ex) {
            }
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
                        ImageRef imageToLoad;

                        synchronized (imageQueue.imageRefs) {
                            imageToLoad = imageQueue.imageRefs.pop();
                        }

                        Bitmap bmp = getBitmap(imageToLoad);
                        imageMap.put(imageToLoad.url, new SoftReference<Bitmap>(bmp));
                        Object tag = imageToLoad.imageView.getTag();

                        // Make sure we have the right view - thread safety defender
                        if (tag != null && ((String) tag).equals(imageToLoad.url)) {
                            BitmapDisplayer bmpDisplayer =
                                    new BitmapDisplayer(bmp, imageToLoad.imageView, imageToLoad.defDrawableId);

                            Activity a =
                                    (Activity) imageToLoad.imageView.getContext();

                            a.runOnUiThread(bmpDisplayer);
                        }
                    }

                    if (Thread.interrupted())
                        break;
                }
            } catch (InterruptedException e) {
            }
        }
    }
}
