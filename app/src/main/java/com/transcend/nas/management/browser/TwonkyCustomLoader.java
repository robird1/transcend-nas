package com.transcend.nas.management.browser;

import android.content.Context;
import android.os.Bundle;

import com.transcend.nas.management.FileInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by steve_su on 2017/8/10.
 */

public class TwonkyCustomLoader extends TwonkyGeneralPostLoader {
    private String mPath;
    private String mOperation;
    private String mApiArgs;

    TwonkyCustomLoader(Context context, Bundle args) {
        super(context, args);
        mOperation = args.getString("op");
        mApiArgs = args.getString("api_args");
        mPath = args.getString("path");
    }

    @Override
    protected String onRequestBody() {
        return "hash="+ getHash()+ "&fmt=json&op="+ mOperation+ "&" + mApiArgs;
    }

    @Override
    protected String onRequestUrl() {
        return "http://"+ getHost()+ "/nas/get/twonkycustom";
    }

    @Override
    protected Boolean onLoadInBackground(JSONObject jsonObj) {
        ArrayList<FileInfo> list = new ArrayList<>();
        try {
            JSONArray arrJson = jsonObj.getJSONArray("result");
            for (int i = 0; i < arrJson.length(); i++) {
                JSONObject object = arrJson.getJSONObject(i);
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

    public String getPath() {
        return mPath;
    }

}
