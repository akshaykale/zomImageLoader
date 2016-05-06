package com.ark.zomimagelibraryproject;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.ark.zomimagelib.ImageManager;
import com.ark.zomimagelib.ZomImageView;


/**
 * Created by zomguest on 02/05/16.
 */
public class CustomListViewAdapter extends BaseAdapter {

    private static final String TAG = "CustomListViewAdapter";
    String[] dataList;
    Context context;

    ImageManager imageManager;

    int state = 0;

    public CustomListViewAdapter(Context context, ListView listView){
    //    this.dataList = dataList;
        this.context = context;

        dataList = new String[100];//2,4,5,9,11,12,13,15

        for(int i=0;i<100;i++){
            dataList[i] = "http://dummyimage.com/800x701/000/fff&text=aa"+i+".jpg";
        }

        imageManager = new ImageManager(context, listView, 1000000000);
    }

    @Override
    public int getCount() {
        return 100;
    }

    @Override
    public Object getItem(int position) {
        return dataList[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = inflater.inflate(R.layout.custom_listview_row, parent,false);

        final ZomImageView zomImageView = (ZomImageView) row.findViewById(R.id.ziv_row_image);
        TextView tv_info = (TextView) row.findViewById(R.id.tv_row_info);

        Log.d(TAG,"STATEE ##@@ getView "+ state);

        if (state != 2)
            imageManager.displayImage(dataList[position], zomImageView, R.mipmap.ic_launcher);

        tv_info.setText(dataList[position]);

        return row;
    }
}
