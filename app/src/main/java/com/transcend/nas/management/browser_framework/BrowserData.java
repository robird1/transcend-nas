package com.transcend.nas.management.browser_framework;

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
    DOCUMENT(R.drawable.ic_browser_filetype_document, "view_mode_document");

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
        if (list == null) {
            return;
        }
        mFileList.addAll(list);
    }

    public void updateFileList(ArrayList<FileInfo> list) {
        if (list == null) {
            return;
        }
        mFileList = new ArrayList<>(list);
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
