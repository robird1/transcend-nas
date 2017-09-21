package com.transcend.nas.management.browser;

import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.transcend.nas.R;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.browser_framework.BrowserData;

import java.net.URLEncoder;
import java.util.ArrayList;

import static com.transcend.nas.management.browser.RequestAction.SMB_SEARCH;
import static com.transcend.nas.management.browser.RequestAction.TWONKY_CUSTOM;
import static com.transcend.nas.management.browser.RequestAction.TWONKY_VIEW_ALL;

/**
 * Created by steve_su on 2017/9/18.
 */

public class FragmentSearch extends Fragment implements LoaderManager.LoaderCallbacks<Boolean>,
        BrowserActivity.FragmentLoader {

    private BrowserActivity mActivity;
    private RelativeLayout mProgressView;
    private TextView mRecyclerEmptyView;
    private int mRunningLoaderID = -1;
    private ArrayList<FileInfo> mOriginList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (BrowserActivity) getActivity();
        mOriginList = mActivity.mFileList;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        CoordinatorLayout root = (CoordinatorLayout) inflater.inflate(
                R.layout.fragment_file_manage_search, container, false);
        mProgressView = (RelativeLayout) root.findViewById(R.id.loading_container);
        mRecyclerEmptyView = (TextView) root.findViewById(R.id.recycler_empty_view);
        mActivity.mRecyclerAdapter = new BrowserRecyclerAdapter(inflater.getContext(), new ArrayList<FileInfo>());
        mActivity.mRecyclerView = (RecyclerView) root.findViewById(R.id.recycler_view);
        mActivity.mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mActivity.mRecyclerView.setAdapter(mActivity.mRecyclerAdapter);
        mActivity.mRecyclerAdapter.setOnRecyclerItemCallbackListener(mActivity);
        mActivity.mProgressView = mProgressView;
        mActivity.mProgressBar = (ProgressBar) root.findViewById(R.id.main_progress_bar);

        return root;
    }

    @Override
    public void onDestroy() {
        FragmentBrowser fragment =
                (FragmentBrowser) mActivity.getSupportFragmentManager().findFragmentByTag(FragmentBrowser.TAG);
        mActivity.mRecyclerView = fragment.getRecyclerView();
        mActivity.mRecyclerAdapter = (BrowserRecyclerAdapter) fragment.getRecyclerViewAdapter();
        mActivity.mProgressView = fragment.mProgressView;
        mActivity.mProgressBar = fragment.mProgressBar;
        updateFileList(fragment);
        super.onDestroy();
    }

    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        Loader loader = null;
        if (id == TWONKY_VIEW_ALL)
            loader = new TwonkyViewAllLoader(getContext(), args);
        else if (id == TWONKY_CUSTOM)
            loader = new TwonkyCustomLoader(getContext(), args);
        else if (id == SMB_SEARCH)
            loader = new SambaSearchLoader(getContext(), args, mOriginList);

        if (loader != null) {
            mRunningLoaderID = id;
        }
        return loader;
    }

    @Override
    public void onLoadFinished(Loader loader, Boolean isSuccess) {
        mProgressView.setVisibility(View.INVISIBLE);

        if (!isSuccess) {
            Toast.makeText(mActivity, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<FileInfo> list;
        if (loader instanceof TwonkyViewAllLoader) {
            TwonkyViewAllLoader ld = (TwonkyViewAllLoader) loader;
            list = ld.getFileList();

        } else if (loader instanceof TwonkyCustomLoader) {
            TwonkyCustomLoader ld = (TwonkyCustomLoader) loader;
            list = ld.getFileList();

        } else if (loader instanceof SambaSearchLoader) {
            SambaSearchLoader ld = (SambaSearchLoader) loader;
            list = ld.getFileList();

        } else {
            return;
        }

        mActivity.mRecyclerAdapter.updateList(list);
        mActivity.mFileList = new ArrayList<>(list);
        mActivity.mRecyclerAdapter.notifyDataSetChanged();
        checkEmptyView(list);
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {

    }

    @Override
    public void startLoader(int loaderID, Bundle args) {
        args.putString("system_path", URLEncoder.encode(mActivity.mSystemPath));
        stopRunningLoader();
        getLoaderManager().restartLoader(loaderID, args, this).forceLoad();
    }

    @Override
    public void stopRunningLoader() {
        boolean hasRunningLoaders = getLoaderManager().hasRunningLoaders();
        if (hasRunningLoaders) {
            getLoaderManager().destroyLoader(mRunningLoaderID);
            mRunningLoaderID = -1;
        }
    }

    private void checkEmptyView(ArrayList list) {
        if (list.size() == 0) {
            mRecyclerEmptyView.setVisibility(View.VISIBLE);
        } else {
            mRecyclerEmptyView.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * If current view mode is photo, music or video, then update file list after closing search result;
     * otherwise update file list by the SmbFileListLoader if current view mode is folder
     * (triggered when user clicks a folder from the search result).
     *
     * @param fragment
     */
    private void updateFileList(FragmentBrowser fragment) {
        if (mActivity.mTabPosition != BrowserData.ALL.getTabPosition()) {
            mActivity.mFileList = ((BrowserRecyclerAdapter) fragment.getRecyclerViewAdapter()).getList();
        }
    }

}
