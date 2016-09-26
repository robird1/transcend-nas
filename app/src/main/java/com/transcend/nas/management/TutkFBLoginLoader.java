package com.transcend.nas.management;

import android.content.Context;
import android.util.Log;

/**
 * Created by steve_su on 2016/9/13.
 */
public class TutkFBLoginLoader extends TutkLoginLoader {

    private static final String TAG = TutkFBLoginLoader.class.getSimpleName();

    private static final String mPassword = "";
    private static final String mProviderName = "facebook";
    private static final String mPlatform = "android";
    private String mUserName;
    private String mUID;
    private String mToken;

    public TutkFBLoginLoader(Context context, String server, String email, String userName, String uid, String token) {
        super(context, server, email, mPassword);

        mUserName = userName;
        mUID= uid;
        mToken = token;
    }

    @Override
    public Boolean loadInBackground() {

        Log.d(TAG, "[Enter] loadInBackground()");

        String param = "{\"username\":\""+ mUserName + "\"" +
                ",\"email\":\""+ getEmail() + "\"" +
                ",\"provider\":\""+ mProviderName + "\"" +
                ",\"thirdPartyId\":\""+ mUID + "\"" +
                ",\"thirdPartyToken\":\""+ mToken + "\"" +
                ",\"platform\":\""+ mPlatform + "\"" +
                ",\"serialNum\":\""+ getDeviceID() + "\"" +
                ",\"language\":\""+ getSystemLanguage() + "\"" +
                "}";


        Log.d(TAG, "mUserName: " + mUserName);
        Log.d(TAG, "email: " + getEmail());
        Log.d(TAG, "thirdPartyId: " + mUID);
        Log.d(TAG, "thirdPartyToken: " + mToken);
        Log.d(TAG, "serialNum: " + getDeviceID());

        String result = doJsonPostRequest(doGenerateUrl(), param);

        return doParserResult(result);
    }

    @Override
    protected String doGenerateUrl() {

        return getServer() + "/users/signin";
    }
}
