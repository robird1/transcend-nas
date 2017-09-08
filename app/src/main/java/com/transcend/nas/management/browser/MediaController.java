package com.transcend.nas.management.browser;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;

import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.browser_framework.Browser;
import com.transcend.nas.management.browser_framework.BrowserData;

/**
 * Created by steve_su on 2017/7/20.
 */

public class MediaController {
    private MediaGeneral mControl;
    private Context mContext;

    public MediaController(Context context, int position) {
        mContext = context;
        mControl = getInstance(position);
    }

    public boolean createOptionsMenu(Menu menu) {
        return mControl.createOptionsMenu(menu);
    }

    public boolean optionsItemSelected(MenuItem item) {
        return mControl.optionsItemSelected(item);
    }

    public void onPrepareOptionsMenu(Menu menu) {
        mControl.onPrepareOptionsMenu(menu);
    }

    public Browser.LayoutType onViewAllLayout() {
        return mControl.onViewAllLayout();
    }

    public void onRecyclerItemClick(FileInfo fileInfo) {
        mControl.onRecyclerItemClick(fileInfo);
    }

    public void onBackPressed() {
        mControl.onBackPressed();
    }

    public void refresh(boolean showProgress) {
        mControl.refresh(showProgress);
    }

    public void onPageChanged() {
        mControl.onPageChanged();
    }

    public void onCreateActionMode(Menu menu) {
        mControl.onCreateActionMode(menu);
    }

    public void lazyLoad() {
        mControl.lazyLoad();
    }

    private MediaGeneral getInstance(int position) {
        if (position == BrowserData.ALL.getTabPosition()) {
            return new MediaAll(mContext);
        } else if (position == BrowserData.PHOTO.getTabPosition()) {
            return new MediaPhoto(mContext);
        } else if (position == BrowserData.MUSIC.getTabPosition()) {
            return new MediaMusic(mContext);
        } else if (position == BrowserData.VIDEO.getTabPosition()) {
            return new MediaVideo(mContext);
        }
        return new MediaAll(mContext);
    }
}
