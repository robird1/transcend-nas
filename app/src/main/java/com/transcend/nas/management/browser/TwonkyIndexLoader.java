package com.transcend.nas.management.browser;

import android.content.Context;
import android.os.Bundle;

import com.transcend.nas.management.FileInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by steve_su on 2017/8/9.
 */

public class TwonkyIndexLoader extends TwonkyGeneralPostLoader {
    static final HashMap<String, String> mMonthMap = new HashMap<>();
    static final HashMap<String, String> mMonthMap2 = new HashMap<>();
    private String mPath;
    private String mOperation;
    private String mApiArgs;

    static {
        mMonthMap.put("1", "January");
        mMonthMap.put("2", "February");
        mMonthMap.put("3", "March");
        mMonthMap.put("4", "April");
        mMonthMap.put("5", "May");
        mMonthMap.put("6", "June");
        mMonthMap.put("7", "July");
        mMonthMap.put("8", "August");
        mMonthMap.put("9", "September");
        mMonthMap.put("10", "October");
        mMonthMap.put("11", "November");
        mMonthMap.put("12", "December");

        mMonthMap2.put("January", "1");
        mMonthMap2.put("February", "2");
        mMonthMap2.put("March", "3");
        mMonthMap2.put("April", "4");
        mMonthMap2.put("May", "5");
        mMonthMap2.put("June", "6");
        mMonthMap2.put("July", "7");
        mMonthMap2.put("August", "8");
        mMonthMap2.put("September", "9");
        mMonthMap2.put("October", "10");
        mMonthMap2.put("November", "11");
        mMonthMap2.put("December", "12");
    }


    TwonkyIndexLoader(Context context, Bundle args) {
        super(context, args);
        mOperation = args.getString("op");

        mApiArgs = "path="+ args.getString("system_path");
        if (args.getString("api_args") != null) {
            mApiArgs = mApiArgs + "&"+  args.getString("api_args");
        }

        // Twonky API path
        mPath = args.getString("path");
    }

    @Override
    protected String onRequestBody() {
        return "hash="+ getHash()+ "&fmt=json&op="+ mOperation+ "&" + mApiArgs;
    }

    @Override
    protected String onRequestUrl() {
        return "http://"+ getHost()+ "/nas/get/twonkyidx";
    }

    @Override
    protected Boolean onLoadInBackground(JSONObject jsonObj) {
        ArrayList<FileInfo> list = new ArrayList<>();
        try {
            JSONArray arrJson = jsonObj.getJSONArray("result");
            for (int i = 0; i < arrJson.length(); i++) {
                FileInfo info = new FileInfo();
                info.path = mPath;
                info.type = FileInfo.TYPE.DIR;
                info.time = "";
                info.size = 0L;
                info.isTwonkyIndexFolder = true;
                setFileInfo(arrJson, i, info);

                list.add(info);
            }
            setFileList(list);
            return true;

        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void setFileInfo(JSONArray arrJson, int i, FileInfo info) throws JSONException {
        if ("get_photo_album".equals(mOperation) || "get_video_album".equals(mOperation)) {
            info.name = arrJson.getJSONObject(i).getString("index_name");
            info.twonkyFolderPath = arrJson.getJSONObject(i).getString("path");
            info.twonkyIndexCount = arrJson.getJSONObject(i).getInt("count");
        } else {
            info.name = arrJson.getJSONObject(i).getString("index_name");
            if ("get_photo_months".equals(mOperation)) {
                info.name = mMonthMap.get(info.name);
            }
            info.twonkyIndexCount = arrJson.getJSONObject(i).getInt("count");
        }

        if ("get_music_album".equals(mOperation)) {
            info.thumbnail = arrJson.getJSONObject(i).getString("thumbnail");
        }
    }

    public String getPath() {
        return mPath;
    }

}
