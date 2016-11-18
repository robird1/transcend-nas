package com.transcend.nas.tutk;

import android.content.Context;


/**
 * Created by ikelee on 06/4/16.
 */
public class TutkDeleteNasLoader extends TutkBasicLoader {
    private String mToken;
    private String mNasID;

    public TutkDeleteNasLoader(Context context, String server, String token, String nasID) {
        super(context, server);
        mToken = token;
        mNasID = nasID;
    }

    @Override
    public Boolean loadInBackground() {
        String url = doGenerateUrl() + mNasID;
        String result = doDeleteRequest(url, mToken);
        boolean success = doParserResult(result);
        return success;
    }

    @Override
    protected boolean doParserResult(String result) {
        boolean success = doErrorParser(result);

        return success;
    }

    @Override
    protected String doGenerateUrl() {
        String url = getServer() + "/nas/";
        return url;
    }

    public String getNasID(){
        return mNasID;
    }

    public String getAuthToken(){
        return mToken;
    }
}
