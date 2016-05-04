package com.ark.zomimagelib;

/**
 * Created by zomguest on 04/05/16.
 */


public interface DownloadListener {

    void downloadComplete(int current_chunk, byte[] data);

    void downloadFailed (int current_chunk, ImagePartDownloadRunnable runnable);

}