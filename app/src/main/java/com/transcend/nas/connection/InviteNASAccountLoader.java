package com.transcend.nas.connection;


import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

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
 * Created by steve_su on 2017/4/13.
 */

public class InviteNASAccountLoader extends AsyncTaskLoader<Boolean> {
    private static final String TAG = InviteNASAccountLoader.class.getSimpleName();
    private ArrayList<String> mAccountList;
    public InviteNASAccountLoader(Context context) {
        super(context);
    }

    @Override
    public Boolean loadInBackground() {
        mAccountList = queryList();
        return true;
    }

    private ArrayList queryList() {
        ArrayList list = null;
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
            list = doParse(inputStream, inputEncoding);
        }

        return list;
    }

    private HttpEntity sendPostRequest() {
        HttpEntity entity = null;
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String ip = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
        String hash = server.getHash();
        String commandURL = "http://" + ip + "/nas/get/users";
        Log.d(TAG, "hash: "+ hash);

        try {
            HttpPost httpPost = new HttpPost(commandURL);
            List<NameValuePair> nameValuePairs = new ArrayList<>();
            nameValuePairs.add(new BasicNameValuePair("hash", hash));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = HttpClientManager.getClient().execute(httpPost);

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

    private ArrayList doParse(InputStream inputStream, String inputEncoding) {
        ArrayList<String> accounts = new ArrayList<>();
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
                    if (tagName.equals("name")) {
                        parser.next();
                        accounts.add(parser.getText());
                    }
                }

                eventType = parser.next();

            } while (eventType != XmlPullParser.END_DOCUMENT);

        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return accounts;
    }


    public ArrayList<String> getAccountList() {
        return mAccountList;
    }
}
