package com.transcend.nas.viewer.photo;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.transcend.nas.R;
import com.transcend.nas.management.firmware.PhotoFactory;

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
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        View view = layoutInflater.inflate(R.layout.viewer_item, container, false);
        String path = mList.get(position);
        PhotoView pv = (PhotoView) view.findViewById(R.id.viewer_image);
        pv.setDrawingCacheEnabled(false);
        pv.setOnPhotoTapListener(mOnPhotoTapListener);
        ProgressBar pb = (ProgressBar) view.findViewById(R.id.viewer_progress);
        PhotoFactory.getInstance().displayPhoto(mContext, false, path, pv, pb);
        container.addView(view);
        Log.w(TAG, "instantiateItem [" + position + "]: " + path);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        if (object instanceof View) {
            View iv = (View) object;
            ImageView img = (ImageView) iv.findViewById(R.id.viewer_image);
            if(img != null)
                ImageLoader.getInstance().cancelDisplayTask(img);
            container.removeView(iv);
        }
    }

    public void removeView(int index) {
        mList.remove(index);
        notifyDataSetChanged();
    }

    @Override
    public int getItemPosition(Object object) {
        if (mList.contains((View) object)) {
            return mList.indexOf((View) object);
        } else {
            return POSITION_NONE;
        }
    }

}
