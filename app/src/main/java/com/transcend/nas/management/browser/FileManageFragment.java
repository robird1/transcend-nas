package com.transcend.nas.management.browser;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.transcend.nas.R;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.FileManageRecyclerAdapter;

import java.util.ArrayList;

/**
 * Created by steve_su on 2017/6/7.
 */

public class FileManageFragment extends AbstractFileManageFragment {
    static final String TAG = FileManageFragment.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "[Enter] onCreate");

        super.onCreate(savedInstanceState);
//        setHasOptionsMenu(true);
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
        super.onCreateView(root);

        mActivity.mRecyclerAdapter = new FileManageRecyclerAdapter(inflater.getContext(), new ArrayList<FileInfo>());
        mActivity.mRecyclerView = (RecyclerView) root.findViewById(R.id.recycler_view);
        mActivity.mRecyclerEmptyView = (LinearLayout) root.findViewById(R.id.main_recycler_empty_view);

        mActivity.onRecyclerViewInit();

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
