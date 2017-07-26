package com.transcend.nas.management.browser;

import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.transcend.nas.R;

/**
 * Created by steve_su on 2017/7/20.
 */

public class MediaVideo extends MediaType {
    private static final String TAG = MediaVideo.class.getSimpleName();
    private StoreJetCloudData mModel;

    MediaVideo(Context context) {
        super(context);
        mActivity.mPath = "/twonky/";
        mModel = StoreJetCloudData.VIDEO;
    }

    @Override
    public boolean optionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.option_menu_all_videos:
                viewAllVideo();
                return true;
            case R.id.option_menu_album:
                viewByAlbum();
                return true;
            case R.id.option_menu_date:
                viewByDate();
                return true;
            case R.id.option_menu_select:
                doSelect();
                return true;
            case R.id.option_menu_select_all:
                doSelectAll();
                return true;
        }
        return false;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        MenuInflater inflater = mActivity.getMenuInflater();
        inflater.inflate(R.menu.option_menu_video, menu);
    }

    @Override
    public void load(int position) {
        int menuPosition = mModel.getViewPreference(mContext);
        Log.d(TAG, "menuPosition: "+ menuPosition+ " ==========================================");
        switch (menuPosition) {
            case 0:
                viewAllVideo();
                break;
            case 1:
                viewByAlbum();
                break;
            case 2:
                viewByDate();
                break;
        }
    }

    private void viewAllVideo() {
        viewAll();
    }

    private void viewByAlbum() {

    }

    private void viewByDate() {

    }

}
