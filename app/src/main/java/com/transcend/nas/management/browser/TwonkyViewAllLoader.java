package com.transcend.nas.management.browser;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.management.FileInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by steve_su on 2017/8/10.
 */

class TwonkyViewAllLoader extends TwonkyGeneralPostLoader {
    private static final String TAG = TwonkyViewAllLoader.class.getSimpleName();
    private static final int COUNT_LAZY_LOAD = 100;
    private int mCount;
    private int mStart;
    private int mLoadedCount;
    private boolean mIsLoadingFinish = false;
    private String mPath;

    TwonkyViewAllLoader(Context context, Bundle args) {
        super(context, args);
    }

    @Override
    protected void onStartLoading() {
        Log.d(TAG, "[Enter] onStartLoading");
        if (!mIsLoadingFinish) {
            forceLoad();
        }
    }

    @Override
    protected String onRequestBody() {
        return getRequestContent(mArgs);
    }

    @Override
    protected String onRequestUrl() {
        return "http://" + getHost() + "/nas/get/twonky";
    }

    @Override
    protected Boolean onLoadInBackground(JSONObject jsonObject) {
        if (mArgs == null) {
            return false;
        }

        ArrayList<FileInfo> list = new ArrayList<>();
        try {
            JSONArray jArray = jsonObject.getJSONArray("result");
            mLoadedCount = jArray.length();

            for (int i = 0; i < jArray.length(); i++) {
                JSONObject object = jArray.getJSONObject(i);
                FileInfo info = new FileInfo();
                info.path = object.optString("local_path").replace("/home", "");
//                info.name = object.optString("title");
                String localPath = object.optString("local_path");
                int tempIndex = localPath.lastIndexOf("/");
                info.name = localPath.substring(tempIndex+1);
                info.type = FileInfo.getType(info.path);
                info.time = FileInfo.getTime(object.optLong("modifiedTime") * 1000);
                info.size = object.optLong("size");
                info.thumbnail = object.optString("thumbnail");
                if (info.type.equals(FileInfo.TYPE.MUSIC)) {
                    info.thumbnail = object.optString("album_art_url");
                }

                list.add(info);
            }
            setFileList(list);

//            if (mLoadedCount < COUNT) {
//                mIsLoadingFinish = true;
//            }

            return true;

        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        return false;
    }

    int nextLoadingIndex() {
        return mStart + mLoadedCount;
    }

    public boolean isLoadingFinish() {
        return mIsLoadingFinish;
    }

    private String getRequestContent(Bundle args) {
        mStart = args.getInt("start", 0);
        String userName = ServerManager.INSTANCE.getCurrentServer().getUsername();
        int type = args.getInt("type", -1);
        String orderby = args.getString("orderby", "title_asc");
        // shared folder path
        String systemPath = args.getString("system_path");
        // twonky path
        mPath = args.getString("path", "");
        mCount = args.getInt("count");

        if (mCount == 0) {
            mCount = COUNT_LAZY_LOAD;
        }
        
        return "hash=" + getHash() + "&fmt=json&start=" + mStart + "&count=" + mCount +
                "&login=" + userName + "&path="+ systemPath+ "&type=" + type + "&orderby="+ orderby;
    }

    int getStartIndex() {
        return mStart;
    }

    /**
     * get Twonky path.
     *
     * @return
     */
    public String getPath() {
        return mPath;
    }

}