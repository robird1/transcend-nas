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
 * Created by steve_su on 2017/7/12.
 */

public class ConfigNTPServerLoader extends AsyncTaskLoader<Boolean> {
    private static final String TAG = ConfigNTPServerLoader.class.getSimpleName();
    private Context mContext;
    private String mResult = "";

    public ConfigNTPServerLoader(Context context) {
        super(context);
        mContext = context;
    }

    public Boolean loadInBackground() {
        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, getRequestBody());
        Request request = new Request.Builder()
                .url(getRequestUrl())
                .post(body)
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("cache-control", "no-cache")
                .addHeader("postman-token", "49c6768a-d0d4-bf48-6282-87fc446b971b")
                .build();

        try {
            okhttp3.Response response = client.newCall(request).execute();
            ResponseBody message = response.body();
            Log.d(TAG, "message: "+ message);
            String result = doParse(message.string());
            Log.d(TAG, "result: "+ result);

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    String getHost() {
        Server server = ServerManager.INSTANCE.getCurrentServer();
        return P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
    }

    String getHash() {
        Server server = ServerManager.INSTANCE.getCurrentServer();
        return server.getHash();
    }

    private String doParse(String response) {
        Log.d(TAG, "[Enter] doParse response: "+ response);
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(getInputStream(response), null);
            int eventType = parser.getEventType();

            do {
                String tagName = parser.getName();
                Log.d(TAG, "tagName: "+ tagName);
                if (eventType == XmlPullParser.START_TAG) {
                    if (tagName.equals("date")) {
                        parser.next();
                        mResult = parser.getText();
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

        return mResult;

    }

    private InputStream getInputStream(String response) {
        return new ByteArrayInputStream(response.getBytes(Charset.forName("UTF-8")));
    }

    private String getRequestBody() {
        return "hash="+ getHash()+ "&date=now&sync=daily&manual=yes&myntp=time.google.com";
    }

    private String getRequestUrl() {
        return "http://"+ getHost()+"/nas/set/date";
    }

    public String getConfigResult() {
        return mResult;
    }

}
