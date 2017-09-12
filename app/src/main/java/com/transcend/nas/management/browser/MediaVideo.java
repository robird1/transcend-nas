package com.transcend.nas.management.browser;

import android.content.Context;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.transcend.nas.R;

/**
 * Created by steve_su on 2017/7/20.
 */

public class MediaVideo extends MediaGeneral {

    MediaVideo(Context context) {
        super(context);
        mModel = StoreJetCloudData.VIDEO;
        mRequestControl = new RequestVideo(mActivity);

    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        MenuInflater inflater = mActivity.getMenuInflater();

        if ("view_all".equals(mRequestControl.getAPIName(mActivity.mPath)) ||
                "get_video".equals(mRequestControl.getAPIName(mActivity.mPath))) {
            inflater.inflate(R.menu.option_menu_video_file, menu);
        } else {
            inflater.inflate(R.menu.option_menu_video_index, menu);
        }

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean optionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.option_menu_all_videos:
                mModel.setListSize(0);
                viewAllVideo();
                mModel.setViewPreference(mContext, 0);
                return true;
            case R.id.option_menu_folder:
                viewByFolder();
                mModel.setViewPreference(mContext, 1);
                return true;
            case R.id.option_menu_select:
                doSelect();
                return true;
            case R.id.option_menu_select_all:
                doSelectAll();
                return true;
            case R.id.option_menu_refresh:
                mRequestControl.refresh(true);
                return true;
        }
        return false;
    }

    @Override
    public void onPageChanged() {
        int menuPosition = mModel.getViewPreference(mContext);
        switch (menuPosition) {
            case 0:
                viewAllVideo();
                break;
            case 1:
                viewByFolder();
                break;
        }

    }

    @Override
    public void lazyLoad() {
        mRequestControl.lazyLoad();
    }

    private void viewAllVideo() {
        mRequestControl.viewAll();
    }

    private void viewByFolder() {
        mRequestControl.viewByFolder();
    }

}
