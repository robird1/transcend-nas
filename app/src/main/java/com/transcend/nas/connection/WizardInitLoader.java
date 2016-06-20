package com.transcend.nas.connection;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.realtek.nasfun.api.HttpClientManager;
import com.realtek.nasfun.api.ServerInfo;
import com.transcend.nas.R;
import com.tutk.IOTC.P2PService;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by silverhsu on 16/1/5.
 */
public class WizardInitLoader extends AsyncTaskLoader<Boolean> {

    private static final String TAG = WizardInitLoader.class.getSimpleName();
    private String mUrl;
    private Bundle mArgs;
    private boolean mRemoteAccess = false;
    private String mError = null;

    public WizardInitLoader(Context context, Bundle args, boolean isRemoteAccess) {
        super(context);
        mRemoteAccess = isRemoteAccess;
        mArgs = args;
    }

    @Override
    public Boolean loadInBackground() {
        boolean success = false;
        if(mRemoteAccess){
            mUrl = null;
            String uuid = mArgs.getString("hostname");
            if (uuid != null && !uuid.equals("")) {
                P2PService.getInstance().stopP2PConnect();
                int result = P2PService.getInstance().startP2PConnect(uuid);
                if (result >= 0) {
                    mUrl = "http://" + P2PService.getInstance().getP2PIP() + ":" + P2PService.getInstance().getP2PPort(P2PService.P2PProtocalType.HTTP) + "/nas/wizard/";
                } else {
                    P2PService.getInstance().stopP2PConnect();
                }
            }
        }
        else{
            mUrl = "http://" + mArgs.getString("hostname") + "/nas/wizard/";
        }

        if(mUrl != null) {
            success = doSetPass(mUrl) && doSetZone(mUrl) && doSetInit(mUrl);
        }

        return success;
    }

    private boolean doSetPass(String url){
        boolean success = false;

        DefaultHttpClient httpClient = HttpClientManager.getClient();
        String commandURL = url;
        HttpResponse response = null;
        InputStream inputStream = null;
        try {
            do{
                HttpPost httpPost = new HttpPost(commandURL);
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("method", "setpass"));
                nameValuePairs.add(new BasicNameValuePair("user", "admin"));
                nameValuePairs.add(new BasicNameValuePair("pass", mArgs.getString("password")));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                response = httpClient.execute(httpPost);
                if(response == null) {
                    Log.e(TAG, "response is null");
                    break;
                }
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    Log.e(TAG, "response entity is null");
                    break;
                }
                inputStream = entity.getContent();
                String inputEncoding = EntityUtils.getContentCharSet(entity);
                if (inputEncoding == null) {
                    inputEncoding = HTTP.DEFAULT_CONTENT_CHARSET;
                }

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(inputStream, inputEncoding);
                int eventType = xpp.getEventType();
                String curTagName = null;
                String text = null;

                do {
                    String tagName = xpp.getName();
                    if(eventType == XmlPullParser.START_TAG) {
                        curTagName = tagName;
                    }
                    else if(eventType == XmlPullParser.TEXT) {
                        if(curTagName != null){
                            text = xpp.getText();
                            if(curTagName.equals("detail")){
                                 if(text.equals("OK"))
                                     success = true;
                            }

                            if(curTagName.equals("wizard")){
                                if(text.equals("NAS has been intialized"))
                                    mError = "StoreJet Cloud has been intialized";
                            }
                        }
                    }
                    else if(eventType == XmlPullParser.END_TAG) {
                        curTagName = null;
                    }
                    eventType = xpp.next();
                } while (eventType != XmlPullParser.END_DOCUMENT);
            }while(false);

        } catch (XmlPullParserException e) {
            Log.d(TAG, "XML Parser error");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "Fail to connect to server");
            e.printStackTrace();
        } catch(IllegalArgumentException e){
            Log.d(TAG, "catch IllegalArgumentException");
            e.printStackTrace();
        } finally {
            try {
                if(inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                success = false;
                e.printStackTrace();
            }
        }

        Log.d(TAG, "set pass : " + success);
        return success;
    }

    private boolean doSetZone(String url){
        boolean success = false;

        DefaultHttpClient httpClient = HttpClientManager.getClient();
        String commandURL = url;
        HttpResponse response = null;
        InputStream inputStream = null;
        try {
            do{
                HttpPost httpPost = new HttpPost(commandURL);
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("method", "setzone"));
                nameValuePairs.add(new BasicNameValuePair("zone", mArgs.getString("timezone")));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                response = httpClient.execute(httpPost);
                if(response == null) {
                    Log.e(TAG, "response is null");
                    break;
                }
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    Log.e(TAG, "response entity is null");
                    break;
                }
                inputStream = entity.getContent();
                String inputEncoding = EntityUtils.getContentCharSet(entity);
                if (inputEncoding == null) {
                    inputEncoding = HTTP.DEFAULT_CONTENT_CHARSET;
                }

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(inputStream, inputEncoding);
                int eventType = xpp.getEventType();
                String curTagName = null;
                String text = null;

                do {
                    String tagName = xpp.getName();
                    if(eventType == XmlPullParser.START_TAG) {
                        curTagName = tagName;
                    }
                    else if(eventType == XmlPullParser.TEXT) {
                        if(curTagName != null){
                            text = xpp.getText();
                            if(curTagName.equals("date")){
                                if(text.equals("update time zone"))
                                    success = true;
                            }
                        }
                    }
                    else if(eventType == XmlPullParser.END_TAG) {
                        curTagName = null;
                    }
                    eventType = xpp.next();
                } while (eventType != XmlPullParser.END_DOCUMENT);
            }while(false);

        } catch (XmlPullParserException e) {
            Log.d(TAG, "XML Parser error");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "Fail to connect to server");
            e.printStackTrace();
        } catch(IllegalArgumentException e){
            Log.d(TAG, "catch IllegalArgumentException");
            e.printStackTrace();
        } finally {
            try {
                if(inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                success = false;
                e.printStackTrace();
            }
        }

        Log.d(TAG, "set zone : " + success);
        return success;
    }

    private boolean doSetInit(String url){
        boolean success = false;

        DefaultHttpClient httpClient = HttpClientManager.getClient();
        String commandURL = url;
        HttpResponse response = null;
        InputStream inputStream = null;
        try {
            do{
                HttpPost httpPost = new HttpPost(commandURL);
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("method", "setinit"));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                response = httpClient.execute(httpPost);
                if(response == null) {
                    Log.e(TAG, "response is null");
                    break;
                }
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    Log.e(TAG, "response entity is null");
                    break;
                }
                inputStream = entity.getContent();
                String inputEncoding = EntityUtils.getContentCharSet(entity);
                if (inputEncoding == null) {
                    inputEncoding = HTTP.DEFAULT_CONTENT_CHARSET;
                }

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(inputStream, inputEncoding);
                int eventType = xpp.getEventType();
                String curTagName = null;
                String text = null;

                do {
                    String tagName = xpp.getName();
                    if(eventType == XmlPullParser.START_TAG) {
                        curTagName = tagName;
                    }
                    else if(eventType == XmlPullParser.TEXT) {
                        if(curTagName != null){
                            text = xpp.getText();
                            if(curTagName.equals("detail")){
                                if(text.equals("OK"))
                                    success = true;
                            }
                        }
                    }
                    else if(eventType == XmlPullParser.END_TAG) {
                        curTagName = null;
                    }
                    eventType = xpp.next();
                } while (eventType != XmlPullParser.END_DOCUMENT);
            }while(false);

        } catch (XmlPullParserException e) {
            Log.d(TAG, "XML Parser error");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "Fail to connect to server");
            e.printStackTrace();
        } catch(IllegalArgumentException e){
            Log.d(TAG, "catch IllegalArgumentException");
            e.printStackTrace();
        } finally {
            try {
                if(inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                success = false;
                e.printStackTrace();
            }
        }

        Log.d(TAG, "set init : " + success);
        return success;
    }

    public Bundle getBundleArgs(){
        return mArgs;
    }

    public String getErrorResult(){
        if(mError != null)
            return mError;
        else
            return getContext().getString(R.string.network_error);
    }

}
