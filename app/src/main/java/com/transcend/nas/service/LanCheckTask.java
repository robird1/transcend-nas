package com.transcend.nas.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import com.realtek.nasfun.api.HttpClientManager;
import com.transcend.nas.NASApp;
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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

/**
 * Created by ike_lee on 2016/3/23.
 */
public class LanCheckTask extends AsyncTask<String, String, Boolean> {
    private static final String TAG = "LanCheckTask";

    private static final String TYPE = "_http._tcp.local.";
    private static final String SERVER = "RealtekNAS";
    private static final String KEY_VALUE_PAIR = "path=/rtknas/index.html";
    private Context mContext;
    private WifiManager mWifiMgr;
    private WifiManager.MulticastLock mLock;
    private JmDNS mJmDNS;
    private ArrayList<HashMap<String, String>> mNASList;
    private int mRetry = 1;
    private String mTargetIp = "";

    private LanCheckCallback mListener;

    public LanCheckTask() {
        mContext = NASApp.getContext();
        mWifiMgr = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mLock = mWifiMgr.createMulticastLock(TAG);
        mNASList = new ArrayList<HashMap<String, String>>();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(String... params) {
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            int type = info.getType();
            switch (type) {
                case ConnectivityManager.TYPE_WIFI:
                    createJmDNS();
                    loadNASList();
                    if(mLock != null)
                        mLock.release();
                    //closeJmDNS();
                    return tryLink();
            }
        }

        return false;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if (mListener != null) {
            mListener.onLanCheckFinished(result, mTargetIp);
        }
    }

    public void addListener(LanCheckCallback listener) {
        mListener = listener;
    }

    private void createJmDNS() {
        // Create an instance of JmDNS and bind it to a specific network interface given its IP-address.
        try {
            InetAddress address = getIPv4Address();
            mLock.setReferenceCounted(true);
            mLock.acquire();
            mJmDNS = JmDNS.create(address);
            Log.w(TAG, "JmDNS create");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private InetAddress getIPv4Address() throws UnknownHostException {
        int ipAddress = mWifiMgr.getConnectionInfo().getIpAddress();
        byte[] ipByteArray = new byte[]{
                (byte) ipAddress,
                (byte) (ipAddress >>> 8),
                (byte) (ipAddress >>> 16),
                (byte) (ipAddress >>> 24),
        };
        return InetAddress.getByAddress(ipByteArray);
    }

    private void loadNASList() {
        // Returns a list of service infos of the specified type.
        int retry = mRetry;

        while ((mNASList.size() == 0) && (retry > 0)) {
            if (mJmDNS == null)
                return;

            ServiceInfo[] serviceInfos = mJmDNS.list(TYPE);
            Log.w(TAG, "Server Scan");

            for (ServiceInfo info : serviceInfos) {
                boolean isMyNAS = false;

                // Method 1 : Compare the text for the service
                byte[] textBytes = info.getTextBytes();
                for (int idx = 0; idx < textBytes.length; idx++) {
                    int length = (int) textBytes[idx];
                    int start = idx + 1;
                    String keyValuePair = new String(textBytes, start, length);
                    if (keyValuePair.equals(KEY_VALUE_PAIR)) {
                        isMyNAS = true;
                        break;
                    }
                    idx += length;
                }

                // Method 2 : Compare the name of the server.
                String server = info.getServer();
                if (server.contains(SERVER)) {
                    isMyNAS = true;
                }

                if (isMyNAS) {
                    HashMap<String, String> nas = new HashMap<String, String>();
                    if (info.getInet4Addresses().length == 0)
                        continue;

                    String name = info.getServer();
                    String end = ".local.";
                    if (name.endsWith(end)) {
                        name = name.substring(0, name.length() - end.length());
                    }

                    nas.put("nasId", "-1");
                    nas.put("nickname", name);
                    nas.put("hostname", info.getInet4Addresses()[0].getHostAddress());
                    if (mNASList.contains(nas))
                        continue;
                    mNASList.add(nas);
                }
            }

            retry--;
            if ((mNASList.size() == 0) && (retry > 0)) {
                Log.w(TAG, "Server Scan empty, retry times : " + retry);
                createJmDNS();
            }
        }


    }

    private void closeJmDNS() {
        try {
            if (mJmDNS != null) {
                mJmDNS.close();
                mJmDNS = null;
            }
            if (mLock != null) {
                mLock.release();
                mLock = null;
            }
            Log.w(TAG, "JmDNS close");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean tryLink() {
        boolean success = false;
        String macAddress = NASPref.getMacAddress(mContext);
        for (HashMap<String, String> nas : mNASList) {
            String mUrl = "http://" + nas.get("hostname") + "/nas/get/register";
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
                    String hwAddr = "", ipAddr = "";

                    do {
                        String tagName = xpp.getName();
                        if (eventType == XmlPullParser.START_TAG) {
                            curTagName = tagName;
                        } else if (eventType == XmlPullParser.TEXT) {
                            if (curTagName != null) {
                                text = xpp.getText();
                                if (curTagName.equals("hwaddr")) {
                                    hwAddr = text;
                                } else if (curTagName.equals("ipaddr")) {
                                    ipAddr = text;
                                }
                            }
                        }

                        eventType = xpp.next();

                    } while (eventType != XmlPullParser.END_DOCUMENT);
                    if(hwAddr != null && hwAddr.equals(macAddress)) {
                        mTargetIp = ipAddr;
                        success = true;
                        Log.w(TAG, "Current IP : " + ipAddr);
                    }
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
        }

        return success;
    }

    public interface LanCheckCallback {
        public void onLanCheckFinished(boolean success, String ip);
    }
}
