package com.transcend.nas.management.browser;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import com.transcend.nas.R;
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
    private String mSystemPath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (BrowserActivity) getActivity();

        // shared folder path
        mSystemPath = "/home".concat(mActivity.mPath);

        mActivity.mDrawerController.setDrawerIndicatorEnabled(true);

        setStateMonitor(this);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
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
        mProgressView.setVisibility(View.INVISIBLE);
        mSwipeRefreshLayout.setRefreshing(false);

        if (!isSuccess) {
            Toast.makeText(mActivity, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            return;
        }

        if (loader instanceof TwonkyViewAllLoader) {
            updateUIViewAll(loader);

        } else if (loader instanceof TwonkyIndexLoader) {
            updateUIIndex(loader);

        } else if (loader instanceof TwonkyCustomLoader) {
            updateUICustom(loader);
        }

        mActivity.checkEmptyView();
    }

    private void updateUICustom(Loader loader) {
        BrowserRecyclerAdapter adapter = (BrowserRecyclerAdapter) getRecyclerViewAdapter();
        TwonkyCustomLoader ld = (TwonkyCustomLoader) loader;
        adapter.updateList(ld.getFileList());
        mActivity.mPath = ld.getPath();
        mActivity.mFileList = new ArrayList<>(ld.getFileList());
        updateLayout(mActivity.mMediaControl.onViewAllLayout(), ld.isForceTop());
        mActivity.invalidateOptionsMenu();
        mActivity.updateSpinner(ld.getPath());
        mActivity.enableFabEdit(true);
        adapter.notifyDataSetChanged();
        BrowserData.getInstance(getTabPosition()).updateFileList(adapter.getList());
        StoreJetCloudData.getInstance(getTabPosition()).setPath(mActivity.mPath);

    }

    private void updateUIIndex(Loader loader) {
        BrowserRecyclerAdapter adapter = (BrowserRecyclerAdapter) getRecyclerViewAdapter();
        TwonkyIndexLoader ld = (TwonkyIndexLoader) loader;
        adapter.updateList(ld.getFileList());
        mActivity.mPath = ld.getPath();
        mActivity.mFileList = new ArrayList<>(ld.getFileList());
        updateLayout(LayoutType.LIST, true);
        mActivity.invalidateOptionsMenu();
        mActivity.updateSpinner(ld.getPath());
        mActivity.enableFabEdit(false);
        adapter.notifyDataSetChanged();
        BrowserData.getInstance(getTabPosition()).updateFileList(adapter.getList());
        StoreJetCloudData.getInstance(getTabPosition()).setPath(mActivity.mPath);

    }

    private void updateUIViewAll(Loader loader) {
        BrowserRecyclerAdapter adapter = (BrowserRecyclerAdapter) getRecyclerViewAdapter();
        TwonkyViewAllLoader ld = (TwonkyViewAllLoader) loader;
        mActivity.mPath = ld.getPath();

        if (ld.getStartIndex() == 0) {
            adapter.updateList(ld.getFileList());
            mActivity.mFileList = new ArrayList<>(ld.getFileList());
            updateLayout(mActivity.mMediaControl.onViewAllLayout(), false);
            mActivity.invalidateOptionsMenu();
            mActivity.updateSpinner(ld.getPath());
            mActivity.enableFabEdit(true);

        } else {  // lazy loading case
            adapter.addFiles(ld.getFileList());
            mActivity.mFileList.addAll(ld.getFileList());

            if (mActivity.mIsSelectAll) {
                mActivity.updateSelectAll();
            }
        }

        int nextLoadingIndex = ld.nextLoadingIndex();
        StoreJetCloudData.getInstance(getTabPosition()).setLoadingIndex(nextLoadingIndex);
        StoreJetCloudData.getInstance(getTabPosition()).setListSize(adapter.getList().size());

        adapter.notifyDataSetChanged();
        BrowserData.getInstance(getTabPosition()).updateFileList(adapter.getList());
        StoreJetCloudData.getInstance(getTabPosition()).setPath(mActivity.mPath);

    }

    @Override
    public void onLoaderReset(Loader loader) {
        //TODO
    }

    @Override
    protected void onPageChanged(int lastPosition, int currentPosition) {
        mActivity.closeEditorMode();
        updateViewReference();
        mProgressView.setVisibility(View.INVISIBLE);

        stopRunningLoader();
        boolean isFirstSwitch = getRecyclerViewAdapter().getItemCount() == 0;
        if (isFirstSwitch) {
            mActivity.mMediaControl.onPageChanged();
        }
    }

    @Override
    public void onFinishCreateView(int position) {
        if (position == 0 && getTabPosition() == 0) {          // TODO
            updateViewReference();
        }

        if (position != BrowserData.ALL.getTabPosition()) {
            addScrollListener(position);
        }

    }

    void stopRunningLoader() {
        boolean hasRunningLoaders = getLoaderManager().hasRunningLoaders();
        if (hasRunningLoaders) {
            getLoaderManager().destroyLoader(mRunningLoaderID);
            mRunningLoaderID = -1;
        }
    }

    String getSystemPath() {
        return mSystemPath;
    }

    private void updateViewReference() {
        mActivity.collapseSearchView();
        MediaFragment fragment = getCurrentFragment();
        mActivity.onRecyclerViewInit(fragment);
        mActivity.onProgressViewInit(this);
    }

    private void addScrollListener(int position) {
        MediaFragment fragment = getFragment(position);
        if (fragment != null) {
            fragment.getRecyclerView().addOnScrollListener(new RecyclerScrollListener() {
                @Override
                public void onLoadMore(int current_page) {
                    if (isLoadMoreEnabled()) {
                        mActivity.mMediaControl.lazyLoad();
                    }
                }

                private boolean isLoadMoreEnabled() {
                    int viewPreference = StoreJetCloudData.getInstance(getTabPosition()).getViewPreference(mActivity);
                    boolean isViewAll = viewPreference == 0;
                    if (isViewAll) {
                        if (!BrowserSearchView.mIsSearchMode) {
                            return true;
                        } else {
                            this.cancelLoadMore();
                        }
                    } else {
                        this.cancelLoadMore();
                    }
                    return false;
                }
            });
        }
    }

    /**
     *
     * @param mode
     * @param isForce : true if the first visible item is the first item of RecyclerView.
     */
    private void updateLayout(LayoutType mode, boolean isForce) {
        RecyclerView.LayoutManager lm = null;
        if (isForce) {
            if (mode == LayoutType.GRID) {
                int spanCount = 3;
                lm = new GridLayoutManager(getContext(), spanCount);

            } else {
                lm = new LinearLayoutManager(getContext());
            }

        } else {
            RecyclerView.LayoutManager currentLayout = getRecyclerView().getLayoutManager();
            if (currentLayout instanceof GridLayoutManager) {
                if (mode == LayoutType.LIST) {
                    lm = new LinearLayoutManager(getContext());
                }
            } else if (currentLayout instanceof LinearLayoutManager) {
                if (mode == LayoutType.GRID) {
                    int spanCount = 3;
                    lm = new GridLayoutManager(getContext(), spanCount);
                }
            }
        }

        if (lm != null) {
            getRecyclerView().setLayoutManager(lm);
            getRecyclerView().getRecycledViewPool().clear();
            getRecyclerViewAdapter().notifyDataSetChanged();
        }
        BrowserData.getInstance(getTabPosition()).setLayout(mode);
    }

}