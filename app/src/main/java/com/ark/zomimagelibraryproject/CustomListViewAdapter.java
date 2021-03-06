package com.ark.zomimagelibraryproject;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    public CustomListViewAdapter(Context context, ListView listView){
    //    this.dataList = dataList;
        this.context = context;

        dataList = new String[10];//2,4,5,9,11,12,13,15
        dataList[0] = Constants.IMG1;
        dataList[1] = Constants.IMG3;
        dataList[2] = Constants.IMG6;
        dataList[3] = Constants.IMG7;
        dataList[4] = Constants.IMG8;
        dataList[5] = Constants.IMG1;
        dataList[6] = Constants.IMG14;
        dataList[7] = Constants.IMG6;
        dataList[8] = Constants.IMG7;
        dataList[9] = Constants.IMG8;
        imageManager = new ImageManager(context, listView, 1000000000);
    }

    @Override
    public int getCount() {
        return 100;
    }

    @Override
    public Object getItem(int position) {
        return dataList[position%10];
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

        final int  finalHeight = 300, finalWidth = 300;
       // finalHeight = zomImageView.getMeasuredHeight();
       // finalWidth = zomImageView.getMeasuredWidth();

       // Log.d(TAG,finalWidth+"  -#-  "+finalHeight);

        imageManager.displayImage(dataList[position%10], zomImageView, R.mipmap.ic_launcher);

        tv_info.setText(dataList[position%10]);

        return row;
    }
}
