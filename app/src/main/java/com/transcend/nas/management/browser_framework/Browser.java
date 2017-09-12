package com.transcend.nas.management.browser_framework;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.transcend.nas.R;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.FileManageRecyclerAdapter;

import java.util.ArrayList;

import static com.transcend.nas.R.id.viewPager;


/**
 * Created by steve_su on 2017/7/10.
 */

public abstract class Browser extends Fragment {
    private int mTabPosition;
    private ViewPager mViewPager;
    private ArrayList<StateMonitor> mObservers;
    public RelativeLayout mProgressView;
    public ProgressBar mProgressBar;
    public SwipeRefreshLayout mSwipeRefreshLayout;
    protected MyPagerAdapter mPagerAdapter;


    /**
     * Interface for the state of sub-fragment (i.e. All, Photo, Music, Video, File)
     */
    public interface StateMonitor {
        void onFinishCreateView(int position);
    }

    public enum LayoutType{
        LIST, GRID
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int i = 0;
        for(BrowserData tab : onTabInstance()) {
            tab.setTabPosition(i++);
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        CoordinatorLayout root = (CoordinatorLayout) inflater.inflate(R.layout.fragment_file_manage_tab, container, false);
        initProgressView(root);
        initViewPager(root);
        initTabLayout(root);
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        return root;
    }

    @Override
    public void onDestroy() {
        BrowserData.clear();
        super.onDestroy();
    }

    private void initProgressView(CoordinatorLayout root) {
        mSwipeRefreshLayout = (SwipeRefreshLayout) root.findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent);
        mProgressView = (RelativeLayout) root.findViewById(R.id.loading_container);
        mProgressBar = (ProgressBar) root.findViewById(R.id.main_progress_bar);
    }

    private void initViewPager(CoordinatorLayout root) {
        mViewPager = (ViewPager) root.findViewById(viewPager);
        mPagerAdapter = new MyPagerAdapter(getChildFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.addOnPageChangeListener(new MyPagerChangeListener());
        mViewPager.setCurrentItem(mTabPosition);
    }

    private void initTabLayout(CoordinatorLayout root) {
        TabLayout tabLayout = (TabLayout) root.findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(mViewPager);
        BrowserData[] tab = onTabInstance();
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            tabLayout.getTabAt(i).setIcon(tab[i].getIconId());
        }
    }

    public int getTabPosition() {
        return mViewPager.getCurrentItem();
    }

    public MediaFragment getCurrentFragment() {
        return mPagerAdapter.getRegisteredFragment(mViewPager.getCurrentItem());
    }

    /**
     * Get the fragment of certain tab.
     *
     * @param position
     * @return
     */
    public MediaFragment getFragment(int position) {
        return mPagerAdapter.getRegisteredFragment(position);
    }

    public RecyclerView getRecyclerView() {
        return getCurrentFragment().getRecyclerView();
    }

    public RecyclerView.Adapter getRecyclerViewAdapter() {
        return getCurrentFragment().getRecyclerView().getAdapter();
    }

    public void clearData() {
        FileManageRecyclerAdapter adapter = (FileManageRecyclerAdapter) getRecyclerViewAdapter();
        if (adapter.getItemCount() > 0) {
            adapter.updateList(new ArrayList<FileInfo>());
            adapter.notifyDataSetChanged();
        }
        BrowserData.getInstance(getTabPosition()).clearFileList();
    }

    public void setCurrentPage(int tabPosition) {
        mViewPager.setCurrentItem(tabPosition);
    }

    protected void onPageChanged(int lastPosition, int currentPosition) {
        mProgressView.setVisibility(View.INVISIBLE);
    }

    protected void setStateMonitor(StateMonitor instance) {
        if (mObservers == null) {
            mObservers = new ArrayList<>();
        }
        mObservers.add(instance);
    }

    void onNotifyState(int position) {
        for (StateMonitor o : mObservers) {
            o.onFinishCreateView(position);
        }
    }

    protected abstract BrowserData[] onTabInstance();


    private class MyPagerAdapter extends FragmentPagerAdapter {
        SparseArray<MediaFragment> mRegisteredFragments = new SparseArray<>();

        MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            MediaFragment fragment = (MediaFragment) super.instantiateItem(container, position);
            mRegisteredFragments.put(position, fragment);
            return fragment;
        }

        @Override
        public android.support.v4.app.Fragment getItem(final int position) {
            return MediaFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return onTabInstance().length;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            mRegisteredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        MediaFragment getRegisteredFragment(int key) {
            return mRegisteredFragments.get(key);
        }

    }


    private class MyPagerChangeListener implements ViewPager.OnPageChangeListener {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            onPageChanged(mTabPosition, position);
            mTabPosition = position;
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            enableDisableSwipeRefresh(state == ViewPager.SCROLL_STATE_IDLE);
        }

        private void enableDisableSwipeRefresh(boolean enable) {
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setEnabled(enable);
            }
        }

    }

}
