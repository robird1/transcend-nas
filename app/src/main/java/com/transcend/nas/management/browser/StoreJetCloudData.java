package com.transcend.nas.management.browser;

import android.content.Context;

import com.transcend.nas.NASPref;

/**
 * Created by steve_su on 2017/7/18.
 */

public enum StoreJetCloudData {
    ALL(0, 0, "view_preference_folder"),
    PHOTO(1, 2, "view_preference_photo"),
    MUSIC(2, 1, "view_preference_music"),
    VIDEO(3, 3, "view_preference_video");

    private int mTabPosition;
    private int mTwonkyType;
    private String mViewKey;
    private int mLoadingIndex = 0;
    private String mPath;
    private boolean mIsFabEnabled;
    private int mListSize;

    StoreJetCloudData(int tabPosition, int twonkyType, String viewKey) {
        mTabPosition = tabPosition;
        mTwonkyType = twonkyType;
        mViewKey = viewKey;
    }

    public static StoreJetCloudData getInstance(int position) {
        for (StoreJetCloudData instance : StoreJetCloudData.values()) {
            if (position == instance.mTabPosition) {
                return instance;
            }
        }
        return StoreJetCloudData.ALL;
    }

    public void setLoadingIndex(int index) {
        mLoadingIndex = index;
    }

    public int getLoadingIndex() {
        return mLoadingIndex;
    }

    public int getTwonkyType() {
        return mTwonkyType;
    }

    public void setViewPreference(Context context, int menuPosition) {
        NASPref.setViewPreference(context, mViewKey, menuPosition);
    }

    public int getViewPreference(Context context) {
        int mode = NASPref.getViewPreference(context, mViewKey);
        return mode;
    }

    public void setPath(String path) {
        mPath = path;
    }

    public String getPath() {
        return mPath;
    }

//    public void setFabEnabled(boolean isEnabled) {
//        mIsFabEnabled = isEnabled;
//    }
//
//    public boolean getFabEnabled() {
//        return mIsFabEnabled;
//    }

    public void setListSize(int size) {
        mListSize = size;
    }

    public int getListSize() {
        return mListSize;
    }

}
