package com.transcend.nas.settings;

import android.content.AsyncTaskLoader;
import android.content.Context;

import com.realtek.nasfun.api.HttpClientManager;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.tutk.IOTC.P2PService;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ikelee on 16/12/09.
 */
public class FirmwareVersionLoader extends AsyncTaskLoader<Boolean> {

    private static final String TAG = FirmwareVersionLoader.class.getSimpleName();
    private static final String XML_TAG_FIRMWARE_VERSION = "current";
    private static final String XML_TAG_FIRMWARE_UPGRADE = "upgrade";
    private String mVersion = "";
    private String mIsUpgrade = "";

    public FirmwareVersionLoader(Context context) {
        super(context);
    }

    @Override
    public Boolean loadInBackground() {
        mVersion = getFirmwareVersion();
        return true;
    }

    public String getVersion(){
        return mVersion;
    }

    public String getIsUpgrade() {
        return mIsUpgrade;
    }

    private String getFirmwareVersion() {
        String firmwareVersion = null;
        HttpEntity entity = sendPostRequest();
        InputStream inputStream = null;
        String inputEncoding = null;

        if (entity != null) {
            try {
                inputStream = entity.getContent();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputEncoding = EntityUtils.getContentCharSet(entity);
        }

        if (inputEncoding == null) {
            inputEncoding = HTTP.DEFAULT_CONTENT_CHARSET;
        }

        if (inputStream != null) {
            firmwareVersion = doParse(inputStream, inputEncoding);
        }

        return firmwareVersion;
    }

    private HttpEntity sendPostRequest() {
        HttpEntity entity = null;
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String hostname = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
        String hash = server.getHash();
        String commandURL = "http://" + hostname + "/nas/firmware/getversion";

        HttpResponse response;
        try {
            HttpPost httpPost = new HttpPost(commandURL);
            List<NameValuePair> nameValuePairs = new ArrayList<>();
            nameValuePairs.add(new BasicNameValuePair("hash", hash));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            response = HttpClientManager.getClient().execute(httpPost);

            if (response != null) {
                entity = response.getEntity();
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return entity;
    }

    private String doParse(InputStream inputStream, String inputEncoding) {
        String firmwareVersion = null;
        XmlPullParserFactory factory;

        try {
            factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(inputStream, inputEncoding);
            int eventType = parser.getEventType();

            do {
                String tagName = parser.getName();
                if (eventType == XmlPullParser.START_TAG) {
                    if (tagName.equals(XML_TAG_FIRMWARE_VERSION)) {
                        parser.next();
                        firmwareVersion = parser.getText();
//                        break;
                    } else if (tagName.equals(XML_TAG_FIRMWARE_UPGRADE)) {
                        parser.next();
                        mIsUpgrade = parser.getText();
                        break;
                    }
                }

                eventType = parser.next();

            } while (eventType != XmlPullParser.END_DOCUMENT);

        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return firmwareVersion;
    }
}
