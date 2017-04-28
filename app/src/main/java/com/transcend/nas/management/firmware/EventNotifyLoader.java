package com.transcend.nas.management.firmware;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.realtek.nasfun.api.HttpClientManager;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerInfo;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASPref;
import com.tutk.IOTC.P2PService;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
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
 * Created by ikelee on 16/7/29.
 */
public class EventNotifyLoader extends AsyncTaskLoader<Boolean> {

    private static final String TAG = EventNotifyLoader.class.getSimpleName();
    private Bundle mArgs;
    private String mError;

    public EventNotifyLoader(Context context, Bundle args) {
        super(context);
        mArgs = args;
    }

    @Override
    public Boolean loadInBackground() {
        return getEventNotify();
    }

    private boolean getEventNotify() {
        boolean isSuccess = false, isValid = true;
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String hostname = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
        String hash = server.getHash();
        if(hash != null && !"".equals(hash)) {
            DefaultHttpClient httpClient = HttpClientManager.getClient();
            String commandURL = "http://" + hostname + "/nas/query/event_notify";
            HttpResponse response = null;
            InputStream inputStream = null;
            try {
                do {
                    HttpPost httpPost = new HttpPost(commandURL);
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                    nameValuePairs.add(new BasicNameValuePair("hash", hash));
                    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                    response = httpClient.execute(httpPost);
                    if (response == null) {
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
                        if (eventType == XmlPullParser.START_TAG) {
                            curTagName = tagName;
                            if (curTagName != null)
                                isSuccess = true;
                        } else if (eventType == XmlPullParser.TEXT) {
                            if (curTagName != null) {
                                text = xpp.getText();
                                if (curTagName.equals("reason")) {
                                    String reason = text;
                                    if (reason != null && reason.equals("Not Login")) {
                                        isValid = false;
                                    }
                                }

                            }
                        } else if (eventType == XmlPullParser.END_TAG) {
                            curTagName = null;
                        }
                        eventType = xpp.next();
                    } while (eventType != XmlPullParser.END_DOCUMENT);
                } while (false);

            } catch (XmlPullParserException e) {
                Log.d(TAG, "XML Parser error");
                e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG, "Fail to connect to server");
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "catch IllegalArgumentException");
                e.printStackTrace();
            } finally {
                try {
                    if (inputStream != null)
                        inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            isValid = false;
        }

        if(isSuccess) {
            Log.w(TAG, "hash key valid : " + isValid + ", hash : " + hash );
            if (!isValid) {
                Log.w(TAG, "hash key not valid, start login again");
                Server mServer = ServerManager.INSTANCE.getCurrentServer();
                String newHostname = P2PService.getInstance().getIP(mServer.getHostname(), P2PService.P2PProtocalType.HTTP);
                mServer.setHostname(newHostname);
                isSuccess = mServer.connect(false);
                if (isSuccess) {
                    ServerManager.INSTANCE.saveServer(mServer);
                    ServerManager.INSTANCE.setCurrentServer(mServer);
                    NASPref.setSessionVerifiedTime(getContext(), Long.toString(System.currentTimeMillis()));
                    Log.w(TAG, "hash key time update");

                    //check need to get server info or not
                    if(mServer.getServerInfo() == null)
                        server.doGetServerInfo(newHostname);
                } else {
                    mError = mServer.getLoginError();
                    Log.d(TAG, "login fail due to : " + mError);
                }
            } else {
                NASPref.setSessionVerifiedTime(getContext(), Long.toString(System.currentTimeMillis()));
                Log.w(TAG, "hash key time update");
            }
        } else {
            Log.w(TAG, "hash key check error");
        }

        return isSuccess;
    }

    public Bundle getBundleArgs(){
        return mArgs;
    }

    public String getError(){
        return mError;
    }
}
