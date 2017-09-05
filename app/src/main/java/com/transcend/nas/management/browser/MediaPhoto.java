package com.transcend.nas.management.browser;

import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.transcend.nas.R;
import com.transcend.nas.management.FileInfo;

/**
 * Created by steve_su on 2017/7/20.
 */

public class MediaPhoto extends MediaGeneral {
    private static final String TAG = MediaPhoto.class.getSimpleName();

    MediaPhoto(Context context) {
        super(context);
        mModel = StoreJetCloudData.PHOTO;
        mRequestControl = new RequestPhoto(mActivity);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        MenuInflater inflater = mActivity.getMenuInflater();

        Log.d(TAG, "[Enter] onPrepareOptionsMenu mPath: "+ mActivity.mPath);

        if ("view_all".equals(mRequestControl.getAPIName(mActivity.mPath)) ||
                "get_photo".equals(mRequestControl.getAPIName(mActivity.mPath))) {
            inflater.inflate(R.menu.option_menu_photo_file, menu);
            Log.d(TAG, "[Enter] R.menu.option_menu_photo_file");
        } else {
            inflater.inflate(R.menu.option_menu_photo_index, menu);
            Log.d(TAG, "[Enter] R.menu.option_menu_photo_index");
        }

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean optionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.option_menu_all_photo:
                viewAllPhoto();
                mModel.setViewPreference(mContext, 0);
                return true;
            case R.id.option_menu_date:
                viewByDate();
                mModel.setViewPreference(mContext, 1);
                return true;
            case R.id.option_menu_folder:
                viewByFolder();
                mModel.setViewPreference(mContext, 2);
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

    private void viewAllPhoto() {
        mRequestControl.viewAll();
    }

    private void viewByDate() {
        mRequestControl.viewByDate();
    }

    private void viewByFolder() {
        mRequestControl.viewByFolder();
    }

    @Override
    public void onPageChanged() {
        int menuPosition = mModel.getViewPreference(mContext);
        switch (menuPosition) {
            case 0:
                viewAllPhoto();
                break;
            case 1:
                viewByDate();
                break;
            case 2:
                viewByFolder();
                break;
        }

    }

    @Override
    public void lazyLoad() {
        mRequestControl.lazyLoad();
    }

}
