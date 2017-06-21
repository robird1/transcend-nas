package com.transcend.nas.management;

import android.app.Activity;
import android.content.res.Configuration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.transcend.nas.NASPref;
import com.transcend.nas.R;

import java.util.ArrayList;

import static android.support.v7.widget.RecyclerView.LayoutManager;
import static com.transcend.nas.management.FileManageActivity.GRID_PORTRAIT;
import static com.transcend.nas.management.FileManageFragmentActivity.GRID_LANDSCAPE;
import static com.transcend.nas.management.FileManageRecyclerAdapter.LayoutType;

/**
 * Created by steve_su on 2017/6/15.
 */

enum MediaType {
    ALL(R.drawable.ic_browser_filetype_all, 0, 0, "view_mode_folder"),
    PHOTO(R.drawable.ic_browser_filetype_image, 1, 2, "view_mode_photo"),
    MUSIC(R.drawable.ic_browser_filetype_music, 2, 1, "view_mode_music"),
    VIDEO(R.drawable.ic_browser_filetype_video, 3, 3, "view_mode_video");

    private String TAG = MediaType.class.getSimpleName();
    private final int mIconId, mTabPosition, mTwonkyType;
    final String mViewModeKey;
    private View mRootView;
    private RecyclerView mRecyclerView;
    private View mRecyclerEmptyView;
    private ArrayList<FileInfo> mFileList = new ArrayList<>();
    private String mPath;
    private int mStartIndex = 0;

    MediaType(int iconId, int tabPosition, int twonkyType, String viewModeKey) {
        mIconId = iconId;
        mTabPosition = tabPosition;
        mTwonkyType = twonkyType;
        mViewModeKey = viewModeKey;
    }

    static MediaType getInstance(int position) {
        for (MediaType f : MediaType.values()) {
            if (position == f.getTabPosition()) {
                return f;
            }
        }
        return MediaType.ALL;
    }

    int getIconId() {
        return mIconId;
    }

    int getTabPosition() {
        return mTabPosition;
    }

    int getTwonkyType() {
        return mTwonkyType;
    }

    View getRootView() {
        return mRootView;
    }

    RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    View getRecyclerEmptyView() {
        return mRecyclerEmptyView;
    }

    void setStartIndex(int index) {
        mStartIndex = index;
    }

    int getStartIndex() {
        return mStartIndex;
    }

    ArrayList<FileInfo> getFileList() {
        return mFileList;
    }

    void updateFileList(ArrayList<FileInfo> list, boolean isReset) {
        Log.d(TAG, "\n[Enter] MediaType.updateFileList "+ this.toString());
        Log.d(TAG, "mFileList size before: "+ mFileList.size());
        if (isReset) {
            mFileList.clear();
            mFileList.addAll(list);
        } else {
            mFileList.addAll(list);
        }
        Log.d(TAG, "mFileList size after: "+ mFileList.size());
    }

    void clear() {
        ViewGroup group = (ViewGroup) mRootView.getParent();
        if (group != null) {
            group.removeView(mRootView);
        }

        // TODO
        if (mTabPosition != ALL.mTabPosition) {
            mFileList.clear();
        }

        mRecyclerView.clearOnScrollListeners();

        mRootView = null;
        mRecyclerView = null;
        mRecyclerEmptyView = null;
        mStartIndex = 0;
    }

    void init(Activity activity) {
//        if (mRootView == null) {
            mRootView = activity.getLayoutInflater().inflate(R.layout.fragment_pager, null);
//        }
        mRecyclerView = (RecyclerView) mRootView.findViewById(R.id.recycler_view);
        mRecyclerEmptyView = mRootView.findViewById(R.id.main_recycler_empty_view);

        LayoutManager lm = initLayoutManager(activity);
        mRecyclerView.setLayoutManager(lm);
        mRecyclerView.setAdapter(new FileManageRecyclerAdapter(activity, new ArrayList<FileInfo>()));
    }

    private LayoutManager initLayoutManager(Activity activity) {
        int mode = NASPref.getViewMode(activity, mViewModeKey);
        if (mode == LayoutType.LIST.ordinal()) {
            return new LinearLayoutManager(activity);
        } else {
            int orientation = activity.getResources().getConfiguration().orientation;
            int spanCount = (orientation == Configuration.ORIENTATION_PORTRAIT) ? GRID_PORTRAIT : GRID_LANDSCAPE;
            return new GridLayoutManager(activity, spanCount);
        }
    }

}
