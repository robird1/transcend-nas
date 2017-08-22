package com.transcend.nas;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;

import com.realtek.nasfun.api.HttpClientManager;
import com.realtek.nasfun.api.Server;
import com.transcend.nas.management.firmware.FirmwareHelper;

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

    private boolean isWizard = false;
    private boolean isRemote = false;
    private Bundle mArgs;

    public AutoLinkLoader(Context context, Bundle args) {
        super(context);
        mArgs = args;
        if (args != null)
            isRemote = args.getBoolean("RemoteAccess");
    }

    @Override
    public Boolean loadInBackground() {
        if (mArgs == null)
            return false;

        String hostname = mArgs.getString("hostname");
        String username = mArgs.getString("username");
        String password = mArgs.getString("password");
        Log.d(TAG, "AutoLink : " + hostname + ", " + username + "," + password);
        if (hostname.isEmpty() || username.isEmpty() || password.isEmpty())
            return false;

        if (!checkNetworkAvailable())
            return false;

        if (!doWizardCheck(hostname))
            return false;

        if (isWizard) {
            Server server = new Server(hostname, username, password);
            FirmwareHelper helper = new FirmwareHelper();
            return helper.doLogin(getContext(), server, false);
        }

        return false;
    }

    private boolean checkNetworkAvailable() {
        ConnectivityManager connMgr = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connMgr.getActiveNetworkInfo();
        if (info == null)
            return false;

        boolean isWiFi = (ConnectivityManager.TYPE_WIFI == info.getType());
        boolean isMobile = (ConnectivityManager.TYPE_MOBILE == info.getType());
        boolean isAvailable = isWiFi || (isRemote ? isMobile : false);
        Log.w(TAG, "Wi-Fi: " + isWiFi);
        Log.w(TAG, "Mobile: " + isMobile);
        Log.w(TAG, "checkNetworkAvailable: " + isAvailable);
        return isAvailable;
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

    public boolean isWizard() {
        return isWizard;
    }

    public boolean isRemote() {
        return isRemote;
    }

}
