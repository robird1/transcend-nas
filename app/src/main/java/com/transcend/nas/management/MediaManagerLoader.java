package com.transcend.nas.management;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

import com.realtek.nasfun.api.HttpClientManager;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by ikeLee on 16/5/26.
 */
public class MediaManagerLoader extends AsyncTaskLoader<Boolean> {

    private static final String TAG = MediaManagerLoader.class.getSimpleName();
    private Bundle mArgs;

    public MediaManagerLoader(Context context, Bundle args) {
        super(context);
        mArgs = args;
    }

    @Override
    public Boolean loadInBackground() {
        boolean isSuccess = false;
        String url = mArgs.getString("path");
        Log.d(TAG, url);
        HttpClient client = new DefaultHttpClient();
        HttpParams params = client.getParams();
        HttpClientParams.setRedirecting(params, false);
        try {
            do{
                HttpGet httpPost = new HttpGet(url);
                HttpResponse response = client.execute(httpPost);
                String redirect = response.getLastHeader("Location").getValue();
                if(redirect != null && !redirect.equals("")) {
                    mArgs.putString("path",redirect);
                    isSuccess = true;
                }
            }while(false);
        } catch (IOException e) {
            Log.d(TAG, "Fail to connect to server");
            e.printStackTrace();
        } catch(IllegalArgumentException e){
            Log.d(TAG, "catch IllegalArgumentException");
            e.printStackTrace();
        } catch(NullPointerException e){
            Log.d(TAG, "catch NullPointerException");
            e.printStackTrace();
        }

        return isSuccess;
    }

    public Bundle getBundleArgs(){
        return mArgs;
    }
}
