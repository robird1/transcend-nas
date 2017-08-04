package com.transcend.nas.management.browser_framework;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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

public abstract class Browser extends Fragment implements LoaderManager.LoaderCallbacks<ArrayList> {
    static final String TAG = Browser.class.getSimpleName();
    public static final int VIEW_ALL = 168;
    public static final int VIEW_ALBUM = 169;

    private static final int GRID_PORTRAIT = 3;

    // TODO is landscape mode supported ?
    private static final int GRID_LANDSCAPE = 5;
    private Context mContext;
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
//        Log.d(TAG, "[Enter] onCreate");
        super.onCreate(savedInstanceState);
        mContext = getActivity();

        int i = 0;
        for(BrowserData tab : onTabInstance()) {
            tab.setTabPosition(i++);
        }

    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
//        Log.d(TAG, "[Enter] onCreateView");
        CoordinatorLayout root = (CoordinatorLayout) inflater.inflate(R.layout.fragment_file_manage_tab, container, false);

//        if (savedInstanceState != null) {
//            Log.d(TAG, "[Enter] savedInstanceState != null");
//            mTabPosition = savedInstanceState.getInt("key_mCurrentTabPosition");
//            MediaType.getInstance(mTabPosition).init(this.getActivity());
//        }

        initProgressView(root);
        initViewPager(root);
        initTabLayout(root);
//        updateViewReference(MediaType.ALL);

        //LinearLayoutManager manager = new LinearLayoutManager(getContext());
        //manager.setOrientation(LinearLayoutManager.HORIZONTAL);
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        return root;
    }

//    @Override
//    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
////        Log.d(TAG, "[Enter] onViewCreated");
//
//        super.onViewCreated(view, savedInstanceState);
//        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
//            @Override
//            public void onRefresh() {
//                Log.d(TAG, "\n\n[Enter] onRefresh");
//                load(mTabPosition);
//            }
//        });
//        mProgressView.setVisibility(View.INVISIBLE);
//    }
//
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
//        Log.d(TAG, "[Enter] onActivityCreated");
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
//        Log.d(TAG, "[Enter] onStart");

        super.onStart();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "[Enter] onDestroy");
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

    protected MediaFragment getCurrentFragment() {
        return mPagerAdapter.getRegisteredFragment(mViewPager.getCurrentItem());
    }

    protected RecyclerView getRecyclerView() {
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
    }

    protected void onPageChanged(int lastPosition, int currentPosition) {
        mProgressView.setVisibility(View.INVISIBLE);

//        if (getLoaderManager().hasRunningLoaders()) {
//            Log.d(TAG, "[Enter] destroyLoader VIEW_ALL");
//
//            // TODO
//            getLoaderManager().destroyLoader(VIEW_ALL);
//        }
//
//        boolean isFirstSwitch = getRecyclerViewAdapter().getItemCount() == 0;
//        if (isFirstSwitch) {
//            Log.d(TAG, "[Enter] isFirstSwitch");
//            mProgressView.setVisibility(View.VISIBLE);
//            load(currentPosition);
//        }
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

    @Override
    public void onLoadFinished(Loader loader, ArrayList data) {
        Log.d(TAG, "[Enter] onLoadFinished data.size(): " + data.size());

        FileManageRecyclerAdapter adapter = (FileManageRecyclerAdapter) getRecyclerViewAdapter();
        if (adapter.getItemCount() > 0) {
            Log.d(TAG, "[Enter] adapter.addFiles");
            adapter.addFiles(data);
            BrowserData.getInstance(getTabPosition()).updateFileList(data, false);
        } else {
            Log.d(TAG, "[Enter] adapter.updateList");
            adapter.updateList(data);
            BrowserData.getInstance(getTabPosition()).updateFileList(data, true);
        }
        adapter.notifyDataSetChanged();

        mProgressView.setVisibility(View.INVISIBLE);
        mSwipeRefreshLayout.setRefreshing(false);
    }

//    public void load(int position) {
//        Log.d(TAG, "[Enter] load(int position)");
//        FileManageRecyclerAdapter adapter = (FileManageRecyclerAdapter) getRecyclerViewAdapter();
//        if (adapter.getItemCount() > 0) {
//            Log.d(TAG, "[Enter] adapter.updateList(new ArrayList<FileInfo>())");
//
//            adapter.updateList(new ArrayList<FileInfo>());
//            adapter.notifyDataSetChanged();
//        }
//
////        load(position, false);
//    }

//    protected void load(int position, boolean isLazyLoading) {
//        if (isLazyLoading) {
//            mProgressView.setVisibility(View.VISIBLE);
//        }
//    }

    protected abstract BrowserData[] onTabInstance();


    private class MyPagerAdapter extends FragmentPagerAdapter {
        SparseArray<MediaFragment> mRegisteredFragments = new SparseArray<>();

        MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
//            Log.d(TAG, "[Enter] instantiateItem() position: "+ position);
            MediaFragment fragment = (MediaFragment) super.instantiateItem(container, position);
            mRegisteredFragments.put(position, fragment);
            return fragment;
        }

        @Override
        public android.support.v4.app.Fragment getItem(final int position) {
//            Log.d(TAG, "[Enter] getItem() position: "+ position);
            return MediaFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return onTabInstance().length;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
//            Log.d(TAG, "[Enter] destroyItem position: "+ position);
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
            Log.d(TAG, "\n\n[Enter] onPageSelected position: "+ position);

            onPageChanged(mTabPosition, position);
            mTabPosition = position;
        }

        @Override
        public void onPageScrollStateChanged(int state) {
//            Log.d(TAG, "[Enter] onPageScrollStateChanged");
            enableDisableSwipeRefresh(state == ViewPager.SCROLL_STATE_IDLE);
        }

        private void enableDisableSwipeRefresh(boolean enable) {
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setEnabled(enable);
            }
        }

    }

}
