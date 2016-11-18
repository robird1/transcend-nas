package com.transcend.nas.tutk;

import android.content.Context;
import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by ikelee on 06/4/16.
 */
public class TutkCreateNasLoader extends TutkBasicLoader {
    private String mToken;
    private String mName;
    private String mUUID;
    private String mNasID;
    private Bundle mArgs;

    public TutkCreateNasLoader(Context context, Bundle args) {
        this(context, args.getString("server"), args.getString("token"), args.getString("nasName"), args.getString("nasUUID"));
        mArgs = args;
    }

    public TutkCreateNasLoader(Context context, String server, String token, String nasName, String nasUUID) {
        super(context, server);
        mToken = token;
        mName = nasName;
        mUUID = nasUUID;
    }

    @Override
    public Boolean loadInBackground() {
        String url = doGenerateUrl();
        String param = "authToken=" + mToken + "&name=" + mName + "&uid=" + mUUID;
        String result = doPostRequest(url, param, mToken, false);
        boolean success = doParserResult(result);
        return success;
    }

    @Override
    protected boolean doParserResult(String result) {
        boolean success = doErrorParser(result);
        if (success) {
            try {
                JSONObject obj = new JSONObject(result);
                mNasID = obj.optString("nasId");
                mUUID = obj.optString("uid");
            } catch (JSONException e) {
                mNasID = "";
                e.printStackTrace();
            }
        }

        return success;
    }

    @Override
    protected String doGenerateUrl() {
        String url = getServer() + "/nas/create";
        return url;
    }

    public String getNasID(){
        return mNasID;
    }

    public String getNasUUID(){
        return mUUID;
    }

    public String getNasName(){
        return mName;
    }

    public String getAuthToken(){
        return mToken;
    }

    public Bundle getBundleArgs(){
        return mArgs;
    }
}
