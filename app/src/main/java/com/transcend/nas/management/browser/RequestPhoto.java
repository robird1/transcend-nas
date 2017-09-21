package com.transcend.nas.management.browser;

import android.os.Bundle;

import com.transcend.nas.NASApp;
import com.transcend.nas.management.FileInfo;

/**
 * Created by steve_su on 2017/8/11.
 */

class RequestPhoto extends RequestAction {

    RequestPhoto(BrowserActivity activity) {
        super(activity);
    }

    @Override
    public void onRecyclerItemClick(FileInfo fileInfo) {
        fileInfo.name = urlEncode(fileInfo.name);
        String apiName = getAPIName(fileInfo.path);
        Bundle args = new Bundle();
        if ("get_photo_years".equals(apiName)) {
            String path = fileInfo.path.concat("||get_photo_months?year=").concat(fileInfo.name);
            args.putString("op", "get_photo_months");
            args.putString("api_args", "year="+ fileInfo.name);
            args.putString("path", path);
            startLoader(TWONKY_INDEX, args);

        } else if ("get_photo_months".equals(apiName)) {
            String arg = getAPIArgs(fileInfo.path)+ "&month="+ TwonkyIndexLoader.mMonthMap2.get(fileInfo.name);
            String path = fileInfo.path.concat("||get_photo?").concat(arg);
            args.putString("op", "get_photo");
            args.putString("api_args", arg);
            args.putString("path", path);
            args.putBoolean("force_top", true);
            startLoader(TWONKY_CUSTOM, args);

        } else if ("get_photo_album".equals(apiName)) {
            fileInfo.twonkyFolderPath = urlEncode(fileInfo.twonkyFolderPath);
            String path = fileInfo.path.concat("||get_photo?folder=").concat(fileInfo.twonkyFolderPath);
            args.putString("op", "get_photo");
            args.putString("api_args", "folder="+ fileInfo.twonkyFolderPath);
            args.putString("path", path);
            startLoader(TWONKY_CUSTOM, args);
        }

    }

    @Override
    public void onBackPressed() {
        String parentPath = getParentPath();
        String apiName = getAPIName(parentPath);
        Bundle args = new Bundle();
        if ("get_photo_months".equals(apiName)) {
            String apiArgs = getAPIArgs(parentPath);
            args.putString("op", "get_photo_months");
            args.putString("api_args", apiArgs);
            args.putString("path",  parentPath);
            startLoader(TWONKY_INDEX, args);

        } else if ("get_photo_years".equals(apiName)) {
            args.putString("op", "get_photo_years");
            args.putString("path", parentPath);
            startLoader(TWONKY_INDEX, args);

        } else if ("get_photo_album".equals(apiName)) {
            args.putString("op", "get_photo_album");
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
        Bundle args = new Bundle();
        if ("get_photo_years".equals(apiName)) {
            args.putString("op", "get_photo_years");
            args.putString("path", mActivity.mPath);
            startLoader(TWONKY_INDEX, args, showProgress);

        } else if ("get_photo_months".equals(apiName)) {
            args.putString("op", "get_photo_months");
            args.putString("api_args", getAPIArgs(mActivity.mPath));
            args.putString("path", mActivity.mPath);
            startLoader(TWONKY_INDEX, args, showProgress);

        } else if ("get_photo".equals(apiName)) {
            args.putString("op", "get_photo");
            args.putString("api_args", getAPIArgs(mActivity.mPath));
            args.putString("path", mActivity.mPath);
            startLoader(TWONKY_CUSTOM, args, showProgress);

        } else if ("get_photo_album".equals(apiName)) {
            args.putString("op", "get_photo_album");
            args.putString("path", mActivity.mPath);
            startLoader(TWONKY_INDEX, args, showProgress);

        } else {  // View all photos
            viewAll(showProgress);
        }
    }

    @Override
    public void search(String text) {
        String apiName = getAPIName(mActivity.mPath);
        Bundle args = new Bundle();
        if ("get_photo_years".equals(apiName) || "get_photo_album".equals(apiName)) {
            // search under view by date / view by folder
            args.putInt("type", StoreJetCloudData.PHOTO.getTwonkyType());
            args.putString("search_key", text);
            startLoader(TWONKY_VIEW_ALL, args);

        } else if ("get_photo".equals(apiName) || "get_photo_months".equals(apiName)) {
            // search under view by selected year / view by selected month / view by selected folder
            args.putString("op", "get_photo");
            args.putString("api_args", getAPIArgs(mActivity.mPath));
            args.putString("search_key", text);
            startLoader(TWONKY_CUSTOM, args);

        } else {
            // search under view all photos
            args.putInt("type", StoreJetCloudData.PHOTO.getTwonkyType());
            args.putString("search_key", text);
            startLoader(TWONKY_VIEW_ALL, args);
        }

    }

    @Override
    public void viewByDate() {
        Bundle args = new Bundle();
        args.putString("op", "get_photo_years");
        args.putString("path", "||get_photo_years");
        startLoader(TWONKY_INDEX, args);
    }

    @Override
    public void viewByFolder() {
        Bundle args = new Bundle();
        args.putString("op", "get_photo_album");
        args.putString("path", "||get_photo_album");
        startLoader(TWONKY_INDEX, args);
    }

}
