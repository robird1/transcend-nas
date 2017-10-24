package com.transcend.nas.management.firmwareupdate;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.management.firmware.FirmwareHelper;
import com.tutk.IOTC.P2PService;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

/**
 * Created by steve_su on 2017/6/28.
 */

abstract class FirmwareLoader extends AsyncTaskLoader<Boolean> {
    private static final String TAG = FirmwareLoader.class.getSimpleName();
    private Context mContext;
    private boolean mIsHashValid = true;
    private int mRetryLoginCount = 2;

    FirmwareLoader(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public Boolean loadInBackground() {
        Log.d(TAG, "[Enter] loadInBackground");

        OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(10,
                TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build();

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, onRequestBody());
        Request request = new Request.Builder()
                .url(onRequestUrl())
                .post(body)
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("cache-control", "no-cache")
                .build();

        try {
            okhttp3.Response response = client.newCall(request).execute();
            ResponseBody message = response.body();
            if (message == null)
                return false;
            String msg = message.string();
            Log.d(TAG, "msg: "+ msg);
            boolean isSuccess = doParse(msg);

            if (!isSuccess && !mIsHashValid) {
                return retryLogin();
            }
            return isSuccess;

        } catch (IOException e) {
            Log.d(TAG, "[Enter] IOException e ===========================================");
            Log.d(TAG, e.toString());
            e.printStackTrace();
            return false;
        }
    }

    private Boolean retryLogin() {
        if (mRetryLoginCount == 0)
            return false;

        boolean isSuccess = new FirmwareHelper().doReLogin(getContext());
        mRetryLoginCount--;
        if (isSuccess) {
            return loadInBackground();
        } else {
            return false;
        }
    }

    String getHost() {
        Server server = ServerManager.INSTANCE.getCurrentServer();
        return P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
    }

    String getHash() {
        Server server = ServerManager.INSTANCE.getCurrentServer();
        return server.getHash();
    }

    String parse(String response, String tag) {
        int begin = response.indexOf(tag);
        String endTag = tag.replace("<", "</");
        int end = response.indexOf(endTag);
        if (begin == -1 || end == -1) {
            // return "" to parse the tag "percentage"
            // TODO check the response <retcode>
            return "";
        }
        begin += tag.length();
        return response.substring(begin, end);
    }

    protected boolean doParse(String response) {
        return checkHash(response);
    }

    private boolean checkHash(String response) {
        String reason = parse(response, "<reason>");
        if (reason != null && reason.equals("No Permission")) {
            mIsHashValid = false;
        } else {
            mIsHashValid = true;
        }
        return mIsHashValid;
    }

    boolean isHashValid() {
        return mIsHashValid;
    }

    boolean isDataValid(String... arg) {
        for (String a: arg) {
            if (TextUtils.isEmpty(a)) {
                return false;
            }
        }
        return true;
    }

    protected Bundle getData() {
        return new Bundle();
    }

    protected abstract String onRequestBody();
    protected abstract String onRequestUrl();

}
