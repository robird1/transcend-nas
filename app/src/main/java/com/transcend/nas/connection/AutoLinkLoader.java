package com.transcend.nas.connection;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;

import com.realtek.nasfun.api.HttpClientManager;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASPref;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by silverhsu on 15/12/30.
 */
public class AutoLinkLoader extends AsyncTaskLoader<Boolean> {

    private static final String TAG = AutoLinkLoader.class.getSimpleName();

    private ConnectivityManager mConnMgr;
    private Server mServer;
    private boolean isWizard = false;
    private boolean isRemote = false;
    private Context mContext;
    private String mHostname;
    private String mUsername;
    private String mPassword;

    public enum LinkType {
        NO_LINK, INTRANET, INTERNET
    }

    public AutoLinkLoader(Context context, Bundle args) {
        super(context);
        mContext = context;
        mConnMgr = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        mHostname = args.getString("hostname");
        mUsername = args.getString("username");
        mPassword = args.getString("password");
        isRemote = args.getBoolean("RemoteAccess");
    }

    @Override
    public Boolean loadInBackground() {
        Log.d(TAG, "AutoLink : " + mHostname + ", " + mUsername + "," + mPassword);
        if (checkNetworkAvailable()) {
            if (mHostname.isEmpty() || mUsername.isEmpty() || mPassword.isEmpty()) {
            } else {
                if (doWizardCheck(mHostname)) {
                    if (isWizard) {
                        return login(mHostname, mUsername, mPassword);
                    }
                }
            }
        }
        return false;
    }

    private boolean checkNetworkAvailable() {
        NetworkInfo info = mConnMgr.getActiveNetworkInfo();
        if (info == null)
            return false;
        boolean isWiFi = (info.getType() == ConnectivityManager.TYPE_WIFI);
        boolean isMobile = (info.getType() == ConnectivityManager.TYPE_MOBILE);
        Log.w(TAG, "Wi-Fi: " + isWiFi);
        Log.w(TAG, "Mobile: " + isMobile);
        return (isWiFi || isMobile);
    }

    private boolean doWizardCheck(String hostname) {
        isWizard = false;
        boolean success = false;
        String commandURL = "http://" + hostname + "/nas/get/register";
        Log.d(TAG, "Wizard check : " + commandURL);

        try {
            DefaultHttpClient httpClient = HttpClientManager.getClient();
            HttpGet httpGet = new HttpGet(commandURL);
            HttpResponse httpResponse;
            httpResponse = httpClient.execute(httpGet);
            HttpEntity httpEntity = httpResponse.getEntity();
            InputStream inputStream = httpEntity.getContent();
            String inputEncoding = EntityUtils.getContentCharSet(httpEntity);
            if (inputEncoding == null) {
                inputEncoding = HTTP.DEFAULT_CONTENT_CHARSET;
            }

            try {
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
                    } else if (eventType == XmlPullParser.TEXT) {
                        if (curTagName != null) {
                            text = xpp.getText();
                            if (curTagName.equals("initialized")) {
                                String initialized = text;
                                if (initialized != null) {
                                    isWizard = initialized.equals("yes");
                                    break;
                                }
                            }
                        }
                    }

                    eventType = xpp.next();

                } while (eventType != XmlPullParser.END_DOCUMENT);
                success = true;
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }


        Log.d(TAG, "Wizard check : " + success + ", isWizard : " + isWizard);
        return success;
    }

    private boolean login(String hostname, String username, String password) {
        mServer = new Server(hostname, username, password);
        boolean isConnected = mServer.connect(false);
        if (isConnected) {
            updateServerManager();
            NASPref.setHostname(getContext(), hostname);
        }

        Log.d(TAG, "AutoLink : " + isConnected);
        Log.d(TAG, "AutoLink ip: " + hostname);
        Log.d(TAG, "AutoLink username: " + username);
        Log.d(TAG, "AutoLink password: " + password);
        return isConnected;
    }

    private void updateServerManager() {
        ServerManager.INSTANCE.saveServer(mServer);
        ServerManager.INSTANCE.setCurrentServer(mServer);
        NASPref.setSessionVerifiedTime(getContext(), Long.toString(System.currentTimeMillis()));
    }

    public boolean isWizard() {
        return isWizard;
    }

    public boolean isRemote(){
        return isRemote;
    }

}
