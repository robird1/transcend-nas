package com.transcend.nas.management.browser;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.transcend.nas.R;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.browser_framework.Browser;
import com.transcend.nas.management.browser_framework.BrowserData;
import com.transcend.nas.management.browser_framework.MediaFragment;

import java.util.ArrayList;

import static com.transcend.nas.management.browser.RequestAction.TWONKY_CUSTOM;
import static com.transcend.nas.management.browser.RequestAction.TWONKY_INDEX;
import static com.transcend.nas.management.browser.RequestAction.TWONKY_VIEW_ALL;


/**
 * Created by steve_su on 2017/7/10.
 */

public class BrowserFragment extends Browser implements LoaderManager.LoaderCallbacks<Boolean>, Browser.StateMonitor {
    static final String TAG = BrowserFragment.class.getSimpleName();
    private BrowserActivity mActivity;
    private int mRunningLoaderID = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (BrowserActivity) getActivity();

        mActivity.mDrawerController.setDrawerIndicatorEnabled(true);

        setStateMonitor(this);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
//        Log.d(TAG, "[Enter] onViewCreated");
        super.onViewCreated(view, savedInstanceState);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mActivity.mMediaControl.refresh(false);
            }
        });
        mProgressView.setVisibility(View.INVISIBLE);
    }

    @Override
    protected BrowserData[] onTabInstance() {
        BrowserData[] tabs = {BrowserData.ALL, BrowserData.PHOTO, BrowserData.MUSIC, BrowserData.VIDEO};
        return tabs;
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        Loader loader = null;
        if (id == TWONKY_VIEW_ALL) {
            loader = new TwonkyViewAllLoader(getContext(), args);
        } else if (id == TWONKY_INDEX) {
            loader = new TwonkyIndexLoader(getContext(), args);
        } else if (id == TWONKY_CUSTOM) {
            loader = new TwonkyCustomLoader(getContext(), args);
        }

        if (loader != null) {
            mRunningLoaderID = id;
        }
        return loader;
    }

    @Override
    public void onLoadFinished(Loader loader, Boolean isSuccess) {
        Log.d(TAG, "\n[Enter] onLoadFinished");
        mProgressView.setVisibility(View.INVISIBLE);
        mSwipeRefreshLayout.setRefreshing(false);

        if (!isSuccess) {
            // TODO error handling
            Toast.makeText(mActivity, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "isSuccess == false ========================================================");
            return;
        }

        BrowserRecyclerAdapter adapter = (BrowserRecyclerAdapter) getRecyclerViewAdapter();

        if (loader instanceof TwonkyViewAllLoader) {
//            Log.d(TAG, "[Enter] loader instanceof TwonkyViewAllLoader");
            TwonkyViewAllLoader ld = ((TwonkyViewAllLoader) loader);
            BrowserData.getInstance(getTabPosition()).addFiles(ld.getFileList());

            Log.d(TAG, "ld.getFileList().size(): "+ ld.getFileList().size());

            mActivity.mPath = ld.getPath();

            if (ld.getStartIndex() == 0) {
                adapter.updateList(ld.getFileList());
                mActivity.mFileList = new ArrayList<>(ld.getFileList());
                changeViewLayout(mActivity.mMediaControl.onViewAllLayout());
                mActivity.invalidateOptionsMenu();
                mActivity.updateSpinner(ld.getPath());
                mActivity.enableFabEdit(true);

            } else {
                Log.d(TAG, "mActivity.mFileList.size(): "+ mActivity.mFileList.size());
                adapter.addFiles(ld.getFileList());
                Log.d(TAG, "mActivity.mFileList.size(): "+ mActivity.mFileList.size());
                mActivity.mFileList.addAll(ld.getFileList());
                Log.d(TAG, "adapter.getList().size(): "+ adapter.getList().size());
                Log.d(TAG, "mActivity.mFileList.size(): "+ mActivity.mFileList.size());

                if (mActivity.mIsSelectAll) {
                    mActivity.updateSelectAll();
                }

            }
            adapter.notifyDataSetChanged();

            int nextLoadingIndex = ld.nextLoadingIndex();
            Log.d(TAG, "nextLoadingIndex: "+ nextLoadingIndex);
            StoreJetCloudData.getInstance(getTabPosition()).setLoadingIndex(nextLoadingIndex);

//            if (!ld.isLoadingFinish()) {
//                Log.d(TAG, "[Enter] lazyLoad");
//                mActivity.mMediaControl.lazyLoad();
//            }


        } else if (loader instanceof TwonkyIndexLoader) {
//            Log.d(TAG, "[Enter] loader instanceof TwonkyIndexLoader");
            TwonkyIndexLoader ld = ((TwonkyIndexLoader) loader);
            adapter.updateList(ld.getFileList());

//            printLog(adapter);

            BrowserData.getInstance(getTabPosition()).updateFileList(ld.getFileList());
            mActivity.mPath = ld.getPath();
            mActivity.mFileList = new ArrayList<>(ld.getFileList());
            changeViewLayout(LayoutType.LIST);
//            printLog(adapter);
            mActivity.invalidateOptionsMenu();
            mActivity.updateSpinner(ld.getPath());
            mActivity.enableFabEdit(false);

        } else if (loader instanceof TwonkyCustomLoader) {
//            Log.d(TAG, "[Enter] loader instanceof TwonkyCustomLoader");
            TwonkyCustomLoader ld = ((TwonkyCustomLoader) loader);
            adapter.updateList(ld.getFileList());
//            printLog(adapter);
            BrowserData.getInstance(getTabPosition()).updateFileList(ld.getFileList());
            mActivity.mPath = ld.getPath();
            mActivity.mFileList = new ArrayList<>(ld.getFileList());
            changeViewLayout(mActivity.mMediaControl.onViewAllLayout());

            mActivity.invalidateOptionsMenu();
            mActivity.updateSpinner(ld.getPath());
            mActivity.enableFabEdit(true);

        }
        StoreJetCloudData.getInstance(getTabPosition()).setPath(mActivity.mPath);

        Log.d(TAG, "BrowserData.getInstance().getFileList().size(): "+ BrowserData.getInstance(getTabPosition()).getFileList().size());
        Log.d(TAG, "mFileList.size(): "+ mActivity.mFileList.size());
//        Log.d(TAG, "itemCount: " + getRecyclerViewAdapter().getItemCount());

        mActivity.checkEmptyView();
    }

    @Override
    public void onLoaderReset(Loader loader) {
        Log.d(TAG, "[Enter] onLoaderReset");

        //TODO update BrowserData after destroying a running loader

    }

    @Override
    protected void onPageChanged(int lastPosition, int currentPosition) {
        Log.d(TAG, "\n[Enter] onPageChanged");

        mActivity.closeEditorMode();
        updateViewReference();
        mProgressView.setVisibility(View.INVISIBLE);

        stopRunningLoader();
        boolean isFirstSwitch = getRecyclerViewAdapter().getItemCount() == 0;
        if (isFirstSwitch) {
//            Log.d(TAG, "[Enter] isFirstSwitch");
            mActivity.mMediaControl.onPageChanged();
        }
    }

    private void updateViewReference() {
        MediaFragment fragment = getCurrentFragment();
        mActivity.onRecyclerViewInit(fragment);
        mActivity.onProgressViewInit(this);
    }

    @Override
    public void onFinishCreateView(int position) {
//        Log.d(TAG, "[Enter] onFinishCreateView");

        // TODO check this statement
        if (position == 0 && getTabPosition() == 0) {
            updateViewReference();
        }

        getRecyclerView().addOnScrollListener(new RecyclerScrollListener() {
            @Override
            public void onLoadMore(int current_page) {
                Log.d(TAG, "[Enter] onLoadMore fragment: "+ getCurrentFragment().getPosition());
                int viewPreference = StoreJetCloudData.getInstance(getTabPosition()).getViewPreference(mActivity);
                if (viewPreference == 0) {
                    if (!BrowserSearchView.mIsSearchMode) {
                        mActivity.mMediaControl.lazyLoad();
                    } else {
                        this.cancelLoadMore();
                    }
                }

//                if (!BrowserSearchView.mIsSearchMode) {
//                    mActivity.mMediaControl.lazyLoad();
//                } else {
//                    this.cancelLoadMore();
//                }
            }
        });

    }

    public void changeViewLayout(LayoutType mode) {
        RecyclerView.LayoutManager lm;
        if (mode == LayoutType.GRID) {
            int spanCount = 3;
            lm = new GridLayoutManager(getContext(), spanCount);
        } else {
            lm = new LinearLayoutManager(getContext());
        }
        getRecyclerView().setLayoutManager(lm);
        getRecyclerView().getRecycledViewPool().clear();
        getRecyclerViewAdapter().notifyDataSetChanged();

        BrowserData.getInstance(getTabPosition()).setLayout(mode);
    }

    public void stopRunningLoader() {
        Log.d(TAG, "\n[Enter] stopRunningLoader ==================================================================");
        boolean hasRunningLoaders = getLoaderManager().hasRunningLoaders();
        Log.d(TAG, "hasRunningLoaders: "+ hasRunningLoaders);
        if (hasRunningLoaders) {
            getLoaderManager().destroyLoader(mRunningLoaderID);
            mRunningLoaderID = -1;
        }
    }

    private void printLog(BrowserRecyclerAdapter adapter) {
        for (FileInfo info : adapter.getList()) {
            Log.d(TAG, "info.name: "+ info.name);
            Log.d(TAG, "info.path: "+ info.path);
            Log.d(TAG, "info.type: "+ info.type);
            Log.d(TAG, "info.isTwonkyIndexFolder: "+ info.isTwonkyIndexFolder);
        }

//        for (FileInfo info : mActivity.mFileList) {
//            Log.d(TAG, "[mActivity.mFileList] info.name: "+ info.name);
//            Log.d(TAG, "[mActivity.mFileList] info.path: "+ info.path);
//            Log.d(TAG, "[mActivity.mFileList] info.type: "+ info.type);
//            Log.d(TAG, "[mActivity.mFileList] info.isTwonkyIndexFolder: "+ info.isTwonkyIndexFolder);
//        }

    }

}