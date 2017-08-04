package com.transcend.nas.management.browser;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.transcend.nas.R;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.FileManageRecyclerAdapter;

import java.util.ArrayList;

/**
 * Created by steve_su on 2017/6/7.
 */

public class RootFragment extends Fragment {
    static final String TAG = RootFragment.class.getSimpleName();
    protected BrowserActivity mActivity;
    protected RelativeLayout mProgressView;
    protected ProgressBar mProgressBar;
    protected SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "[Enter] onCreate");
        super.onCreate(savedInstanceState);
        mActivity = (BrowserActivity) getActivity();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "[Enter] onSaveInstanceState");
        super.onSaveInstanceState(outState);
//        outState.putInt("key_mCurrentTabPosition", mCurrentTabPosition);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "[Enter] onCreateView");

        CoordinatorLayout root = (CoordinatorLayout) inflater.inflate(R.layout.fragment_file_manage, container, false);
        mSwipeRefreshLayout = (SwipeRefreshLayout) root.findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent);
        mProgressView = (RelativeLayout) root.findViewById(R.id.loading_container);
        mProgressBar = (ProgressBar) root.findViewById(R.id.main_progress_bar);


        mActivity.mRecyclerAdapter = new FileManageRecyclerAdapter(inflater.getContext(), new ArrayList<FileInfo>());
        mActivity.mRecyclerView = (RecyclerView) root.findViewById(R.id.recycler_view);
        mActivity.mRecyclerEmptyView = (LinearLayout) root.findViewById(R.id.main_recycler_empty_view);

        mActivity.onRecyclerViewInit();
        mActivity.onProgressViewInit(this);

        //LinearLayoutManager manager = new LinearLayoutManager(getContext());
        //manager.setOrientation(LinearLayoutManager.HORIZONTAL);
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "[Enter] onViewCreated");
        super.onViewCreated(view, savedInstanceState);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mActivity.doRefresh();
            }
        });

    }


}
