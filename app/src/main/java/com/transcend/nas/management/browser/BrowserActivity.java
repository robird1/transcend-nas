package com.transcend.nas.management.browser;

import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.DefaultItemAnimator;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
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
import java.util.ArrayList;

/**
 * Created by steve_su on 2017/6/3.
 */

public class BrowserActivity extends FileManageActivity {
    private static final String TAG = BrowserActivity.class.getSimpleName();
    private static final int FRAGMENT_COUNT_ONE = 1;
    private static final int FRAGMENT_COUNT_TWO = 2;
    MediaController mMediaControl;
    int mTabPosition;
    boolean mIsSelectAll = false;

    @Override
    public int onLayoutID() {
        return R.layout.activity_file_manage_fragment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "[Enter] onCreate");
        super.onCreate(savedInstanceState);

        String password = NASPref.getPassword(this);
        if (!TextUtils.isEmpty(password)) {
            replaceFragment(new RootFragment(), RootFragment.TAG);

            // TODO check this statement
//            Log.d(TAG, "[Enter]  new MediaController");
            mMediaControl = new MediaController(this, BrowserData.ALL.getTabPosition());
            mTabPosition = BrowserData.ALL.getTabPosition();
        }
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//
//    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "[Enter] onNewIntent");
//        mMediaControl.refresh(true);
        toggleDrawerCheckedItem();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        Log.d(TAG, "[Enter] onCreateOptionsMenu");              // TODO
        return mMediaControl.createOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        Log.d(TAG, "[Enter] onOptionsItemSelected");
        boolean isConsumed = super.onOptionsItemSelected(item);
//        Log.d(TAG, "isConsumed: "+ isConsumed);
        if (!isConsumed) {
            return mMediaControl.optionsItemSelected(item);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
//        Log.d(TAG, "[Enter] onPrepareOptionsMenu");
        mMediaControl.onPrepareOptionsMenu(menu);
//        return super.onPrepareOptionsMenu(menu);
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
        Log.d(TAG, "[Enter] onLoadFinished loader: "+ loader.toString());
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
     * This function does nothing. RecyclerView will be initialized after calling onRecyclerViewInit().
     *
     */
    @Override
    protected void initRecyclerView() {

    }

    @Override
    public void onRecyclerItemClick(int position) {
        Log.d(TAG, "\n[Enter] onRecyclerItemClick");

//        if (mTabPosition == BrowserData.ALL.getTabPosition()) {
//            super.onRecyclerItemClick(position);
//
//        } else {
//
//            if (mEditorMode == null) {
////            FileInfo fileInfo = mFileList.get(position);
//                Log.d(TAG, "mRecyclerAdapter.getList().size: "+ mRecyclerAdapter.getList().size());
//
//                FileInfo fileInfo = mRecyclerAdapter.getList().get(position);
//
//                Log.d(TAG, "fileInfo.path: "+ fileInfo.path);
//                Log.d(TAG, "fileInfo.name: "+ fileInfo.name);
//                Log.d(TAG, "fileInfo.isTwonkyIndexFolder: "+ fileInfo.isTwonkyIndexFolder);
//
//                if (FileInfo.TYPE.DIR.equals(fileInfo.type) && fileInfo.isTwonkyIndexFolder) {
//                    Log.d(TAG, "[Enter] mMediaControl.onRecyclerItemClick");
//                    mMediaControl.onRecyclerItemClick(fileInfo);
//                }
//            } else {
//                selectAtPosition(position);
//            }
//
//        }

        super.onRecyclerItemClick(position);

        if (mEditorMode == null) {
//            Log.d(TAG, "mRecyclerAdapter.getList().size: "+ mRecyclerAdapter.getList().size());

            FileInfo fileInfo = mRecyclerAdapter.getList().get(position);

//            Log.d(TAG, "fileInfo.path: "+ fileInfo.path);
//            Log.d(TAG, "fileInfo.name: "+ fileInfo.name);
//            Log.d(TAG, "fileInfo.isTwonkyIndexFolder: "+ fileInfo.isTwonkyIndexFolder);

            if (FileInfo.TYPE.DIR.equals(fileInfo.type) && fileInfo.isTwonkyIndexFolder) {
//                Log.d(TAG, "[Enter] mMediaControl.onRecyclerItemClick");
                mMediaControl.onRecyclerItemClick(fileInfo);
            }
        }

    }

    @Override
    public void onRecyclerItemLongClick(int position) {
//        if (!mFileActionManager.isDirectorySupportFileAction(mPath))
//            return;
//
//        boolean isTwonkyIndexFolder = mFileList.get(position).isTwonkyIndexFolder;
//        if (!isTwonkyIndexFolder) {
//            if (mEditorMode == null) {
//                startEditorMode();
//                selectAtPosition(position);
//            }
//        }
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
        Log.d(TAG, "\n[Enter] onBackPressed mPath: "+ mPath);
        if (mTabPosition == BrowserData.ALL.getTabPosition()) {
            super.onBackPressed();
        } else {
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
            mMediaControl.onCreateActionMode(menu);
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

    void updateSelectAll() {
        for (FileInfo file : mFileList)
            file.checked = true;

        updateEditorModeTitle(mFileList.size());
    }

    void updateDropdown(int position) {
//        Log.d(TAG, "[Enter] updateDropdown");

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

    void updateSpinner(String path) {
//        Log.d(TAG, "[Enter] updateSpinner path: "+ path);
        mDropdownAdapter.updateList(path, mFileActionManager.getServiceMode());
        mDropdownAdapter.notifyDataSetChanged();
    }

    public void onRecyclerViewInit(MediaFragment fragment) {
        Log.d(TAG, "\n[Enter] onRecyclerViewInit");
        mRecyclerView = fragment.getRecyclerView();
        mRecyclerEmptyView = fragment.getRecyclerEmptyView();
        mRecyclerAdapter = (BrowserRecyclerAdapter) mRecyclerView.getAdapter();
        mRecyclerAdapter.setOnRecyclerItemCallbackListener(this);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addOnScrollListener(new FileManageRecyclerListener(ImageLoader.getInstance(), true, false));
        mFileList = new ArrayList<>(mRecyclerAdapter.getList());
//        mRecyclerAdapter.updateList(mFileList);
//        mRecyclerAdapter.notifyDataSetChanged();
        Log.d(TAG, "[Enter] mFileList.size(): "+ mFileList.size());

        mTabPosition = fragment.getPosition();
        mMediaControl = new MediaController(this, mTabPosition);
        invalidateOptionsMenu();
        mPath = StoreJetCloudData.getInstance(mTabPosition).getPath();
        updateDropdown(mTabPosition);
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
        updateDropdown(mTabPosition);
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

    private void replaceFragment(Fragment fragment, String tag) {
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
