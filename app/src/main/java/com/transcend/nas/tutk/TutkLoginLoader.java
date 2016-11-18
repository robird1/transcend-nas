package com.transcend.nas.tutk;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by ikelee on 06/4/16.
 */
public class TutkLoginLoader extends TutkBasicLoader {

    private String mEmail;
    private String mPwd;
    private String mAuthToken = "";
    private String mAuthTokenExpirateDate = "";

    public TutkLoginLoader(Context context, String server, String email, String pwd) {
        super(context, server);
        mEmail = email;
        mPwd = pwd;
    }

    @Override
    public Boolean loadInBackground() {
        String url = doGenerateUrl();
        String param = "password=" + mPwd + "&email=" + mEmail + "&serialNum=" + getDeviceID() + "&platform=android";
        String result = doPostRequest(url, param);
        boolean success = doParserResult(result);
        return success;
    }

    @Override
    protected boolean doParserResult(String result) {
        boolean success = doErrorParser(result);
        if(success) {
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
        String url = getServer() + "/users/login";
        return url;
    }

    public String getAuthToke(){
        return mAuthToken;
    }

    public String getAuthTokenExpirateDate(){
        return mAuthTokenExpirateDate;
    }

    public String getEmail(){
        return mEmail;
    }

    public String getPassword(){
        return mPwd;
    }
}
