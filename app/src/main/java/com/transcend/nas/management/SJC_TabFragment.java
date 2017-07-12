package com.transcend.nas.management;

import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.transcend.nas.management.browser_framework.FileListTabFragment;
import com.transcend.nas.management.browser_framework.TabData;

import java.util.ArrayList;


/**
 * Created by steve_su on 2017/7/10.
 */

public class SJC_TabFragment extends FileListTabFragment {
    static final String TAG = SJC_TabFragment.class.getSimpleName();
    static final int TAB_LOADER_ID = 168;
    private LoaderManager.LoaderCallbacks<ArrayList<FileInfo>> mCallbacks;
    private FileManageFragmentActivity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "[Enter] onCreate");

        super.onCreate(savedInstanceState);

        mActivity = (FileManageFragmentActivity) getActivity();

//        setHasOptionsMenu(true);
        mCallbacks = new LoaderCallback();
    }


    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        CoordinatorLayout root = (CoordinatorLayout) super.onCreateView(inflater, container, savedInstanceState);

//        initMediaTypeInstance();
//        updateViewReference(TabData.ALL);

        return root;
    }

    @Override
    protected TabData[] onTabInstance() {
        TabData[] tabs = {TabData.ALL, TabData.PHOTO, TabData.MUSIC, TabData.VIDEO};
        return tabs;
    }

    protected void onPageChanged(int lastPosition, int currentPosition) {
        if (getLoaderManager().hasRunningLoaders()) {
            mProgressView.setVisibility(View.INVISIBLE);
        }

//        updateViewReference(currentPosition);

//        boolean isFirstSwitch = mActivity.mRecyclerAdapter.getItemCount() == 0;
        boolean isFirstSwitch = getRecyclerViewAdapter().getItemCount() == 0;
        if (isFirstSwitch) {
                Log.d(TAG, "[Enter] isPositionChanged && isFirstSwitch");
            mProgressView.setVisibility(View.VISIBLE);
            load(currentPosition, false);
        }
    }

    @Override
    protected void onLoadMoreItems(int position) {
//        load(position, true);
    }

    //    private void updateViewReference(int position) {
//        updateViewReference(TabData.getInstance(position));
//    }
//
//    private void updateViewReference(TabData instance) {
//        mActivity.onRecyclerViewInit(instance);
//    }
//
    private void load(int position, boolean isLazyLoading) {
        if (position == TabData.ALL.getTabPosition()) {
            mActivity.doRefresh();
        } else {
            TabData instance = TabData.getInstance(position);
            Bundle args = new Bundle();
            int startIndex = isLazyLoading ? instance.getStartIndex() : 0;
            args.putInt("start", startIndex);
            args.putInt("type", getTwonkyType(position));
            args.putBoolean("is_lazy_loading", isLazyLoading);
            args.putInt("position", position);
            args.putString("fragment_type", instance.toString());
            getLoaderManager().restartLoader(TAB_LOADER_ID, args, mCallbacks);
        }
    }

    private int getTwonkyType(int position) {
        TabData instance = TabData.getInstance(position);
        if (instance == TabData.ALL) {
            return 0;
        } else if (instance == TabData.PHOTO) {
            return 2;

        } else if (instance == TabData.MUSIC) {
            return 1;

        } else if (instance == TabData.VIDEO) {
            return 3;
        }
        return 0;
    }


    private class LoaderCallback implements LoaderManager.LoaderCallbacks<ArrayList<FileInfo>> {

        @Override
        public Loader<ArrayList<FileInfo>> onCreateLoader(int id, Bundle args) {
            return new FragmentItemsLoader(getContext(), args);
        }

        @Override
        public void onLoadFinished(Loader<ArrayList<FileInfo>> loader, ArrayList<FileInfo> data) {
            Log.d(TAG, "\n\n[Enter] onLoadFinished data.size(): " + data.size());
            boolean isLazyLoading = ((FragmentItemsLoader) loader).isLazyLoading();
            int position = ((FragmentItemsLoader) loader).getPosition();
            Log.d(TAG, "instance: " + TabData.getInstance(position).toString());

            FileManageRecyclerAdapter adapter = (FileManageRecyclerAdapter) getRecyclerViewAdapter();
            adapter.updateList(data);
            adapter.notifyDataSetChanged();

//            log(true);
            TabData.getInstance(position).updateFileList(data, !isLazyLoading);
//            log(false);

//            mActivity.mRecyclerAdapter.notifyDataSetChanged();

            TabData.getInstance(position).setStartIndex(((FragmentItemsLoader) loader).nextStartIndex());

            mProgressView.setVisibility(View.INVISIBLE);
            mSwipeRefreshLayout.setRefreshing(false);
        }

        @Override
        public void onLoaderReset(Loader<ArrayList<FileInfo>> loader) {
            //mAdapter.update(null);
            Log.d(TAG, "[Enter] onLoaderReset=======================================================");

        }
    }

}