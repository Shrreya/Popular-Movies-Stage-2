package com.shrreyabhatachaarya.popularmovies2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class ImageAdapter extends BaseAdapter {
    private Context mContext;
    private ArrayList<String> mPosters;
    private String[] poster_paths;
    private ArrayList<byte[]> mFavPosters;
    private boolean fav;

    String base_url = "http://image.tmdb.org/t/p/w185";
    String LOG_TAG = "ImageAdapter";

    public ImageAdapter(Context c, ArrayList<String> posters) {
        mContext = c;
        mPosters = posters;
        try {
            poster_paths = new String[mPosters.size()];
            for (int i = 0; i < mPosters.size(); i++) {
                poster_paths[i] = base_url + mPosters.get(i);
            }
        } catch (NullPointerException e) {
            Log.e(LOG_TAG, "Error", e);
        }
        fav = false;
    }

    public ImageAdapter(Context c, ArrayList<byte[]> fav_posters, String sort_type) {
        mContext = c;
        mFavPosters = fav_posters;
        if(sort_type.equals("favorites"))
            fav = true;
    }

    @Override
    public int getCount() {
        if(fav)
            return mFavPosters.size();
        else
            return poster_paths.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(370, 556));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }

        if(fav) {
            Bitmap bmp = BitmapFactory.decodeByteArray(mFavPosters.get(position), 0, mFavPosters.get(position).length);
            imageView.setImageBitmap(bmp);
        }
        else {
            Picasso.with(mContext).load(poster_paths[position]).into(imageView);
    }
        return imageView;
    }
}
