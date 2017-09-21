package com.transcend.nas.management.browser;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;

import com.transcend.nas.R;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.browser_framework.Browser;

/**
 * Created by steve_su on 2017/7/20.
 */

public abstract class MediaGeneral {
    protected Context mContext;
    protected BrowserActivity mActivity;
    protected StoreJetCloudData mModel;
    protected RequestAction mRequestControl;

    MediaGeneral(Context context) {
        mContext = context;
        mActivity = (BrowserActivity) context;
    }

    public void onPrepareOptionsMenu(Menu menu) {
//        menu.clear();
//        MenuInflater inflater = mActivity.getMenuInflater();
//        inflater.inflate(R.menu.file_manage_viewer, menu);
        mActivity.mCastManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
        mActivity.configSearchView(menu);
        mActivity.configSearchCursor();
    }

    public boolean optionsItemSelected(MenuItem item) {
        return false;
    }

    public void onRecyclerItemClick(FileInfo fileInfo) {
        mRequestControl.onRecyclerItemClick(fileInfo);
    }

    public void onBackPressed() {
        mRequestControl.onBackPressed();
    }

    public void refresh(boolean showProgress) {
        mRequestControl.refresh(showProgress);
    }

    public void search(String text) {
        if (!(mActivity.mFragment instanceof FragmentSearch)) {
            mActivity.addFragment(new FragmentSearch());
        }
        mRequestControl.search(text);
    }

    public void onPageChanged() {

    }

    public void lazyLoad() {
        mRequestControl.lazyLoad();
    }

    public Browser.LayoutType onViewAllLayout() {
        return Browser.LayoutType.GRID;
    }

    protected void doSelect() {
        mActivity.startEditorMode();
    }

//    protected void doSelectAll() {
//        mActivity.startEditorMode();
//        mActivity.toggleSelectAll();
//    }

}
