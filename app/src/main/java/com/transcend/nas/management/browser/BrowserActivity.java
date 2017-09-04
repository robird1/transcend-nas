package com.transcend.nas.management.browser;

import android.content.Loader;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.management.FileManageActivity;
import com.transcend.nas.management.FileManageRecyclerAdapter;
import com.transcend.nas.management.FileManageRecyclerListener;
import com.transcend.nas.management.SmbFileListLoader;
import com.transcend.nas.management.browser_framework.Browser;
import com.transcend.nas.management.browser_framework.BrowserData;
import com.transcend.nas.management.browser_framework.MediaFragment;

import java.io.File;

/**
 * Created by steve_su on 2017/6/3.
 */

public class BrowserActivity extends FileManageActivity {

    private static final String TAG = BrowserActivity.class.getSimpleName();
    private static final int FRAGMENT_COUNT_ONE = 1;
    private static final int FRAGMENT_COUNT_TWO = 2;
    MediaController mMediaControl;

    @Override
    public int onLayoutID() {
        return R.layout.activity_file_manage_fragment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String password = NASPref.getPassword(this);
        if (!TextUtils.isEmpty(password)) {
            replaceFragment(new RootFragment(), RootFragment.TAG);

            // TODO check this statement
            Log.d(TAG, "[Enter]  new MediaController");
            mMediaControl = new MediaController(this, BrowserData.ALL.getTabPosition());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "[Enter] onCreateOptionsMenu");              // TODO onCreateOptionsMenu is been called many times
        return mMediaControl.createOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mMediaControl.optionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d(TAG, "[Enter] onPrepareOptionsMenu");
        mMediaControl.onPrepareOptionsMenu(menu);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        Loader<Boolean> loader = mActionHelper.onCreateLoader(id, args);
        if (loader instanceof SmbFileListLoader)
            mSmbFileListLoader = (SmbFileListLoader) loader;
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        Log.d(TAG, "[Enter] onLoadFinished loader: "+ loader.toString());
        super.onLoadFinished(loader, success);

        if (success) {
            if (loader instanceof SmbFileListLoader) {
                BrowserData.getInstance(BrowserData.ALL.getTabPosition()).updateFileList(mFileList, true);
                postCheckFragment();
            }
        }
    }

    @Override
    protected void initRecyclerView() {

    }

//    @Override
//    protected void initProgressView() {
//        mProgressView = (RelativeLayout) findViewById(R.id.main_progress_view);
//        mProgressBar = (ProgressBar) findViewById(R.id.main_progress_bar);
//        mActionHelper.setProgressLayout(mProgressView);
//    }

    public void onRecyclerViewInit(MediaFragment fragment) {
        Log.d(TAG, "[Enter] onRecyclerViewInit");
        mRecyclerView = fragment.getRecyclerView();
        mRecyclerEmptyView = fragment.getRecyclerEmptyView();

        Browser.LayoutType type = BrowserData.getInstance(fragment.getPosition()).getViewMode(this);
        if (type == Browser.LayoutType.GRID) {
            updateGridView(false, mRecyclerView);
        } else {
            updateListView(false, mRecyclerView);
        }

        mRecyclerAdapter = (FileManageRecyclerAdapter) mRecyclerView.getAdapter();
        mRecyclerAdapter.setOnRecyclerItemCallbackListener(this);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addOnScrollListener(new FileManageRecyclerListener(ImageLoader.getInstance(), true, false));

        mFileList = mRecyclerAdapter.getList();
        mRecyclerAdapter.updateList(mFileList);
        mRecyclerAdapter.notifyDataSetChanged();

        mMediaControl = new MediaController(this, fragment.getPosition());
        Log.d(TAG, "[Enter] invalidateOptionsMenu");
        invalidateOptionsMenu();

    }

    void onRecyclerViewInit() {
        FileManageRecyclerAdapter.LayoutType type = NASPref.getFileViewType(this);
        switch (type) {
            case GRID:
                updateGridView(false);
                break;
            default:
                updateListView(false);
                break;
        }
        mRecyclerAdapter.setOnRecyclerItemCallbackListener(this);

        mRecyclerView.setAdapter(mRecyclerAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addOnScrollListener(new FileManageRecyclerListener(ImageLoader.getInstance(), true, false));
    }

    void onProgressViewInit(RootFragment fragment) {
        mProgressView.setVisibility(View.INVISIBLE);
        mProgressView = fragment.mProgressView;
        mProgressBar = fragment.mProgressBar;
        mRecyclerRefresh = fragment.mSwipeRefreshLayout;
        mActionHelper.setProgressLayout(mProgressView);
    }
    void onProgressViewInit(BrowserFragment fragment) {
        mProgressView = fragment.mProgressView;
        mProgressBar = fragment.mProgressBar;
        mRecyclerRefresh = fragment.mSwipeRefreshLayout;
        mActionHelper.setProgressLayout(mProgressView);
    }

    // TODO check super.updateListView
    protected void updateListView(boolean update, RecyclerView view) {
        mRecyclerView.setLayoutManager(view.getLayoutManager());
        if (update) {
            mRecyclerView.getRecycledViewPool().clear();
            mRecyclerAdapter.notifyDataSetChanged();
        }
    }

    // TODO check super.updateGridView
    protected void updateGridView(boolean update, RecyclerView view) {
        GridLayoutManager grid = (GridLayoutManager) view.getLayoutManager();
        grid.setSpanSizeLookup(new SpanSizeLookup(grid.getSpanCount()));
        mRecyclerView.setLayoutManager(grid);
        if (update) {
            mRecyclerView.getRecycledViewPool().clear();
            mRecyclerAdapter.notifyDataSetChanged();
        }
    }

    private void replaceFragment(Fragment fragment, String tag) {
        Log.d(TAG, "[Enter] replaceFragment");
//        if (fragment instanceof BrowserFragment) {
//            Log.d(TAG, "[Enter] enableFabEdit mPath: "+ mPath);
//            enableFabEdit(mFileActionManager.isDirectorySupportFileAction(mPath));
//        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//        transaction.setCustomAnimations(R.anim.appear, 0);
        transaction.replace(R.id.fragment_container, fragment, tag);
        transaction.addToBackStack(null);
        transaction.commit();
        getSupportFragmentManager().executePendingTransactions();
    }

    /**
     * Do fragment transaction after finishing SmbFileListLoader if it is necessary.
     *
     */
    private void postCheckFragment() {
        switch (getSupportFragmentManager().getBackStackEntryCount()) {
            case FRAGMENT_COUNT_ONE:
                boolean isNavigateFromRoot = NASApp.ROOT_SMB.equals(new File(mPath).getParent());

                if (isNavigateFromRoot) {
                    replaceFragment(new BrowserFragment(), BrowserFragment.TAG);
                }
                break;
            case FRAGMENT_COUNT_TWO:
                // back to root fragment
                if (NASApp.ROOT_SMB.equals(mPath)) {
                    getSupportFragmentManager().popBackStackImmediate();
                    updateScreen();
                    mProgressView.setVisibility(View.INVISIBLE);
                }
                break;
            default:
                // do nothing
                break;
        }
    }

}
