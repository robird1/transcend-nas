package com.transcend.nas.connection;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by steve_su on 2017/3/16.
 */

public class InviteShortLinkLoader extends AsyncTaskLoader<Boolean> {
    private static String TAG = InviteShortLinkLoader.class.getSimpleName();
    private Context mContext;
    private String mLongLink;
    private String mShortLink;

    public InviteShortLinkLoader(Context context, String url) {
        super(context);
        mContext = context;
        mLongLink = url;
    }

    @Override
    public Boolean loadInBackground() {
        mShortLink = getShortLink();
        return true;
    }

    public String getLink(){
        return mShortLink;
    }

    private String getShortLink() {
        Log.d(TAG, "[Enter] getShortLink");

        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\r\n   \"longDynamicLink\": \"" + mLongLink + "\"\r\n}");
        Request request = new Request.Builder()
                .url("https://firebasedynamiclinks.googleapis.com/v1/shortLinks?key=AIzaSyCJE9ebs5aa9totw5mGlyLNkoR5T17K9cY")
                .post(body)
                .addHeader("content-type", "application/json")
                .addHeader("cache-control", "no-cache")
                .build();

        try {
            Response response = client.newCall(request).execute();
            String responseMsg = response.body().string();

            Log.d(TAG, "responseMsg: "+ responseMsg);
            String shortLink = responseMsg.split("\"shortLink\": \"")[1].split("\",")[0];
            Log.d(TAG, "shortLink: "+ shortLink);
            return shortLink;

        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "[Enter] IOException e================================================================");

        }

        return "";
    }

}
