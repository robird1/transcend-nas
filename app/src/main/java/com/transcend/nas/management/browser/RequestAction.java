package com.transcend.nas.management.browser;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.transcend.nas.management.FileInfo;

import java.net.URLEncoder;

/**
 * Created by steve_su on 2017/8/11.
 */

public abstract class RequestAction {
    private static final String TAG = RequestAction.class.getSimpleName();
    public static final int TWONKY_VIEW_ALL = 168;
    public static final int TWONKY_INDEX = 169;
    public static final int TWONKY_CUSTOM = 170;
    static final String PATH_TOKEN = "||";
    protected BrowserActivity mActivity;
    protected BrowserFragment mFragment;

    RequestAction(BrowserActivity activity) {
        mActivity = activity;
        mFragment = (BrowserFragment) mActivity.getSupportFragmentManager().findFragmentByTag(BrowserFragment.TAG);
    }

    public static String getAPIName(String path) {
        Log.d(TAG, "[Enter] getAPIName path: "+ path);
        if (path == null) {
            return "";
        }
        String name = "";
        int tempIndex = path.lastIndexOf(PATH_TOKEN);
        if (tempIndex != -1) {
            String tmpString = path.substring(tempIndex + PATH_TOKEN.length());
            String[] temp = tmpString.split("\\?");
            if (temp.length > 0) {
                name = temp[0];
            }
        }
//        Log.d(TAG, "name: "+ name);

        return name;
    }

    public static String getAPIArgs(String path) {
//        Log.d(TAG, "[Enter] getAPIArgs path: "+ path);
        if (path == null) {
            return "";
        }
        String args = "";
        int tempIndex = path.lastIndexOf(PATH_TOKEN);
        if (tempIndex != -1) {
            String tmpString = path.substring(tempIndex + PATH_TOKEN.length());
            String[] temp = tmpString.split("\\?");
            if (temp.length == 2) {
                args = temp[1];
            }
        }

        Log.d(TAG, "args: "+ args);
        return args;
    }

    public String urlEncode(String arg) {
        if (TextUtils.isEmpty(arg)) {
            return "";
        }
        return URLEncoder.encode(arg);
    }

    public void viewAll() {
        viewAll(true);
    }

    public void viewAll(boolean showProgress) {
        mFragment.clearData();
        StoreJetCloudData instance = StoreJetCloudData.getInstance(mFragment.getTabPosition());
        Bundle args = new Bundle();
        args.putInt("start", 0);
        args.putInt("type", instance.getTwonkyType());
        args.putString("path", "||view_all?".concat(String.valueOf(instance.getTwonkyType())));
        setOrderBy(instance, args);
        startLoader(TWONKY_VIEW_ALL, args, showProgress);
    }

    public void lazyLoad() {
        Log.d(TAG, "[Enter] lazyLoad");
        StoreJetCloudData instance = StoreJetCloudData.getInstance(mFragment.getTabPosition());
        Bundle args = new Bundle();
        int startIndex = instance.getLoadingIndex();
        Log.d(TAG, "startIndex: "+ startIndex);
        args.putInt("start", startIndex);
        args.putInt("type", instance.getTwonkyType());
        args.putString("path", "||view_all?".concat(String.valueOf(instance.getTwonkyType())));
        setOrderBy(instance, args);
        startLoader(TWONKY_VIEW_ALL, args, true);
    }

//    public void refresh(boolean showProgress) {
//
//    }

    protected void startLoader(int loaderID, Bundle args) {
        startLoader(loaderID, args, true);
    }

    protected void startLoader(int loaderID, Bundle args, boolean showProgress) {
        mFragment.mProgressView.setVisibility(showProgress? View.VISIBLE : View.INVISIBLE);
        mFragment.stopRunningLoader();
        mFragment.getLoaderManager().restartLoader(loaderID, args, mFragment).forceLoad();
    }

    protected void stopLoader() {
        mFragment.stopRunningLoader();
    }

    @NonNull
    protected String getParentPath() {
        String path = mActivity.mPath.substring(0, mActivity.mPath.lastIndexOf("||"));
        return path;
    }

    private void setOrderBy(StoreJetCloudData instance, Bundle args) {
        if (instance.getTwonkyType() == StoreJetCloudData.PHOTO.getTwonkyType()) {
            args.putString("orderby", "modifiedTime_desc");
        } else {
            args.putString("orderby", "title_asc");
        }
    }

    abstract public void onRecyclerItemClick(FileInfo fileInfo);
    abstract public void onBackPressed();
    abstract public void refresh(boolean showProgress);

    protected void viewByDate() {

    }

    protected void viewByFolder() {

    }

    protected void viewByArtist() {

    }

    protected void viewByAlbum() {

    }

    protected void viewByGenre() {

    }


}
