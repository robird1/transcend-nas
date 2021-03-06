package com.transcend.nas.connection;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.realtek.nasfun.api.HttpClientManager;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASPref;
import com.tutk.IOTC.P2PService;

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
 * Created by silverhsu on 16/1/5.
 */
public class WizardCheckLoader extends AsyncTaskLoader<Boolean> {

    private String mUrl;
    private Bundle mArgs;
    private boolean isWizard = false;
    private String mModel = "";
    private String mSerialNum = "";
    private String mHwAddr = "";
    private String mIpAddr = "";

    public WizardCheckLoader(Context context, Bundle args) {
        super(context);
        mArgs = args;
    }

    @Override
    public Boolean loadInBackground() {
        boolean success = false;
        mUrl = "http://" + mArgs.getString("hostname") + "/nas/get/register";
        try {
            String commandURL = mUrl;
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
                                }
                            } else if (curTagName.equals("model")) {
                                String model = text;
                                if (model != null) {
                                    mModel = model;
                                }
                            } else if (curTagName.equals("serialnum")) {
                                String serialNum = text;
                                if (serialNum != null) {
                                    mSerialNum = serialNum;
                                    NASPref.setSerialNum(getContext(), mSerialNum);
                                }
                            } else if (curTagName.equals("hwaddr")) {
                                String hwAddr = text;
                                if (hwAddr != null) {
                                    mHwAddr = hwAddr;
                                }
                            } else if (curTagName.equals("ipaddr")) {
                                String ipAddr = text;
                                if (ipAddr != null) {
                                    mIpAddr = ipAddr;
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

        return success;
    }

    public Bundle getBundleArgs() {
        return mArgs;
    }

    public String getModel() {
        return mModel;
    }

    public String getSerialNum() {
        return mSerialNum;
    }

    public String getMacAddress() {
        return mHwAddr;
    }

    public String getIpAddress() {
        return mIpAddr;
    }

    public boolean isWizard() {
        return isWizard;
    }

}
