package com.ark.zomimagelib;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by akshay on 05/05/16.
 */

 class ScrollState{
    static SharedPreferences sp;
    static SharedPreferences.Editor ed ;
    public ScrollState(Context context){
        sp = context.getSharedPreferences("zozozozozozozoz",Context.MODE_PRIVATE);
        ed = sp.edit();
    }

    public static int State(){
        return sp.getInt("state",0);
    }

    public static void State(int state){
        ed.putInt("state", state);
    }
}