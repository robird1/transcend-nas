package com.transcend.nas.management.browser_framework;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.transcend.nas.R;
import com.transcend.nas.management.FileManageRecyclerAdapter;

import java.util.ArrayList;

import static com.transcend.nas.management.browser_framework.Browser.LayoutType;

/**
 * Created by steve_su on 2017/7/11.
 */

public class MediaFragment extends Fragment {
    private static final String TAG = MediaFragment.class.getSimpleName();
    private int mPosition;
    private RecyclerView mRecyclerView;
    private LinearLayout mRecyclerEmptyView;

    static MediaFragment newInstance(int position) {
        MediaFragment f = new MediaFragment();
        Bundle args = new Bundle();
        args.putInt("position", position);
        f.setArguments(args);
        return f;
    }

    public int getPosition() {
        return mPosition;
    }

    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    public LinearLayout getRecyclerEmptyView() {
        return mRecyclerEmptyView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPosition = getArguments() != null ? getArguments().getInt("position") : -1;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "[Enter] onCreateView instance: "+ BrowserData.getInstance(mPosition).toString());

        View rootView = inflater.inflate(R.layout.fragment_pager, null);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        mRecyclerEmptyView = (LinearLayout) rootView.findViewById(R.id.main_recycler_empty_view);
        RecyclerView.LayoutManager lm = initLayoutManager(mPosition);
        mRecyclerView.setLayoutManager(lm);

        ArrayList data = BrowserData.getInstance(mPosition).getFileList();

        // TODO FileManageRecyclerAdapter
        mRecyclerView.setAdapter(new FileManageRecyclerAdapter(getContext(), data));

        Browser browser = ((Browser) getParentFragment());
        browser.onNotifyState(mPosition);

        return rootView;
    }

    private RecyclerView.LayoutManager initLayoutManager(int position) {
        LayoutType mode = BrowserData.getInstance(position).getViewMode(getContext());
        if (mode == LayoutType.LIST) {
            return new LinearLayoutManager(getContext());
        } else {
            int orientation = getActivity().getResources().getConfiguration().orientation;
//            int spanCount = (orientation == Configuration.ORIENTATION_PORTRAIT) ? GRID_PORTRAIT : GRID_LANDSCAPE;
            int spanCount = 3;
            return new GridLayoutManager(getContext(), spanCount);
        }
    }

}
