package com.transcend.nas.management.browser;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.transcend.nas.R;

/**
 * Created by steve_su on 2017/6/8.
 */

public class AbstractFileManageFragment extends Fragment {
    protected SJC_FileManageActivity mActivity;
    protected RelativeLayout mProgressView;
    protected ProgressBar mProgressBar;
    protected SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (SJC_FileManageActivity) getActivity();
    }

    protected void onCreateView(View root) {
        mSwipeRefreshLayout = (SwipeRefreshLayout) root.findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent);
        mProgressView = (RelativeLayout) root.findViewById(R.id.loading_container);
        mProgressBar = (ProgressBar) root.findViewById(R.id.main_progress_bar);

        mActivity.onProgressViewInit(this);
    }

}
