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
import android.widget.RelativeLayout;

import com.transcend.nas.R;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.FileManageRecyclerAdapter;
import com.transcend.nas.management.FragmentItemsLoader;

import java.util.ArrayList;

import static com.transcend.nas.R.id.viewPager;


/**
 * Created by steve_su on 2017/7/10.
 */

public abstract class FileListTabFragment extends Fragment {
    static final String TAG = FileListTabFragment.class.getSimpleName();
    private static final int GRID_PORTRAIT = 3;

    // TODO is landscape mode supported ?
    private static final int GRID_LANDSCAPE = 5;
    private Context mContext;
    private int mTabPosition;
    private ViewPager mViewPager;
    protected RelativeLayout mProgressView;
//    protected ProgressBar mProgressBar;
    protected SwipeRefreshLayout mSwipeRefreshLayout;
    protected MyPagerAdapter mPagerAdapter;
    protected LoaderManager.LoaderCallbacks<ArrayList<FileInfo>> mCallbacks;

    enum LayoutType{
        LIST, GRID
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "[Enter] onCreate");

        super.onCreate(savedInstanceState);
        mContext = getActivity();

        int i = 0;
        for(TabData tab : onTabInstance()) {
            tab.setTabPosition(i++);
        }


//        setHasOptionsMenu(true);
//        mActivity = (FileManageFragmentActivity) getActivity();
//        mCallbacks = new LoaderCallback();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "[Enter] onCreateView");
        CoordinatorLayout root = (CoordinatorLayout) inflater.inflate(R.layout.fragment_file_manage_tab, container, false);

//        if (savedInstanceState != null) {
//            Log.d(TAG, "[Enter] savedInstanceState != null");
//            mTabPosition = savedInstanceState.getInt("key_mCurrentTabPosition");
//            MediaType.getInstance(mTabPosition).init(this.getActivity());
//        }

        initProgressView(root);
        initViewPager(root);
        initTabLayout(root);
//        initMediaTypeInstance();
//        updateViewReference(MediaType.ALL);

        //LinearLayoutManager manager = new LinearLayoutManager(getContext());
        //manager.setOrientation(LinearLayoutManager.HORIZONTAL);
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
//        Log.d(TAG, "[Enter] onViewCreated");

        super.onViewCreated(view, savedInstanceState);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
//                load(mTabPosition, false);
            }
        });
    }

    private void initProgressView(CoordinatorLayout root) {
        mSwipeRefreshLayout = (SwipeRefreshLayout) root.findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent);
        mProgressView = (RelativeLayout) root.findViewById(R.id.loading_container);
//        mProgressBar = (ProgressBar) root.findViewById(R.id.main_progress_bar);
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
        TabData[] tab = onTabInstance();
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            tabLayout.getTabAt(i).setIcon(tab[i].getIconId());
        }
    }

    protected int getTabPosition() {
        return mViewPager.getCurrentItem();
    }

    protected ViewFragment getCurrentFragment() {
        return mPagerAdapter.getRegisteredFragment(mViewPager.getCurrentItem());
    }

    protected RecyclerView.Adapter getRecyclerViewAdapter() {
        return getCurrentFragment().getRecyclerView().getAdapter();
    }

//    protected void onPageChanged(int lastPosition, int currentPosition) {
//        if (getLoaderManager().hasRunningLoaders()) {
//            mProgressView.setVisibility(View.INVISIBLE);
//        }
//
////        updateViewReference(currentPosition);
//
////        boolean isFirstSwitch = mActivity.mRecyclerAdapter.getItemCount() == 0;
//        boolean isFirstSwitch = getRecyclerViewAdapter().getItemCount() == 0;
//        if (isFirstSwitch) {
////                Log.d(TAG, "[Enter] isPositionChanged && isFirstSwitch");
//            mProgressView.setVisibility(View.VISIBLE);
//            load(currentPosition, false);
//        }
//    }

    protected abstract TabData[] onTabInstance();
    protected abstract void onLoadMoreItems(int position);
    protected abstract void onPageChanged(int lastPosition, int currentPosition);

    private class MyPagerAdapter extends FragmentPagerAdapter {
        SparseArray<ViewFragment> mRegisteredFragments = new SparseArray<>();

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Log.d(TAG, "[Enter] instantiateItem() position: "+ position);
            ViewFragment fragment = (ViewFragment) super.instantiateItem(container, position);
            mRegisteredFragments.put(position, fragment);
            return fragment;
        }

        @Override
        public android.support.v4.app.Fragment getItem(final int position) {
            Log.d(TAG, "[Enter] getItem() position: "+ position);
            return ViewFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return onTabInstance().length;
        }

        /**
         * This method is called when you call notifyDataSetChanged() on your ViewPagerAdapter.
         * @param object
         * @return
         */
        @Override
        public int getItemPosition(Object object) {
            Log.d(TAG, "[Enter] getItemPosition");

//            if (object instanceof ViewFragment) {
//                ((ViewFragment) object).updateView();
//            }
            return super.getItemPosition(object);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            Log.d(TAG, "[Enter] destroyItem position: "+ position);
            mRegisteredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        ViewFragment getRegisteredFragment(int key) {
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

//            for (MediaType m : MediaType.values()) {
//                Log.d(TAG, "instance:  "+ m.toString()+ " mFileList size: "+ m.getFileList().size());
//            }
//
//            boolean isPositionChanged = (mTabPosition != position);
//            if (isPositionChanged) {
            onPageChanged(mTabPosition, position);
//            }

            mTabPosition = position;

//            mAdapter.notifyDataSetChanged();
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


//    private class LoaderCallback implements LoaderManager.LoaderCallbacks<ArrayList<FileInfo>> {
//
//        @Override
//        public Loader<ArrayList<FileInfo>> onCreateLoader(int id, Bundle args) {
//            return new FragmentItemsLoader(getContext(), args);
//        }
//
//        @Override
//        public void onLoadFinished(Loader<ArrayList<FileInfo>> loader, ArrayList<FileInfo> data) {
//            Log.d(TAG, "\n\n[Enter] onLoadFinished data.size(): "+ data.size());
//            boolean isLazyLoading = ((FragmentItemsLoader) loader).isLazyLoading();
//            int position = ((FragmentItemsLoader) loader).getPosition();
//            Log.d(TAG, "instance: "+ TabData.getInstance(position).toString());
//
//            FileManageRecyclerAdapter adapter = (FileManageRecyclerAdapter) getRecyclerViewAdapter();
//            adapter.updateList(data);
//            adapter.notifyDataSetChanged();
//
////            log(true);
//            TabData.getInstance(position).updateFileList(data, !isLazyLoading);
////            log(false);
//
////            mActivity.mRecyclerAdapter.notifyDataSetChanged();
//
//            TabData.getInstance(position).setStartIndex(((FragmentItemsLoader) loader).nextStartIndex());
//
//            mProgressView.setVisibility(View.INVISIBLE);
//            mSwipeRefreshLayout.setRefreshing(false);
//        }
//
//        @Override
//        public void onLoaderReset(Loader<ArrayList<FileInfo>> loader) {
//            //mAdapter.update(null);
//            Log.d(TAG, "[Enter] onLoaderReset=======================================================");
//
//        }

//        private void log(boolean isBefore) {
//            String tag = isBefore ? "before" : "after";
//            Log.d(TAG, "mActivity.mFileList.size() "+ tag+ ": "+ mActivity.mFileList.size());
//            Log.d(TAG, "mActivity.mRecyclerAdapter.getItemCount() "+ tag+ ": "+ mActivity.mRecyclerAdapter.getItemCount());
//            Log.d(TAG, "MediaType.getInstance(mTabPosition).getFileList().size() "+ tag+ ": "+ TabData.getInstance(getTabPosition()).getFileList().size());
//        }
//
//    }


}
