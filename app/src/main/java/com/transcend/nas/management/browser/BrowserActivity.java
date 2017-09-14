package com.transcend.nas.management.browser;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.DefaultItemAnimator;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.FileManageActivity;
import com.transcend.nas.management.FileManageDropdownAdapter;
import com.transcend.nas.management.FileManageRecyclerListener;
import com.transcend.nas.management.SmbFileListLoader;
import com.transcend.nas.management.browser_framework.BrowserData;
import com.transcend.nas.management.browser_framework.MediaFragment;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * Created by steve_su on 2017/6/3.
 */

public class BrowserActivity extends FileManageActivity implements BrowserRecyclerAdapter.FilterNotify{
    private static final int FRAGMENT_COUNT_ONE = 1;
    private static final int FRAGMENT_COUNT_TWO = 2;
    MediaController mMediaControl;
    boolean mIsSelectAll = false;
    private int mTabPosition;
    private MenuItem mMenuSearchItem;
    private BrowserSearchView mSearchView;

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
            mMediaControl = new MediaController(this, BrowserData.ALL.getTabPosition());
            mTabPosition = BrowserData.ALL.getTabPosition();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
//        mMediaControl.refresh(true);
        toggleDrawerCheckedItem();

        boolean isTargetBrowser = intent.getBooleanExtra("isTargetBrowser", false);
        if (isTargetBrowser) {
            navigateToTarget(intent);
        }
    }

    private void navigateToTarget(Intent intent) {
        String path = intent.getStringExtra("path");
        BrowserFragment fragment = (BrowserFragment) getSupportFragmentManager().findFragmentByTag(BrowserFragment.TAG);
        if (fragment != null) {
            fragment.setCurrentPage(BrowserData.ALL.getTabPosition());
            doLoad(path);
        } else {
            replaceFragment(new BrowserFragment(), BrowserFragment.TAG);
            doLoad(path);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case R.id.nav_storage:
                BrowserFragment fragment = (BrowserFragment) getSupportFragmentManager().findFragmentByTag(BrowserFragment.TAG);
                if (fragment != null) {
                    backToRootFragment();
                    return true;
                }
        }
        return super.onNavigationItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return mMediaControl.createOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        collapseSearchView();
        boolean isConsumed = super.onOptionsItemSelected(item);
        if (!isConsumed) {
            return mMediaControl.optionsItemSelected(item);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mMediaControl.onPrepareOptionsMenu(menu);
        return true;
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
        super.onLoadFinished(loader, success);

        if (success) {
            if (loader instanceof SmbFileListLoader) {
                BrowserData.getInstance(BrowserData.ALL.getTabPosition()).updateFileList(mFileList);
                StoreJetCloudData.getInstance(BrowserData.ALL.getTabPosition()).setPath(mPath);
                postCheckFragment();

                // TODO check this statement
                // Always enable the drawer indicator
                mDrawerController.setDrawerIndicatorEnabled(true);
                enableFabEdit(mFileActionManager.isDirectorySupportFileAction(mPath));
            }
        }
    }

    /**
     * This function does nothing. RecyclerView will be initialized after calling
     * onRecyclerViewInit().
     *
     */
    @Override
    protected void initRecyclerView() {

    }

    @Override
    public void onRecyclerItemClick(int position) {
        super.onRecyclerItemClick(position);

        if (mEditorMode == null) {
            FileInfo fileInfo = mRecyclerAdapter.getList().get(position);
            if (FileInfo.TYPE.DIR.equals(fileInfo.type) && fileInfo.isTwonkyIndexFolder) {
                mMediaControl.onRecyclerItemClick(fileInfo);
            }
        }

    }

    @Override
    public void onRecyclerItemLongClick(int position) {
        if (mTabPosition == BrowserData.ALL.getTabPosition()) {
            super.onRecyclerItemLongClick(position);
        } else {
            boolean isTwonkyIndexFolder = mFileList.get(position).isTwonkyIndexFolder;
            if (!isTwonkyIndexFolder) {
                if (mEditorMode == null) {
                    startEditorMode();
                    selectAtPosition(position);
                }
            }
        }

    }

    @Override
    public void onRecyclerItemInfoClick(int position) {
        if (mTabPosition == BrowserData.ALL.getTabPosition()) {
            super.onRecyclerItemInfoClick(position);
        } else {
            FileInfo fileInfo = mFileList.get(position);
            if (fileInfo.isTwonkyIndexFolder) {
                if (mEditorMode == null) {
                    mMediaControl.onRecyclerItemClick(fileInfo);
                } else {
                    selectAtPosition(position);
                }
            } else {
                startFileInfoActivity(fileInfo);
            }
        }
    }

    @Override
    public void onRecyclerItemIconClick(int position) {
        if (mTabPosition == BrowserData.ALL.getTabPosition()) {
            super.onRecyclerItemIconClick(position);
        } else {
            FileInfo fileInfo = mFileList.get(position);
            if (!fileInfo.isTwonkyIndexFolder) {
                if (mEditorMode == null) {
                    startEditorMode();
                }
                selectAtPosition(position);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mTabPosition == BrowserData.ALL.getTabPosition()) {
            super.onBackPressed();
        } else {
            if (mPath == null) {
                return;
            }

            clearDownloadTask();
            toggleDrawerCheckedItem();
            if (!stopRunningLoader()) {               // TODO check this statement
                if (!mDrawerController.isDrawerOpen()) {
                    mMediaControl.onBackPressed();
                } else {
                    mDrawerController.closeDrawer();
                }
            }
        }
    }

    @Override
    protected void enableFabEdit(boolean enabled) {
        super.enableFabEdit(enabled);
        StoreJetCloudData.getInstance(mTabPosition).setFabEnabled(enabled);
    }

    @Override
    public void onDropdownItemSelected(int position) {
        if (mTabPosition == BrowserData.ALL.getTabPosition()) {
            super.onDropdownItemSelected(position);
        } else {
            // do nothing
        }
    }

    @Override
    public void doRefresh() {
        if (mTabPosition == BrowserData.ALL.getTabPosition()) {
            doLoad(mPath);
        } else {
            mMediaControl.refresh(true);
        }
    }

    @Override
    protected void initMenu(Menu menu) {
        if (mTabPosition == BrowserData.ALL.getTabPosition()) {
            super.initMenu(menu);
        } else {
            getMenuInflater().inflate(R.menu.twonky_editor, menu);
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        super.onDestroyActionMode(mode);
        mIsSelectAll = false;
    }

    @Override
    public void toggleSelectAll() {
        super.toggleSelectAll();
        int count = getSelectedCount();
        mIsSelectAll = (count != 0) && (count == mFileList.size());
    }

    @Override
    public void publishResults(ArrayList list) {
        mFileList = list;
    }

    void updateSelectAll() {
        for (FileInfo file : mFileList)
            file.checked = true;

        updateEditorModeTitle(mFileList.size());
    }

    /**
     * update spinner after switching between pages of ViewPager.
     *
     * @param position
     */
    void updateSpinner(int position) {
        if (position != 0) {
            mDropdownAdapter = new BrowserDropdownAdapter(this);
        } else {
            mDropdownAdapter = new FileManageDropdownAdapter(this);
        }

        mDropdownAdapter.updateList(mPath, mFileActionManager.getServiceMode());
        mDropdownAdapter.setOnDropdownItemSelectedListener(this);
        mDropdown = (AppCompatSpinner) findViewById(R.id.main_dropdown);
        mDropdown.setAdapter(mDropdownAdapter);
        mDropdown.setDropDownVerticalOffset(10);
        mDropdownAdapter.notifyDataSetChanged();
    }

    /**
     * update spinner after onLoadFinished().
     *
     * @param path
     */
    void updateSpinner(String path) {
        mDropdownAdapter.updateList(path, mFileActionManager.getServiceMode());
        mDropdownAdapter.notifyDataSetChanged();
    }

    void onRecyclerViewInit(MediaFragment fragment) {
        mRecyclerView = fragment.getRecyclerView();
        mRecyclerEmptyView = fragment.getRecyclerEmptyView();
        mRecyclerAdapter = (BrowserRecyclerAdapter) mRecyclerView.getAdapter();
        mRecyclerAdapter.setOnRecyclerItemCallbackListener(this);
        ((BrowserRecyclerAdapter) mRecyclerAdapter).setFilterListener(this);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addOnScrollListener(new FileManageRecyclerListener(ImageLoader.getInstance(), true, false));
        mFileList = new ArrayList<>(mRecyclerAdapter.getList());
        mTabPosition = fragment.getPosition();
        mMediaControl = new MediaController(this, mTabPosition);
        invalidateOptionsMenu();
        mPath = StoreJetCloudData.getInstance(mTabPosition).getPath();
        updateSpinner(mTabPosition);
        boolean isFabEnabled = StoreJetCloudData.getInstance(mTabPosition).getFabEnabled();
        enableFabEdit(isFabEnabled);
    }

    void onRecyclerViewInit() {
        updateListView(false);
        mRecyclerAdapter.setOnRecyclerItemCallbackListener(this);
        mRecyclerView.setAdapter(mRecyclerAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addOnScrollListener(new FileManageRecyclerListener(ImageLoader.getInstance(), true, false));
        mTabPosition = BrowserData.ALL.getTabPosition();
        mMediaControl = new MediaController(this, mTabPosition);
        updateSpinner(mTabPosition);
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

    void configSearchView(Menu menu) {
        mMenuSearchItem = menu.findItem(R.id.my_search);
        // Get the SearchView and set the searchable configuration
        mSearchView = (BrowserSearchView) mMenuSearchItem.getActionView();
        mSearchView.setActivity(this);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        // Assumes current activity is the searchable activity
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        mSearchView.setIconifiedByDefault(true);
        mSearchView.setOnQueryTextListener(mSearchView);
        MenuItemCompat.setOnActionExpandListener(mMenuSearchItem, mSearchView);
    }

    void configSearchCursor() {
        AutoCompleteTextView searchTextView = (AutoCompleteTextView) mSearchView.findViewById(
                android.support.v7.appcompat.R.id.search_src_text);
        try {
            Field mCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
            mCursorDrawableRes.setAccessible(true);

            //This sets the cursor resource ID to 0 or @null which will make it visible on white background
            mCursorDrawableRes.set(searchTextView, R.drawable.color_cursor);
        } catch (Exception e) {
            // do nothing
        }
    }

    void collapseSearchView() {
        if (mMenuSearchItem != null) {
            if (mMenuSearchItem.isActionViewExpanded()) {
                mMenuSearchItem.collapseActionView();
            }
        }
    }

    private void replaceFragment(Fragment fragment, String tag) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//        transaction.setCustomAnimations(R.anim.appear, 0);
        transaction.replace(R.id.fragment_container, fragment, tag);
        transaction.addToBackStack(null);
        transaction.commitAllowingStateLoss();
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

    private void backToRootFragment() {
        mDrawerController.closeDrawer();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                getSupportFragmentManager().popBackStackImmediate();
                mPath = NASApp.ROOT_SMB;
                doLoad(mPath);
                mProgressView.setVisibility(View.INVISIBLE);
            }
        }, 280);
    }

}
