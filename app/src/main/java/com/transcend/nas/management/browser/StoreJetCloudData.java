package com.transcend.nas.management.browser;

import android.content.Context;

import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.browser_framework.Browser;
import com.transcend.nas.management.browser_framework.BrowserData;

import java.util.ArrayList;

import static android.R.attr.mode;

/**
 * Created by steve_su on 2017/7/18.
 */

public enum StoreJetCloudData {
    ALL(0, 0, "view_preference_folder"),
    PHOTO(1, 2, "view_preference_photo"),
    MUSIC(2, 1, "view_preference_music"),
    VIDEO(3, 3, "view_preference_video");

//    private BrowserData mData;
//    private int mPosition;
//    private int mLoadingIndex = 0;
//
//    StoreJetCloudData(BrowserData instance) {
//        mData = instance;
//        mPosition = instance.getTabPosition();
//    }

    private int mTabPosition;
    private int mTwonkyType;
    private String mViewKey;
    private int mLoadingIndex = 0;
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
//        BrowserData instance = BrowserData.getInstance(mPosition);
//        if (instance == BrowserData.ALL) {
//            return 0;
//        } else if (instance == BrowserData.PHOTO) {
//            return 2;
//        } else if (instance == BrowserData.MUSIC) {
//            return 1;
//        } else if (instance == BrowserData.VIDEO) {
//            return 3;
//        }
//        return 0;
        return mTwonkyType;
    }

    public void setViewPreference(Context context, int menuPosition) {
        NASPref.setViewPreference(context, mViewKey, menuPosition);
    }

    public int getViewPreference(Context context) {
        int mode = NASPref.getViewPreference(context, mViewKey);
        return mode;
    }


}
