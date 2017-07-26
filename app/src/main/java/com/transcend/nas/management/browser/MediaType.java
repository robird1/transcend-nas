package com.transcend.nas.management.browser;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.transcend.nas.R;

import java.util.ArrayList;

import static com.transcend.nas.NASApp.getContext;
import static com.transcend.nas.management.browser_framework.Browser.VIEW_ALBUM;
import static com.transcend.nas.management.browser_framework.Browser.VIEW_ALL;

/**
 * Created by steve_su on 2017/7/20.
 */

public abstract class MediaType {
    private static final String TAG = MediaType.class.getSimpleName();
    protected Context mContext;
    protected SJC_FileManageActivity mActivity;
    protected SJC_Browser mFragment;
//    protected StoreJetCloudData mModel;

    MediaType(Context context) {
        mContext = context;
        mActivity = (SJC_FileManageActivity) context;
        mFragment = (SJC_Browser) mActivity.getSupportFragmentManager().findFragmentByTag(SJC_Browser.TAG);
//        mModel = StoreJetCloudData.getInstance(mFragment.getTabPosition());
    }

    public boolean createOptionsMenu(Menu menu) {
        ((Activity) mContext).getMenuInflater().inflate(R.menu.file_manage_viewer, menu);
//        FileManageRecyclerAdapter.LayoutType type = NASPref.getFileViewType(mActivity);
//        Browser.LayoutType type = BrowserData.ALL.getViewMode(mContext);
//        switch (type) {
//            case GRID:
//                menu.findItem(R.id.file_manage_viewer_action_view).setIcon(R.drawable.ic_toolbar_list_white);
//                break;
//            default:
//                menu.findItem(R.id.file_manage_viewer_action_view).setIcon(R.drawable.ic_toolbar_module_white);
//                break;
//        }

        mActivity.mCastManager.addMediaRouterButton(menu, R.id.media_route_menu_item);

        return true;
    }

    public Loader onCreateLoader(int id, Bundle args) {
        if (id == VIEW_ALL) {
            new ViewAllLoader(getContext(), args);
        } else if (id == VIEW_ALBUM) {
            new ViewAlbumLoader(getContext(), args);
        }
        return new ViewAllLoader(getContext(), args);
    }

    public void onLoadFinished(Loader loader, ArrayList data) {
        int position = mFragment.getTabPosition();

        if (loader instanceof ViewAllLoader) {
            int nextLoadingIndex = ((ViewAllLoader) loader).nextLoadingIndex();
            StoreJetCloudData.getInstance(position).setLoadingIndex(nextLoadingIndex);

            if (! ((ViewAllLoader) loader).isLoadingFinish()) {
                lazyLoad();
            } else {
                int itemCount = mFragment.getRecyclerViewAdapter().getItemCount();
                Log.d(TAG, "itemCount: "+ itemCount + " ======================================================");
            }

        } else if (loader instanceof ViewAlbumLoader) {

        }
    }

    public void load(int position) {

    }

    protected void viewAll() {
        mFragment.viewALL();
    }

    protected void doSelect() {
        mActivity.startEditorMode();
    }

    protected void doSelectAll() {
        mActivity.startEditorMode();
        mActivity.toggleSelectAll();
    }

    private void lazyLoad() {
        mFragment.lazyLoad();
    }

    public abstract boolean optionsItemSelected(MenuItem item);
    public abstract void onPrepareOptionsMenu(Menu menu);


}
