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
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.FileManageRecyclerAdapter;
import com.transcend.nas.management.FileManageScrollListener;

import java.util.ArrayList;

import static com.transcend.nas.management.browser_framework.FileListTabFragment.LayoutType;

/**
 * Created by steve_su on 2017/7/11.
 */

public class ViewFragment extends Fragment {
    private static final String TAG = ViewFragment.class.getSimpleName();
    private int mPosition;
    private RecyclerView mRecyclerView;

    static ViewFragment newInstance(int position) {
        ViewFragment f = new ViewFragment();
        Bundle args = new Bundle();
        args.putInt("position", position);
        f.setArguments(args);
        return f;
    }

    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPosition = getArguments() != null ? getArguments().getInt("position") : -1;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "[Enter] onCreateView mPosition: "+ mPosition);

        View rootView = inflater.inflate(R.layout.fragment_pager, null);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        LinearLayout emptyView = (LinearLayout) rootView.findViewById(R.id.main_recycler_empty_view);
        mRecyclerView.addOnScrollListener(new FileManageScrollListener() {
            @Override
            public void onLoadMore(int current_page) {
                if (getParentFragment() != null) {
                    ((FileListTabFragment) getParentFragment()).onLoadMoreItems(mPosition);
                }
            }
        });

        RecyclerView.LayoutManager lm = initLayoutManager(mPosition);
        mRecyclerView.setLayoutManager(lm);

        // TODO FileManageRecyclerAdapter
        mRecyclerView.setAdapter(new FileManageRecyclerAdapter(getContext(), new ArrayList<FileInfo>()));
        return rootView;
    }

    @Override
    public void onStart() {
        Log.d(TAG, "[Enter] onStart mPosition: "+ mPosition);
        super.onStart();
//        getLoaderManager().restartLoader(TAB_LOADER_ID, getArguments(), mCallbacks);
    }

    @Override
    public void onResume() {
        Log.d(TAG, "[Enter] onResume mPosition: "+ mPosition);
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "[Enter] onPause mPosition: "+ mPosition);
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "[Enter] onStop mPosition: "+ mPosition);
        super.onStop();
//        getLoaderManager().destroyLoader(TAB_LOADER_ID);
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "[Enter] onDestroyView mPosition: "+ mPosition);
//        for (MediaType f : MediaType.values()) {
//            ViewGroup group = (ViewGroup) f.getRootView().getParent();
//            if (group != null) {
//                group.removeView(f.getRootView());
//            }
//        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "[Enter] onDestroy mPosition: "+ mPosition);
//        for (MediaType f: MediaType.values()) {
//            f.clear();
//        }
        super.onDestroy();
    }

    private RecyclerView.LayoutManager initLayoutManager(int position) {
//            int mode = NASPref.getViewMode(mContext, mViewModeKey);
        LayoutType mode = TabData.getInstance(position).getViewMode();
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
