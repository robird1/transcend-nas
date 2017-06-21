package com.transcend.nas.management;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.transcend.nas.R;

import java.util.ArrayList;

/**
 * Created by steve_su on 2017/6/3.
 */

public class FileManageTabFragment extends AbstractFileManageFragment {
    static final String TAG = FileManageTabFragment.class.getSimpleName();
    static final int TAB_LOADER_ID = 168;
    private int mTabPosition = MediaType.ALL.getTabPosition();
    private ViewPager mViewPager;
    private LoaderManager.LoaderCallbacks<ArrayList<FileInfo>> mCallbacks;
//    private FileManageFragmentActivity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "[Enter] onCreate");

        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
//        mActivity = (FileManageFragmentActivity) getActivity();
        mCallbacks = new LoaderCallback();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "[Enter] onSaveInstanceState mTabPosition: " + mTabPosition);
        super.onSaveInstanceState(outState);
        outState.putInt("key_mCurrentTabPosition", mTabPosition);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "[Enter] onCreateView");
        CoordinatorLayout root = (CoordinatorLayout) inflater.inflate(R.layout.fragment_file_manage_tab, container, false);
        super.onCreateView(root);

        if (savedInstanceState != null) {
            Log.d(TAG, "[Enter] savedInstanceState != null");
            mTabPosition = savedInstanceState.getInt("key_mCurrentTabPosition");
            MediaType.getInstance(mTabPosition).init(this.getActivity());
        }

        initViewPager(root);
        initTabLayout(root);
        initMediaTypeInstance();
        updateViewReference(MediaType.ALL);

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
                load(mTabPosition, false);
            }
        });
    }

    @Override
    public void onStart() {
//        Log.d(TAG, "[Enter] onStart");
        super.onStart();
//        getLoaderManager().restartLoader(TAB_LOADER_ID, getArguments(), mCallbacks);
    }

    @Override
    public void onResume() {
//        Log.d(TAG, "[Enter] onResume");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "[Enter] onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "[Enter] onStop");
        super.onStop();
        getLoaderManager().destroyLoader(TAB_LOADER_ID);
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "[Enter] onDestroyView");
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
        Log.d(TAG, "[Enter] onDestroy");
        for (MediaType f: MediaType.values()) {
            f.clear();
        }
        super.onDestroy();
    }

    private void initViewPager(CoordinatorLayout root) {
        mViewPager = (ViewPager) root.findViewById(R.id.viewPager);
        MyPagerAdapter adapter = new MyPagerAdapter();
        mViewPager.setAdapter(adapter);
        mViewPager.addOnPageChangeListener(adapter);
        mViewPager.setCurrentItem(mTabPosition);
    }

    private void initTabLayout(CoordinatorLayout root) {
        TabLayout tabLayout = (TabLayout) root.findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(mViewPager);
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            int iconId = MediaType.values()[i].getIconId();
            tabLayout.getTabAt(i).setIcon(iconId);
        }
    }

    private void initMediaTypeInstance() {
        for (final MediaType f : MediaType.values()) {
            f.init(mActivity);

            if (f.getTabPosition() != MediaType.ALL.getTabPosition()) {
                addOnScrollListener(f);
            }
        }
    }

    private void addOnScrollListener(final MediaType f) {
        f.getRecyclerView().addOnScrollListener(new FileManageScrollListener() {
            @Override
            public void onLoadMore(int current_page) {
                load(f.getTabPosition(), true);
            }
        });
    }

    private void updateViewReference(int position) {
        updateViewReference(MediaType.getInstance(position));
    }

    private void updateViewReference(MediaType instance) {
        mActivity.onRecyclerViewInit(instance);
    }

    private void load(int position, boolean isLazyLoading) {
        if (position == MediaType.ALL.getTabPosition()) {
            mActivity.doRefresh();
        } else {
            MediaType instance = MediaType.getInstance(position);
            Bundle args = new Bundle();
            int startIndex = isLazyLoading ? instance.getStartIndex() : 0;
            args.putInt("start", startIndex);
            args.putInt("type", instance.getTwonkyType());
            args.putBoolean("is_lazy_loading", isLazyLoading);
            args.putInt("position", position);
            args.putString("fragment_type", instance.toString());
            getLoaderManager().restartLoader(TAB_LOADER_ID, args, mCallbacks);
        }
    }


    private class LoaderCallback implements LoaderManager.LoaderCallbacks<ArrayList<FileInfo>> {

        @Override
        public Loader<ArrayList<FileInfo>> onCreateLoader(int id, Bundle args) {
            return new FragmentItemsLoader(mActivity, args);
        }

        @Override
        public void onLoadFinished(Loader<ArrayList<FileInfo>> loader, ArrayList<FileInfo> data) {
            Log.d(TAG, "\n\n[Enter] onLoadFinished data.size(): "+ data.size());
            boolean isLazyLoading = ((FragmentItemsLoader) loader).isLazyLoading();
            int position = ((FragmentItemsLoader) loader).getPosition();
            Log.d(TAG, "instance: "+ MediaType.getInstance(position).toString());

            log(true);
            MediaType.getInstance(position).updateFileList(data, !isLazyLoading);
            log(false);

            mActivity.mRecyclerAdapter.notifyDataSetChanged();

            MediaType.getInstance(position).setStartIndex(((FragmentItemsLoader) loader).nextStartIndex());

            mProgressView.setVisibility(View.INVISIBLE);
            mSwipeRefreshLayout.setRefreshing(false);
        }

        @Override
        public void onLoaderReset(Loader<ArrayList<FileInfo>> loader) {
            //mAdapter.update(null);
            Log.d(TAG, "[Enter] onLoaderReset=======================================================");

        }

        private void log(boolean isBefore) {
            String tag = isBefore ? "before" : "after";
            Log.d(TAG, "mActivity.mFileList.size() "+ tag+ ": "+ mActivity.mFileList.size());
            Log.d(TAG, "mActivity.mRecyclerAdapter.getItemCount() "+ tag+ ": "+ mActivity.mRecyclerAdapter.getItemCount());
            Log.d(TAG, "MediaType.getInstance(mTabPosition).getFileList().size() "+ tag+ ": "+ MediaType.getInstance(mTabPosition).getFileList().size());
        }

    }


    private class MyPagerAdapter extends PagerAdapter implements ViewPager.OnPageChangeListener {

        @Override
        public int getCount() {
            return MediaType.values().length;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
//            Log.d(TAG, "[Enter] instantiateItem position: " + position);
            View rootView = MediaType.getInstance(position).getRootView();
            container.addView(rootView);
            return rootView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
//            Log.d(TAG, "[Enter] destroyItem position: " + position);
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
//            Log.d(TAG, "[Enter] onPageSelected");
            updateCurrentTab(position);
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

        private void updateCurrentTab(int position) {
            Log.d(TAG, "\n[Enter] updateCurrentTab "+ MediaType.getInstance(position).toString());
            for (MediaType m : MediaType.values()) {
                Log.d(TAG, "instance:  "+ m.toString()+ " mFileList size: "+ m.getFileList().size());
            }

            boolean isPositionChanged = (mTabPosition != position);
            mTabPosition = position;

            if (getLoaderManager().hasRunningLoaders()) {
                mProgressView.setVisibility(View.INVISIBLE);
            }

            updateViewReference(position);

            boolean isFirstSwitch = mActivity.mRecyclerAdapter.getItemCount() == 0;
            if (isPositionChanged && isFirstSwitch) {
//                Log.d(TAG, "[Enter] isPositionChanged && isFirstSwitch");
                mProgressView.setVisibility(View.VISIBLE);
                load(position, false);
            }
        }

    }

}
