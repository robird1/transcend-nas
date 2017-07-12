package com.transcend.nas.management.browser_framework;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.transcend.nas.R;
import com.transcend.nas.management.FileInfo;

import java.util.ArrayList;

/**
 * Created by steve_su on 2017/7/10.
 */

public enum TabData {
    ALL(R.drawable.ic_browser_filetype_all),
    PHOTO(R.drawable.ic_browser_filetype_image),
    MUSIC(R.drawable.ic_browser_filetype_music),
    VIDEO(R.drawable.ic_browser_filetype_video),
    FILE(R.drawable.ic_browser_filetype_video);

    private static final String TAG = TabData.class.getSimpleName();
    private final int mIconId;
    private int mTabPosition;
    private RecyclerView mRecyclerView;
    private View mRecyclerEmptyView;
    private FileListTabFragment.LayoutType mViewMode = FileListTabFragment.LayoutType.LIST;


    private ArrayList<FileInfo> mFileList = new ArrayList<>();
    private String mPath;
    private int mStartIndex = 0;



    TabData(int iconId) {
        mIconId = iconId;
    }

    public static TabData getInstance(int position) {
        for (TabData f : TabData.values()) {
            if (position == f.getTabPosition()) {
                return f;
            }
        }
        return TabData.ALL;
    }

    int getIconId() {
        return mIconId;
    }

    void setTabPosition(int position) {
        mTabPosition = position;
    }

    public int getTabPosition() {
        return mTabPosition;
    }

    void setRecyclerView(RecyclerView view) {
        mRecyclerView = view;
    }

    RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    void setRecyclerEmptyView(View view) {
        mRecyclerEmptyView = view;
    }

    View getRecyclerEmptyView() {
        return mRecyclerEmptyView;
    }

    void setViewMode(FileListTabFragment.LayoutType mode) {
        mViewMode = mode;
    }

    FileListTabFragment.LayoutType getViewMode() {
        return mViewMode;
    }








    public void setStartIndex(int index) {
        mStartIndex = index;
    }

    public int getStartIndex() {
        return mStartIndex;
    }

    public ArrayList<FileInfo> getFileList() {
        return mFileList;
    }

    public void updateFileList(ArrayList<FileInfo> list, boolean isReset) {
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


}
