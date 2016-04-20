package com.transcend.nas.management;

import android.content.Context;


/**
 * Created by ikelee on 06/4/16.
 */
public class TutkResendActivateLoader extends TutkBasicLoader {

    private String mEmail;

    public TutkResendActivateLoader(Context context, String server, String email) {
        super(context, server);
        mEmail = email;
    }

    @Override
    public Boolean loadInBackground() {
        String url = doGenerateUrl();
        String param = "email=" + mEmail;
        String result = doPostRequest(url, param);
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
        String url = getServer() + "/api/resendActivate";
        return url;
    }

    public String getEmail(){
        return mEmail;
    }
}
