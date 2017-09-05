package com.transcend.nas.management.browser;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.transcend.nas.NASApp;
import com.transcend.nas.management.FileInfo;

/**
 * Created by steve_su on 2017/8/16.
 */

public class RequestMusic extends RequestAction {
    private static final String TAG = RequestMusic.class.getSimpleName();

    public RequestMusic(BrowserActivity activity) {
        super(activity);
    }

    @Override
    public void onRecyclerItemClick(FileInfo fileInfo) {
        Log.d(TAG, "\n[Enter] onRecyclerItemClick()");
        Log.d(TAG, "fileInfo.name: "+ fileInfo.name+ "\nmActivity.mPath: "+ mActivity.mPath+ "\nfileInfo.path: "+ fileInfo.path);

        fileInfo.name = urlEncode(fileInfo.name);
        String apiName = getAPIName(fileInfo.path);
        Bundle args = new Bundle();
        if ("get_music_artists".equals(apiName)) {
            String path = fileInfo.path.concat("||get_music_album?artist=").concat(fileInfo.name);
            args.putString("op", "get_music_album");
            args.putString("api_args", "artist="+ fileInfo.name);
            args.putString("path", path);
            startLoader(TWONKY_INDEX, args);

        } else if ("get_music_album".equals(apiName)) {
            String arg;
            if (!TextUtils.isEmpty(getAPIArgs(fileInfo.path))) {
                arg = getAPIArgs(fileInfo.path)+ "&album="+ fileInfo.name;
            } else {
                arg = "album="+ fileInfo.name;
            }
            String path = fileInfo.path.concat("||get_music?").concat(arg);
            args.putString("op", "get_music");
            args.putString("api_args", arg);
            args.putString("path", path);
            startLoader(TWONKY_CUSTOM, args);

        } else if ("get_music_genre".equals(apiName)) {
            String path = fileInfo.path.concat("||get_music_album?genre=").concat(fileInfo.name);
            args.putString("op", "get_music_album");
            args.putString("api_args", "genre="+ fileInfo.name);
            args.putString("path", path);
            startLoader(TWONKY_INDEX, args);
        }

    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "\n[Enter] onBackPressed() mActivity.mPath: "+ mActivity.mPath);
        String parentPath = getParentPath();
        Log.d(TAG, "parentPath : "+ parentPath);

        String apiName = getAPIName(parentPath);
        Bundle args = new Bundle();
        if ("get_music_album".equals(apiName)) {
            String apiArgs = getAPIArgs(parentPath);
            args.putString("op", "get_music_album");
            args.putString("api_args", apiArgs);
            args.putString("path",  parentPath);
            startLoader(TWONKY_INDEX, args);

        } else if ("get_music_artists".equals(apiName)) {
            args.putString("op", "get_music_artists");
            args.putString("path", parentPath);
            startLoader(TWONKY_INDEX, args);

        } else if ("get_music_genre".equals(apiName)) {
            args.putString("op", "get_music_genre");
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
        if ("get_music_artists".equals(apiName)) {
            args.putString("op", "get_music_artists");
            args.putString("path", mActivity.mPath);
            startLoader(TWONKY_INDEX, args, showProgress);

        } else if ("get_music_album".equals(apiName)) {
            args.putString("op", "get_music_album");
            args.putString("api_args", getAPIArgs(mActivity.mPath));
            args.putString("path", mActivity.mPath);
            startLoader(TWONKY_INDEX, args, showProgress);

        } else if ("get_music".equals(apiName)) {
            args.putString("op", "get_music");
            args.putString("api_args", getAPIArgs(mActivity.mPath));
            args.putString("path", mActivity.mPath);
            startLoader(TWONKY_CUSTOM, args, showProgress);
        } else if ("get_music_genre".equals(apiName)) {
            args.putString("op", "get_music_genre");
            args.putString("path", mActivity.mPath);
            startLoader(TWONKY_INDEX, args, showProgress);

        } else {  // View all music
            viewAll(showProgress);
        }
    }

    @Override
    public void viewByArtist() {
        Bundle args = new Bundle();
        args.putString("op", "get_music_artists");
        args.putString("path", "||get_music_artists");
        startLoader(TWONKY_INDEX, args);
    }

    @Override
    public void viewByAlbum() {
        Bundle args = new Bundle();
        args.putString("op", "get_music_album");
        args.putString("path", "||get_music_album");
        startLoader(TWONKY_INDEX, args);
    }

    @Override
    public void viewByGenre() {
        Bundle args = new Bundle();
        args.putString("op", "get_music_genre");
        args.putString("path", "||get_music_genre");
        startLoader(TWONKY_INDEX, args);
    }

}
