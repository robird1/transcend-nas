package com.transcend.nas.management.browser;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.firmware.FirmwareHelper;
import com.tutk.IOTC.P2PService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by steve_su on 2017/8/9.
 */

public abstract class TwonkyGeneralPostLoader extends AsyncTaskLoader<Boolean> {
    private static final String TAG = TwonkyGeneralPostLoader.class.getSimpleName();
    protected Context mContext;
    protected Bundle mArgs;
    private String mValue = "";
    private ArrayList<FileInfo> mFileList;

    public TwonkyGeneralPostLoader(Context context, Bundle args) {
        super(context);
        mContext = context;
        mArgs = args;
    }

    @Override
    public Boolean loadInBackground() {
        Log.d(TAG, "\n[Enter] loadInBackground");
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, onRequestBody());
        Log.d(TAG, "onRequestBody(): "+ onRequestBody());
        Request request = new Request.Builder()
                .url(onRequestUrl())
                .post(body)
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .build();

        JSONObject jsonObj = null;
        try {
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();
            Log.d(TAG, "response.body().string(): "+ responseBody);

            jsonObj = new JSONObject(responseBody);
            int statusCode = jsonObj.getInt("status");
            Log.d(TAG, "statusCode: "+ statusCode);
            if (statusCode == 0) {
                return onLoadInBackground(jsonObj);
            } else {
                Log.d(TAG, "[Enter] FAIL ===============================================");
                return false;
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            Log.d(TAG, "[Enter] JSONException =========================================");
            Log.d(TAG, "e.printStackTrace(): "+ e.getMessage());
            e.printStackTrace();

            if (jsonObj != null) {
                try {
                    JSONObject temp = jsonObj.getJSONObject("error");
                    String reason = temp.getString("reason");
                    if ("Not Login".equals(reason)) {
                        boolean isSuccess = new FirmwareHelper().doReLogin(mContext);
                        if (isSuccess) {
                            return loadInBackground();
                        }
                    }
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
            }

        }
        return false;
    }

//    @Override
//    protected void onStartLoading() {
//        super.onStartLoading();
//        Log.d(TAG, "[Enter] onStartLoading");
//        forceLoad();
//    }
//
//    @Override
//    protected void onStopLoading() {
//        super.onStopLoading();
//        Log.d(TAG, "[Enter] onStopLoading");
//        cancelLoad();
//    }

    public String getValue() {
        return mValue;
    }

    protected String getHost() {
        Server server = ServerManager.INSTANCE.getCurrentServer();
        return P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
    }

    protected String getHash() {
        Server server = ServerManager.INSTANCE.getCurrentServer();
        return server.getHash();
    }

    protected abstract String onRequestBody();
    protected abstract String onRequestUrl();
    protected abstract Boolean onLoadInBackground(JSONObject jsonObject);

    public ArrayList<FileInfo> setFileList(ArrayList list) {
        mFileList = list;
        return mFileList;
    }

    public ArrayList<FileInfo> getFileList() {
        return mFileList;
    }

}