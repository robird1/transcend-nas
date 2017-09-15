package com.transcend.nas.management.browser;

import android.content.Context;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.transcend.nas.R;
import com.transcend.nas.management.browser_framework.Browser;

/**
 * Created by steve_su on 2017/7/20.
 */

public class MediaMusic extends MediaGeneral {

    MediaMusic(Context context) {
        super(context);
        mModel = StoreJetCloudData.MUSIC;
        mRequestControl = new RequestMusic(mActivity);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        MenuInflater inflater = mActivity.getMenuInflater();

        if ("view_all".equals(mRequestControl.getAPIName(mActivity.mPath)) ||
                "get_music".equals(mRequestControl.getAPIName(mActivity.mPath))) {
            inflater.inflate(R.menu.option_menu_music_file, menu);
        } else {
            inflater.inflate(R.menu.option_menu_music_index, menu);
        }

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean optionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.option_menu_all_tracks:
                mModel.setListSize(0);
                viewAllTrack();
                mModel.setViewPreference(mContext, 0);
                return true;
            case R.id.option_menu_artist:
                viewByArtist();
                mModel.setViewPreference(mContext, 1);
                return true;
            case R.id.option_menu_album:
                viewByAlbum();
                mModel.setViewPreference(mContext, 2);
                return true;
            case R.id.option_menu_genre:
                viewByGenre();
                mModel.setViewPreference(mContext, 3);
                return true;
            case R.id.option_menu_select:
                doSelect();
                return true;
//            case R.id.option_menu_select_all:
//                doSelectAll();
//                return true;
            case R.id.option_menu_refresh:
                mRequestControl.refresh(true);
                return true;
        }
        return false;
    }

    @Override
    public Browser.LayoutType onViewAllLayout() {
        return Browser.LayoutType.LIST;
    }

    @Override
    public void onPageChanged() {
        int menuPosition = mModel.getViewPreference(mContext);
        switch (menuPosition) {
            case 0:
                viewAllTrack();
                break;
            case 1:
                viewByArtist();
                break;
            case 2:
                viewByAlbum();
                break;
            case 3:
                viewByGenre();
                break;
        }
    }

    private void viewAllTrack() {
        mRequestControl.viewAll();
    }

    private void viewByArtist() {
        mRequestControl.viewByArtist();
    }

    private void viewByAlbum() {
        mRequestControl.viewByAlbum();
    }

    private void viewByGenre() {
        mRequestControl.viewByGenre();
    }

}
