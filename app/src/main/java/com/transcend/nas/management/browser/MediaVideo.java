package com.transcend.nas.management.browser;

import android.content.Context;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.transcend.nas.R;
import com.transcend.nas.management.FileInfo;

/**
 * Created by steve_su on 2017/7/20.
 */

public class MediaVideo extends MediaGeneral {
    private static final String TAG = MediaVideo.class.getSimpleName();
//    protected RequestVideo mRequestControl;

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

//    @Override
//    public void onRecyclerItemClick(FileInfo fileInfo) {
//        mRequestControl.onRecyclerItemClick(fileInfo);
//    }
//
//    @Override
//    public void onBackPressed() {
//        if (!mSearchView.isIconified()) {
//            mRequestControl.onBackPressed();
//        }
//    }
//
//    @Override
//    public void refresh(boolean showProgress) {
//        mRequestControl.refresh(showProgress);
//    }

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
