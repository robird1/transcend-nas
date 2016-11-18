package com.transcend.nas.common;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpRequestFactory {
    private static final String TAG = "HttpRequestFactory";

    public static String doGetRequest(String url) {
        return doGetRequest(url, false);
    }

    public static String doGetRequest(String url, boolean json) {
        HttpURLConnection conn = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            conn = (HttpURLConnection) realUrl.openConnection();
            if(json)
                conn.setRequestProperty("content-type", "application/json");
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(10000);

            int responseCode = conn.getResponseCode();
            Log.i(TAG, "url " + url);
            Log.i(TAG, "responseCode: " + responseCode);
            if (responseCode == 200 || responseCode == 201) {
                InputStream is = conn.getInputStream();
                result = getStringFromInputStream(is);
            } else {
                Log.i(TAG, "error " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return result;
    }

    public static String getStringFromInputStream(InputStream is)
            throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = -1;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
        is.close();
        String state = os.toString();
        os.close();
        return state;
    }
}
