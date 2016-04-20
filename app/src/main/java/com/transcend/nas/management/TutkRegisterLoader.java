package com.transcend.nas.management;

import android.app.Activity;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import com.transcend.nas.NASPref;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;


/**
 * Created by ikelee on 06/4/16.
 */
public class TutkRegisterLoader extends TutkBasicLoader {

    private String mEmail;
    private String mPwd;

    public TutkRegisterLoader(Context context, String server, String email, String pwd) {
        super(context, server);
        mEmail = email;
        mPwd = pwd;
    }

    @Override
    public Boolean loadInBackground() {
        String url = doGenerateUrl();
        String name = mEmail.split("@")[0];
        String param = "username="+ name + "&password=" + mPwd + "&email=" + mEmail + "&language=" + getSystemLanguage();
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
        String url = getServer() + "/users/create";
        return url;
    }

    public String getEmail(){
        return mEmail;
    }

    public String getPassword(){
        return mPwd;
    }
}
