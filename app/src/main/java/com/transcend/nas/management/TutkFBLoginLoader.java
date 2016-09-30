package com.transcend.nas.management;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

/**
 * Created by steve_su on 2016/9/13.
 */
public class TutkFBLoginLoader extends TutkBasicLoader {

    private static final String TAG = TutkFBLoginLoader.class.getSimpleName();

    private final String mProviderName = "facebook";
    private final String mPlatform = "android";
    private String mEmail;
    private String mPwd;
    private String mUserName;
    private String mUID;
    private String mToken;
    private String mAuthToken = "";
    private String mAuthTokenExpirateDate = "";

    public TutkFBLoginLoader(Context context, String server, String email, String userName, String uid, String token) {
        super(context, server);
        mEmail = email;
        mPwd = uid;
        mUserName = userName;
        mUID = uid;
        mToken = token;
    }

    @Override
    public Boolean loadInBackground() {

        Log.d(TAG, "[Enter] loadInBackground()");

        String param = "{\"username\":\"" + mUserName + "\"" +
                ",\"email\":\"" + getEmail() + "\"" +
                ",\"provider\":\"" + mProviderName + "\"" +
                ",\"thirdPartyId\":\"" + mUID + "\"" +
                ",\"thirdPartyToken\":\"" + mToken + "\"" +
                ",\"platform\":\"" + mPlatform + "\"" +
                ",\"serialNum\":\"" + getDeviceID() + "\"" +
                ",\"language\":\"" + getSystemLanguage() + "\"" +
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
    protected boolean doParserResult(String result) {
        boolean success = doErrorParser(result);
        if (success) {
            JSONObject obj;
            try {
                obj = new JSONObject(result);
                mAuthToken = obj.optString("authToken");
                mAuthTokenExpirateDate = obj.optString("authTokenExpirateDate");
            } catch (JSONException e) {
                mAuthToken = "";
                mAuthTokenExpirateDate = "";
                e.printStackTrace();
            }
        }

        return success;
    }

    @Override
    protected String doGenerateUrl() {
        return getServer() + "/users/signin";
    }

    public String getAuthToke() {
        return mAuthToken;
    }

    public String getAuthTokenExpirateDate() {
        return mAuthTokenExpirateDate;
    }

    public String getEmail() {
        return mEmail;
    }

    public String getPassword() {
        return mPwd;
    }
}
