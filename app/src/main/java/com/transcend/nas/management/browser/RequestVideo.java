package com.transcend.nas.management.browser;

import android.os.Bundle;
import android.util.Log;

import com.transcend.nas.NASApp;
import com.transcend.nas.management.FileInfo;

/**
 * Created by steve_su on 2017/8/16.
 */

public class RequestVideo extends RequestAction {
    private static final String TAG = RequestVideo.class.getSimpleName();

    public RequestVideo(BrowserActivity activity) {
        super(activity);
    }

    @Override
    public void onRecyclerItemClick(FileInfo fileInfo) {
        Log.d(TAG, "\n[Enter] onRecyclerItemClick() mActivity.mPath: "+ mActivity.mPath);

        fileInfo.twonkyFolderPath = urlEncode(fileInfo.twonkyFolderPath);
        String apiName = getAPIName(fileInfo.path);
        Bundle args = new Bundle();
        if ("get_video_album".equals(apiName)) {
            String path = fileInfo.path.concat("||get_video?folder=").concat(fileInfo.twonkyFolderPath);
            args.putString("op", "get_video");
            args.putString("api_args", "folder="+ fileInfo.twonkyFolderPath);
            args.putString("path", path);
            startLoader(TWONKY_CUSTOM, args);
        }
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "\n[Enter] onBackPressed() mActivity.mPath: "+ mActivity.mPath);
        String parentPath = getParentPath();
        Log.d(TAG, "parentPath : "+ parentPath);

        String apiName = getAPIName(parentPath);
        Bundle args = new Bundle();
        if ("get_video_album".equals(apiName)) {
            args.putString("op", "get_video_album");
            args.putString("path", parentPath);
            startLoader(TWONKY_INDEX, args);

        } else {
            stopLoader();
            mActivity.doLoad(NASApp.ROOT_SMB);
        }
    }

    @Override
    public void refresh(boolean showProgress) {
        String apiName = getAPIName(mActivity.mPath);
        Log.d(TAG, "\n[Enter] refresh apiName: "+ apiName);

        Bundle args = new Bundle();
        if ("get_video_album".equals(apiName)) {
            args.putString("op", "get_video_album");
            args.putString("path", mActivity.mPath);
            startLoader(TWONKY_INDEX, args, showProgress);

        } else if ("get_video".equals(apiName)) {
            args.putString("op", "get_video");
            args.putString("api_args", getAPIArgs(mActivity.mPath));
            args.putString("path", mActivity.mPath);
            startLoader(TWONKY_CUSTOM, args, showProgress);

        } else {  // View all videos
            viewAll(showProgress);
        }
    }

    @Override
    public void viewByFolder() {
        Bundle args = new Bundle();
        args.putString("op", "get_video_album");
        args.putString("path", "||get_video_album");
        startLoader(TWONKY_INDEX, args);

    }

}
