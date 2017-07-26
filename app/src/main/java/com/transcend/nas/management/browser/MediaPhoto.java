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

public class MediaPhoto extends MediaType {
    private static final String TAG = MediaPhoto.class.getSimpleName();
    private StoreJetCloudData mModel;

    MediaPhoto(Context context) {
        super(context);
        mActivity.mPath = "/twonky/";
        mModel = StoreJetCloudData.PHOTO;
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
            case R.id.option_menu_album:
                viewByAlbum();
                mModel.setViewPreference(mContext, 2);
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
        inflater.inflate(R.menu.option_menu_photo, menu);
    }

    @Override
    public void load(int position) {
        int menuPosition = mModel.getViewPreference(mContext);
        Log.d(TAG, "menuPosition: "+ menuPosition+ " ==========================================");
        switch (menuPosition) {
            case 0:
                viewAllPhoto();
                break;
            case 1:
                viewByDate();
                break;
            case 2:
                viewByAlbum();
                break;
        }
    }

    private void viewAllPhoto() {
        viewAll();
    }

    private void viewByDate() {

    }

    private void viewByAlbum() {
//        StoreJetCloudData instance = mFragment.mModels.get(mFragment.getTabPosition());
//        Bundle args = new Bundle();
//        int startIndex = isLazyLoading ? instance.getLoadingIndex() : 0;
//        Log.d(TAG, "startIndex: "+ startIndex);
//        args.putInt("start", startIndex);
//        args.putInt("type", instance.getTwonkyType());
//        mFragment.getLoaderManager().restartLoader(mFragment.VIEW_ALBUM, args, mFragment);

    }

}
