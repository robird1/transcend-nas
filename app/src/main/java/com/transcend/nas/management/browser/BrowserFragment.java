package com.transcend.nas.management.browser;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;

import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.FileManageRecyclerAdapter;
import com.transcend.nas.management.browser_framework.Browser;
import com.transcend.nas.management.browser_framework.BrowserData;
import com.transcend.nas.management.browser_framework.MediaFragment;

import java.util.ArrayList;


/**
 * Created by steve_su on 2017/7/10.
 */

public class BrowserFragment extends Browser implements Browser.StateMonitor {
    static final String TAG = BrowserFragment.class.getSimpleName();
    private BrowserActivity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
//        Log.d(TAG, "[Enter] onCreate");
        super.onCreate(savedInstanceState);

        mActivity = (BrowserActivity) getActivity();

        setStateMonitor(this);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
//        Log.d(TAG, "[Enter] onViewCreated");
        super.onViewCreated(view, savedInstanceState);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d(TAG, "\n\n[Enter] onRefresh");
                load(getTabPosition());
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
        return mActivity.mMediaControl.onCreateLoader(id, args);
    }

    @Override
    public void onLoadFinished(Loader loader, ArrayList data) {
        super.onLoadFinished(loader, data);

        mActivity.mMediaControl.onLoadFinished(loader, data);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        Log.d(TAG, "[Enter] onLoaderReset");

    }

    public void load(int position) {
        Log.d(TAG, "[Enter] load(int position)");
        FileManageRecyclerAdapter adapter = (FileManageRecyclerAdapter) getRecyclerViewAdapter();
        if (adapter.getItemCount() > 0) {
            adapter.updateList(new ArrayList<FileInfo>());
            adapter.notifyDataSetChanged();
        }

        mActivity.mMediaControl.load(position);
    }

    @Override
    protected void onPageChanged(int lastPosition, int currentPosition) {
        updateViewReference();
        super.onPageChanged(lastPosition, currentPosition);
//        updateViewReference();

        if (getLoaderManager().hasRunningLoaders()) {
            Log.d(TAG, "[Enter] destroyLoader VIEW_ALL");

            // TODO
            getLoaderManager().destroyLoader(VIEW_ALL);
        }

        boolean isFirstSwitch = getRecyclerViewAdapter().getItemCount() == 0;
        if (isFirstSwitch) {
            Log.d(TAG, "[Enter] isFirstSwitch");
            mProgressView.setVisibility(View.VISIBLE);
            load(currentPosition);
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
//        checkFirstSubFragment(position);
        if (position == 0 && getTabPosition() == 0) {
            updateViewReference();
        }

//        getRecyclerView().addOnScrollListener(new FileManageScrollListener() {
//            @Override
//            public void onLoadMore(int current_page) {
//                Log.d(TAG, "[Enter] onLoadMore fragment: "+ getCurrentFragment().getPosition());
//                load(getTabPosition(), true);
//            }
//        });

    }

    public void viewALL() {
        clearData();

        StoreJetCloudData instance = StoreJetCloudData.getInstance(getTabPosition());
        Bundle args = new Bundle();
        args.putInt("start", 0);
        args.putInt("type", instance.getTwonkyType());
        getLoaderManager().restartLoader(VIEW_ALL, args, this);
    }

    public void lazyLoad() {
        StoreJetCloudData instance = StoreJetCloudData.getInstance(getTabPosition());
        Bundle args = new Bundle();
        int startIndex = instance.getLoadingIndex();
        Log.d(TAG, "startIndex: "+ startIndex);
        args.putInt("start", startIndex);
        args.putInt("type", instance.getTwonkyType());
        getLoaderManager().restartLoader(VIEW_ALL, args, this);
    }

}