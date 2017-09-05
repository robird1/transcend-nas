package com.transcend.nas.management.browser_framework;

import android.content.Context;
import android.util.Log;

import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.management.FileInfo;

import java.util.ArrayList;

/**
 * Created by steve_su on 2017/7/10.
 */

public enum BrowserData {
    ALL(R.drawable.ic_browser_filetype_all, "view_mode_folder"),
    PHOTO(R.drawable.ic_browser_filetype_image, "view_mode_photo"),
    MUSIC(R.drawable.ic_browser_filetype_music, "view_mode_music"),
    VIDEO(R.drawable.ic_browser_filetype_video, "view_mode_video"),
    FILE(R.drawable.ic_browser_filetype_video, "view_mode_file");

    private static final String TAG = BrowserData.class.getSimpleName();
    private final int mIconId;
    private final String mViewModeKey;
    private int mTabPosition;
    private ArrayList<FileInfo> mFileList = new ArrayList<>();
    private Browser.LayoutType mLayout;

    BrowserData(int iconId, String viewModeKey) {
        mIconId = iconId;
        mViewModeKey = viewModeKey;
    }

    public static BrowserData getInstance(int position) {
        for (BrowserData f : BrowserData.values()) {
            if (position == f.getTabPosition()) {
                return f;
            }
        }
        return BrowserData.ALL;
    }

    static void clear() {
        for (BrowserData f : BrowserData.values()) {
            f.clearFileList();
        }
    }

    int getIconId() {
        return mIconId;
    }

    // TODO
    public void setViewMode(Context context, Browser.LayoutType mode) {
        NASPref.setViewMode(context, mViewModeKey, mode.ordinal());
    }

    // TODO
    public Browser.LayoutType getViewMode(Context context) {
        int mode = NASPref.getViewMode(context, mViewModeKey);
        return (mode == Browser.LayoutType.LIST.ordinal()) ? Browser.LayoutType.LIST: Browser.LayoutType.GRID;
    }

    void setTabPosition(int position) {
        mTabPosition = position;
    }

    public int getTabPosition() {
        return mTabPosition;
    }

    public ArrayList<FileInfo> getFileList() {
        return mFileList;
    }

    public void addFiles(ArrayList<FileInfo> list) {
        mFileList.addAll(list);
    }

    public void updateFileList(ArrayList<FileInfo> list) {
        mFileList.clear();
        mFileList.addAll(list);
    }

    void clearFileList() {
        mFileList.clear();
    }

    public void setLayout(Browser.LayoutType type) {
        mLayout = type;
    }

    public Browser.LayoutType getLayout() {
        return mLayout;
    }

}
