package com.transcend.nas.management;

import android.app.Activity;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import com.realtek.nasfun.api.HttpClientManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by ikelee on 06/4/16.
 */
public abstract class TutkBasicLoader extends AsyncTaskLoader<Boolean> {

    public static final String TAG = TutkBasicLoader.class.getSimpleName();
    public Activity mActivity;
    private String mServer = "https://www.storejetcloud.com/1";
    private String mCode = "";
    private String mStatus = "";
    private int TIMEOUT_IN_MILLIONS = 10000;

    public TutkBasicLoader(Context context) {
        super(context);
        mActivity = (Activity) context;
    }

    public TutkBasicLoader(Context context, String server) {
        super(context);
        mActivity = (Activity) context;
        mServer = server;
    }

    public String getServer() {
        return mServer;
    }

    public String getSystemLanguage() {
        String language = "en";
        Locale locale = Resources.getSystem().getConfiguration().locale;
        Log.d(TAG,"TEST " + locale);
        if("zh_TW".equals(locale.toString())) {
            language = "zh-tw";
        }
        return language;
    }

    public String getDeviceID() {
        String deviceID = android.os.Build.SERIAL;
        ;
        return deviceID;
    }

    public String doGetRequest(String url, String token) {
        HttpsURLConnection conn = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            trustAllHosts();
            conn = (HttpsURLConnection) realUrl.openConnection();
            conn.setHostnameVerifier(DO_NOT_VERIFY);
            conn.setRequestProperty("x-transcend-header", "application/transcend.v1");
            conn.setRequestProperty("authorization", "Bearer " + token);
            conn.setReadTimeout(TIMEOUT_IN_MILLIONS);
            conn.setConnectTimeout(TIMEOUT_IN_MILLIONS);


            int responseCode = conn.getResponseCode();// 调用此方法就不必再使用conn.connect()方法
            Log.i(TAG, "url " + url);
            Log.i(TAG, "responseCode: " + responseCode);
            if (responseCode == 200 || responseCode == 201) {
                InputStream is = conn.getInputStream();
                result = getStringFromInputStream(is);
            } else if (responseCode == 400 || responseCode == 401) {
                InputStream is = conn.getErrorStream();
                result = getStringFromInputStream(is);
            } else {
                Log.i(TAG, "访问失败 " + responseCode);
                //InputStream is = conn.getInputStream();
                //InputStream is = conn.getErrorStream();
                //Log.i(TAG, "访问失败 " + getStringFromInputStream(is));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 使用finally块来关闭输出流、输入流
        finally {
            if (conn != null) {
                conn.disconnect();// 关闭连接
            }
        }

        return result;
    }

    public String doDeleteRequest(String url, String token) {
        HttpsURLConnection conn = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            trustAllHosts();
            conn = (HttpsURLConnection) realUrl.openConnection();
            conn.setHostnameVerifier(DO_NOT_VERIFY);
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("x-transcend-header", "application/transcend.v1");
            conn.setRequestProperty("authorization", "Bearer " + token);
            conn.setReadTimeout(TIMEOUT_IN_MILLIONS);
            conn.setConnectTimeout(TIMEOUT_IN_MILLIONS);

            int responseCode = conn.getResponseCode();// 调用此方法就不必再使用conn.connect()方法
            Log.i(TAG, "url " + url);
            Log.i(TAG, "responseCode: " + responseCode);
            if (responseCode == 200 || responseCode == 201) {
                InputStream is = conn.getInputStream();
                result = getStringFromInputStream(is);
            } else if (responseCode == 400 || responseCode == 401) {
                InputStream is = conn.getErrorStream();
                result = getStringFromInputStream(is);
            } else {
                Log.i(TAG, "访问失败 " + responseCode);
                //InputStream is = conn.getInputStream();
                //InputStream is = conn.getErrorStream();
                //Log.i(TAG, "访问失败 " + getStringFromInputStream(is));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 使用finally块来关闭输出流、输入流
        finally {
            if (conn != null) {
                conn.disconnect();// 关闭连接
            }
        }

        return result;
    }

    public String doPostRequest(String url, String param) {
        return doPostRequest(url, param, null, false);
    }

    public String doJsonPostRequest(String url, String param) {
        return doPostRequest(url, param, null, true);
    }

    public String doPostRequest(String url, String param, String token, boolean isJson) {
        HttpsURLConnection conn = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            trustAllHosts();
            conn = (HttpsURLConnection) realUrl.openConnection();
            conn.setHostnameVerifier(DO_NOT_VERIFY);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("x-transcend-header", "application/transcend.v1");
            if(isJson)
                conn.setRequestProperty("content-type", "application/json");
            else
                conn.setRequestProperty("content-type", "application/x-www-form-urlencoded");
            if (token != null) {
                conn.setRequestProperty("authorization", "Bearer " + token);
            }
            conn.setDoOutput(true);
            conn.setReadTimeout(TIMEOUT_IN_MILLIONS);
            conn.setConnectTimeout(TIMEOUT_IN_MILLIONS);

            // 获得一个输出流,向服务器写数据
            OutputStream out = conn.getOutputStream();
            out.write(param.getBytes());
            out.flush();
            out.close();

            int responseCode = conn.getResponseCode();// 调用此方法就不必再使用conn.connect()方法
            Log.i(TAG, "url " + url);
            Log.i(TAG, "param: " + param);
            Log.i(TAG, "responseCode: " + responseCode);
            if (responseCode == 200 || responseCode == 201) {
                InputStream is = conn.getInputStream();
                result = getStringFromInputStream(is);
            } else if (responseCode == 400 || responseCode == 401) {
                InputStream is = conn.getErrorStream();
                result = getStringFromInputStream(is);
            } else {
                Log.i(TAG, "访问失败 " + responseCode);
                //InputStream is = conn.getInputStream();
                //InputStream is = conn.getErrorStream();
                //Log.i(TAG, "访问失败 " + getStringFromInputStream(is));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 使用finally块来关闭输出流、输入流
        finally {
            if (conn != null) {
                conn.disconnect();// 关闭连接
            }
        }

        return result;
    }

    public String doPostRequestOld(String url, String param) {

        try {
            HttpClient client = new DefaultHttpClient();
            URI website = new URI(url);

            //指定POST模式
            HttpPost request = new HttpPost();
            request.setHeader("x-transcend-header", "application/transcend.v1");
            request.setHeader("content-type", "application/x-www-form-urlencoded");

            List<NameValuePair> parmas = new ArrayList<NameValuePair>();
            parmas.add(new BasicNameValuePair("username", "ikelee"));
            parmas.add(new BasicNameValuePair("password", "123456"));
            parmas.add(new BasicNameValuePair("email", "ike_lee@transcend-info.com"));
            parmas.add(new BasicNameValuePair("language", "en"));

            UrlEncodedFormEntity env = new UrlEncodedFormEntity(parmas, HTTP.UTF_8);
            request.setURI(website);

            request.setEntity(env);

            HttpResponse response = client.execute(request);
            HttpEntity resEntity = response.getEntity();
            if (resEntity != null) {
                //myBundle.putString("response", EntityUtils.toString(resEntity));
                return EntityUtils.toString(resEntity);
            } else {
                return "";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    private String getStringFromInputStream(InputStream is)
            throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        // 模板代码 必须熟练
        byte[] buffer = new byte[1024];
        int len = -1;
        // 一定要写len=is.read(buffer)
        // 如果while((is.read(buffer))!=-1)则无法将数据写入buffer中
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
        is.close();
        String state = os.toString();// 把流中的数据转换成字符串,采用的编码是utf-8(模拟器默认编码)
        os.close();
        return state;
    }

    protected boolean doErrorParser(String result) {
        Log.i(TAG, result);
        //JSON FORMAT { "code": xxx, "status": xxx"}
        if (result.equals(""))
            return false;

        JSONObject obj;
        try {
            obj = new JSONObject(result);
            mCode = obj.optString("code");
            mStatus = obj.optString("status");
        } catch (JSONException e) {
            mCode = "";
            mStatus = "";
            //e.printStackTrace();
        }

        return true;
    }

    public static void trustAllHosts() {
        // Create a trust manager that does not validate certificate chains
        // Android use X509 cert
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[]{};
            }

            public void checkClientTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }
        }};

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    public String getCode() {
        return mCode;
    }

    public String getStatus() {
        return mStatus;
    }

    protected abstract boolean doParserResult(String result);

    protected abstract String doGenerateUrl();
}
