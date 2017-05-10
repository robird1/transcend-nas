package com.transcend.nas.common;

import android.text.TextUtils;
import android.util.Log;

import com.realtek.nasfun.api.HttpClientManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Text;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpRequestFactory {
    private static final String TAG = "HttpRequestFactory";

    public static Map<String, String> doHeadRequest(String url) {
        HttpURLConnection conn = null;
        Map<String, String> result = new HashMap<>();
        try {
            URL realUrl = new URL(url);
            conn = (HttpURLConnection) realUrl.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(10000);

            int responseCode = conn.getResponseCode();
            Log.i(TAG, "url " + url);
            Log.i(TAG, "responseCode: " + responseCode);
            if (responseCode == 200 || responseCode == 201) {
                Map<String,List<String>> map = conn.getHeaderFields();
                for(String key : map.keySet()) {
                    List<String> value = map.get(key);
                    if(value != null)
                        result.put(key, TextUtils.join(",", value));
                }
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

    public static HashMap<String, String> doXmlGetRequest(String url, ArrayList<String> keywords){
        HashMap<String, String> results = new HashMap<>();
        try {
            DefaultHttpClient httpClient = HttpClientManager.getClient();
            HttpGet httpGet = new HttpGet(url);
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

                do {
                    String tagName = xpp.getName();
                    if (eventType == XmlPullParser.START_TAG) {
                        curTagName = tagName;
                    } else if (eventType == XmlPullParser.TEXT) {
                        if(keywords.contains(curTagName)) {
                            String currValue = results.get(curTagName);
                            String value = xpp.getText();
                            if(currValue != null && !"".equals(currValue))
                                currValue = currValue + "|" + value;
                            else
                                currValue = value;
                            results.put(curTagName, currValue);
                        }
                    }
                    eventType = xpp.next();

                } while (eventType != XmlPullParser.END_DOCUMENT);
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

        return results;
    }

    private static String getStringFromInputStream(InputStream is)
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
