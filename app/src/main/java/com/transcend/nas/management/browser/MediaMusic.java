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

public class MediaMusic extends MediaType {
    private static final String TAG = MediaMusic.class.getSimpleName();
    private StoreJetCloudData mModel;

    MediaMusic(Context context) {
        super(context);
        mActivity.mPath = "/twonky/";
        mModel = StoreJetCloudData.MUSIC;
    }

    @Override
    public boolean optionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.option_menu_all_tracks:
                viewAllTrack();
                return true;
            case R.id.option_menu_artist:
                viewByArtist();
                return true;
            case R.id.option_menu_album:
                viewByAlbum();
                return true;
            case R.id.option_menu_genre:
                viewByGenre();
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
        inflater.inflate(R.menu.option_menu_music, menu);
    }

    @Override
    public void load(int position) {
        int menuPosition = mModel.getViewPreference(mContext);
        Log.d(TAG, "menuPosition: "+ menuPosition+ " ==========================================");
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
        }
    }

    private void viewAllTrack() {
        viewAll();
    }

    private void viewByArtist() {

    }

    private void viewByAlbum() {

    }

    private void viewByGenre() {

    }

}
