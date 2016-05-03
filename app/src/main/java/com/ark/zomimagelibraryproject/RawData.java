package com.ark.zomimagelibraryproject;

/**
 * Created by zomguest on 02/05/16.
 */
public class RawData {

    String url;
    String info;

    public RawData(String url){
        info = "Default Info";
    }

    public void URL(String u){
        this.url = u;
    }
    public String URL(){
        return this.url;
    }
}
