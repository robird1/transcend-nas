package com.transcend.nas.settings;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * Created by silverhsu on 16/2/23.
 */
public class DiskInfoPagerAdapter extends PagerAdapter {
    public static final String TAG = DiskInfoPagerAdapter.class.getSimpleName();
    private List<View> mListViews;

    public DiskInfoPagerAdapter() {

    }

    public void setContentList(List<View> listViews){
        mListViews = listViews;
    }

    @Override
    public int getCount() {
        return mListViews.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = mListViews.get(position);
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }
}
