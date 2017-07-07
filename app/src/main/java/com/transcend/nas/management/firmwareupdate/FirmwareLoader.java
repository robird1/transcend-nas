package com.transcend.nas.management.firmwareupdate;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.tutk.IOTC.P2PService;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

/**
 * Created by steve_su on 2017/6/28.
 */

abstract class FirmwareLoader {
    private Context mContext;
    private boolean isHashValid = true;

    FirmwareLoader(Context context) {
        mContext = context;
    }

    public Boolean loadInBackground() {
        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, onRequestBody());
        Request request = new Request.Builder()
                .url(onRequestUrl())
                .post(body)
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("cache-control", "no-cache")
                .addHeader("postman-token", "24c0f25c-3c3f-bfd6-776e-125cc10d2e95")
                .build();

        try {
            okhttp3.Response response = client.newCall(request).execute();
            ResponseBody message = response.body();
            return message != null && doParse(message.string());

        } catch (IOException e) {
            e.printStackTrace();
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
            isHashValid = false;
        }
        return isHashValid;
    }

    boolean isHashValid() {
        return isHashValid;
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
