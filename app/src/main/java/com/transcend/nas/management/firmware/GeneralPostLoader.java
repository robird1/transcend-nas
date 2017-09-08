package com.transcend.nas.management.firmware;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.tutk.IOTC.P2PService;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

/**
 * Created by steve_su on 2017/7/14.
 */

public abstract class GeneralPostLoader extends AsyncTaskLoader<Boolean> {
    private static final String TAG = GeneralPostLoader.class.getSimpleName();
    protected Context mContext;
    private String mValue = "";

    public GeneralPostLoader(Context context) {
        super(context);
        mContext = context;
    }

    public Boolean loadInBackground() {
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, onRequestBody());
        Request request = new Request.Builder()
                .url(onRequestUrl())
                .post(body)
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .build();

        try {
            okhttp3.Response response = client.newCall(request).execute();
            ResponseBody message = response.body();
            Log.d(TAG, "message: " + message);
            String value = doParse(message.string());
            Log.d(TAG, "value: " + value);
            onRequestFinish();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getValue() {
        return mValue;
    }

    protected String getHost() {
        Server server = ServerManager.INSTANCE.getCurrentServer();
        return P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
    }

    protected String getHash() {
        Server server = ServerManager.INSTANCE.getCurrentServer();
        return server.getHash();
    }

    private String doParse(String response) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(getInputStream(response), null);
            int eventType = parser.getEventType();

            do {
                String tagName = parser.getName();
                Log.d(TAG, "tagName: " + tagName);
                if (eventType == XmlPullParser.START_TAG) {
                    if (tagName.equals(onTagName())) {
                        parser.next();
                        mValue = parser.getText();
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

        return mValue;

    }

    private InputStream getInputStream(String response) {
        return new ByteArrayInputStream(response.getBytes(Charset.forName("UTF-8")));
    }

    protected abstract String onRequestBody();

    protected abstract String onRequestUrl();

    protected abstract String onTagName();

    protected boolean onRequestFinish() {
        return false;
    }

}
