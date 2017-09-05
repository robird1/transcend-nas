package com.transcend.nas.management.browser;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;
import android.util.Log;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASUtils;
import com.transcend.nas.management.FileInfo;
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
//        mFileList = new ArrayList<>();
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
                .addHeader("cache-control", "no-cache")
                .addHeader("postman-token", "49c6768a-d0d4-bf48-6282-87fc446b971b")
                .build();

        JSONObject jsonObj = null;
        try {
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();
            Log.d(TAG, "response.body().string(): "+ responseBody);
            jsonObj = new JSONObject(responseBody);
            int statusCode = jsonObj.getInt("status");
//            String detail = jsonObj.getString("detail");

            Log.d(TAG, "statusCode: "+ statusCode);
//            Log.d(TAG, "detail: "+ detail);

            if (statusCode == 0) {
                onLoadInBackground(jsonObj);
                return true;
            } else {
                Log.d(TAG, "[Enter] FAIL ============================================================");
                return false;
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            Log.d(TAG, "[Enter] JSONException====================================================");
            Log.d(TAG, "e.printStackTrace(): "+ e.getMessage());
            e.printStackTrace();

            if (jsonObj != null) {
                try {
                    JSONObject temp = jsonObj.getJSONObject("error");
                    String reason = temp.getString("reason");
                    if ("Not Login".equals(reason)) {
                        Log.d(TAG, "[Enter] NASUtils.reLogin *****************************************");
                        NASUtils.reLogin(mContext);
                        loadInBackground();
                        return true;
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

    protected String getRequestContent(String operation, String apiArgs) {
//        Log.d(TAG, "\n[Enter] getRequestContent");

        String content = "hash="+ getHash()+ "&fmt=json&op="+ operation;
        if (!TextUtils.isEmpty(apiArgs)) {
//            content.concat("\\&").concat(apiArgs);
            content = content + "&" + apiArgs;
        }
        return content;
    }

    public ArrayList<FileInfo> getFileList() {
        return mFileList;
    }

    public ArrayList<FileInfo> setFileList(ArrayList list) {
//        mFileList.clear();
//        mFileList.addAll(list);
        mFileList = list;
        return mFileList;
    }

    public Bundle getArgs() {
        return mArgs;
    }

}
