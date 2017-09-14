package com.transcend.nas.management.browser;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;

import com.transcend.nas.management.FileInfo;

import java.net.URLEncoder;

/**
 * Created by steve_su on 2017/8/11.
 */

public abstract class RequestAction {
    static final int TWONKY_VIEW_ALL = 168;
    static final int TWONKY_INDEX = 169;
    static final int TWONKY_CUSTOM = 170;
    static final String PATH_TOKEN = "||";
    protected BrowserActivity mActivity;
    protected BrowserFragment mFragment;

    RequestAction(BrowserActivity activity) {
        mActivity = activity;
        mFragment = (BrowserFragment) mActivity.getSupportFragmentManager().findFragmentByTag(BrowserFragment.TAG);
    }

    static String getAPIName(String path) {
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

        return name;
    }

    static String getAPIArgs(String path) {
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

        return args;
    }

    String urlEncode(String arg) {
        if (TextUtils.isEmpty(arg)) {
            return "";
        }
        return URLEncoder.encode(arg);
    }

    void viewAll() {
        viewAll(true);
    }

    /**
     * onPageChanged, refresh after file operation, option menu refresh, SwipeRefreshLayout
     *
     */
    void viewAll(boolean showProgress) {
//        mFragment.clearData();
        StoreJetCloudData instance = StoreJetCloudData.getInstance(mFragment.getTabPosition());
        Bundle args = new Bundle();
        args.putInt("start", 0);
        args.putInt("type", instance.getTwonkyType());
        args.putString("path", "||view_all?".concat(String.valueOf(instance.getTwonkyType())));
        args.putInt("count", instance.getListSize());
        setOrderBy(instance, args);
        startLoader(TWONKY_VIEW_ALL, args, showProgress);
    }

    void lazyLoad() {
        StoreJetCloudData instance = StoreJetCloudData.getInstance(mFragment.getTabPosition());
        Bundle args = new Bundle();
        int startIndex = instance.getLoadingIndex();
        args.putInt("start", startIndex);
        args.putInt("type", instance.getTwonkyType());
        args.putString("path", "||view_all?".concat(String.valueOf(instance.getTwonkyType())));
        setOrderBy(instance, args);
        startLoader(TWONKY_VIEW_ALL, args, true);
    }

    void startLoader(int loaderID, Bundle args) {
        startLoader(loaderID, args, true);
    }

    void startLoader(int loaderID, Bundle args, boolean showProgress) {
        args.putString("system_path", URLEncoder.encode(mFragment.getSystemPath()));
        mFragment.mProgressView.setVisibility(showProgress? View.VISIBLE : View.INVISIBLE);
        mFragment.stopRunningLoader();
        mFragment.getLoaderManager().restartLoader(loaderID, args, mFragment).forceLoad();
    }

    void stopLoader() {
        mFragment.stopRunningLoader();
    }

    @NonNull
    String getParentPath() {
        return mActivity.mPath.substring(0, mActivity.mPath.lastIndexOf("||"));
    }

    private void setOrderBy(StoreJetCloudData instance, Bundle args) {
        if (instance.getTwonkyType() == StoreJetCloudData.PHOTO.getTwonkyType()) {
            args.putString("orderby", "modifiedTime_desc");
        } else {
            args.putString("orderby", "local_path_asc");
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
