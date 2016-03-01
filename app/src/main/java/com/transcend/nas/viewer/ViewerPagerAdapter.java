package com.transcend.nas.viewer;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher.OnPhotoTapListener;

/**
 * Created by silverhsu on 16/2/23.
 */
public class ViewerPagerAdapter extends PagerAdapter {

    public static final String TAG = ViewerPagerAdapter.class.getSimpleName();

    private Context mContext;
    private ArrayList<String> mList;

    private OnPhotoTapListener mOnPhotoTapListener;

    private static DisplayImageOptions mOptions;
    static {
        mOptions = new DisplayImageOptions.Builder()
                //.showImageOnLoading(R.drawable.ic_image_gray_24dp)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();
    }

    public ViewerPagerAdapter(Context context) {
        mContext = context;
        mList = new ArrayList<String>();
    }

    public void setContent(ArrayList<String> list) {
        mList = list;
    }

    public void setOnPhotoTapListener(OnPhotoTapListener listener) {
        mOnPhotoTapListener = listener;
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        String path = mList.get(position);
        PhotoView pv = new PhotoView(mContext);
        pv.setDrawingCacheEnabled(false);
        pv.setOnPhotoTapListener(mOnPhotoTapListener);
        ImageLoader.getInstance().displayImage(path, pv, mOptions);
        container.addView(pv);
        Log.w(TAG, "instantiateItem [" + position + "]: " + path);
        return pv;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        if (object instanceof ImageView) {
            ImageView iv = (ImageView) object;
            ImageLoader.getInstance().cancelDisplayTask(iv);
            container.removeView(iv);
            Log.w(TAG, "destroyItem [" + position + "]: " + mList.get(position));
        }
    }

}
