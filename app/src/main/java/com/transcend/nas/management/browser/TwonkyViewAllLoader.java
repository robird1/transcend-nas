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

public class TwonkyViewAllLoader extends TwonkyGeneralPostLoader {

    private static final String TAG = TwonkyViewAllLoader.class.getSimpleName();
    static final int COUNT = 100;
    private int mStart;
    private int mLoadedCount;
    private boolean mIsLoadingFinish = false;
    private String mPath;

    public TwonkyViewAllLoader(Context context, Bundle args) {
        super(context, args);
    }

    @Override
    protected void onStartLoading() {
        Log.d(TAG, "[Enter] onStartLoading");
        if (mIsLoadingFinish == false) {
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
        Log.d(TAG, "[Enter] loadInBackground ");
        if (mArgs == null) {
            return false;
        }

        ArrayList<FileInfo> list = new ArrayList();
        try {
            JSONArray jArray = jsonObject.getJSONArray("result");
            Log.d(TAG, "jArray.length(): " + jArray.length());
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
//                Log.d(TAG, "info.thumbnail: "+ info.thumbnail);

                if (info.type.equals(FileInfo.TYPE.MUSIC)) {
                    info.thumbnail = object.optString("album_art_url");
                }

                list.add(info);
            }
            setFileList(list);

            if (mLoadedCount < COUNT) {
                mIsLoadingFinish = true;
            }

            return true;

        }
        catch (JSONException e) {
            Log.d(TAG, "[Enter] JSONException====================================================");
            Log.d(TAG, "e.printStackTrace(): " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public boolean isLazyLoading() {
        return mArgs.getBoolean("is_lazy_loading", false);
    }

    public int nextLoadingIndex() {
        return mStart + mLoadedCount;
    }

    public boolean isLoadingFinish() {
        return mIsLoadingFinish;
    }

    private String getRequestContent(Bundle args) {
        mStart = args.getInt("start", 0);
        String userName = ServerManager.INSTANCE.getCurrentServer().getUsername();
        int type = args.getInt("type", -1);
        mPath = args.getString("path", "");
        String orderby = args.getString("orderby", "title_asc");

        String content = "hash=" + getHash() + "&fmt=json&start=" + mStart + "&count=" + COUNT +
                "&login=" + userName + "&path=%2F&type=" + type + "&orderby="+ orderby;
        return content;
    }

    public int getStartIndex() {
        return mStart;
    }

    public String getPath() {
        return mPath;
    }

}