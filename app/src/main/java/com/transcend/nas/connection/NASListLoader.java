package com.transcend.nas.connection;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

/**
 * Created by silverhsu on 15/12/30.
 */
public class NASListLoader extends AsyncTaskLoader<Boolean> {

    private static final String TAG = NASListLoader.class.getSimpleName();

    /**
     * The fully qualified service name is build using up to 5 components with the following structure:
     *
     *            <app>.<protocol>.<servicedomain>.<parentdomain>.
     * <Instance>.<app>.<protocol>.<servicedomain>.<parentdomain>.
     * <sub>._sub.<app>.<protocol>.<servicedomain>.<parentdomain>.
     *
     * 1. <servicedomain>.<parentdomain>: This is the domain scope of the service typically "local.", but this can also be something similar to "in-addr.arpa." or "ip6.arpa."
     * 2. <protocol>: This is either "_tcp" or "_udp"
     * 3. <app>: This define the application protocol. Typical example are "_http", "_ftp", etc.
     * 4. <Instance>: This is the service name
     * 5. <sub>: This is the subtype for the application protocol
     */
    private static final String TYPE = "_http._tcp.local.";

    private static final String SERVER = "RealtekNAS";
    private static final String KEY_VALUE_PAIR = "path=/rtknas/index.html";

    private WifiManager mWifiMgr;
    private WifiManager.MulticastLock mLock;

    private JmDNS mJmDNS;
    private ArrayList<HashMap<String, String>> mNASList;
    private int mRetry = 1;
    private Context mContext;

    public NASListLoader(Context context) {
        super(context);
        mWifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mLock = mWifiMgr.createMulticastLock(TAG);
        mNASList = new ArrayList<HashMap<String, String>>();
    }

    public NASListLoader(Context context, int retry) {
        this(context);
        mContext = context;
        mRetry = retry;
    }

    @Override
    public Boolean loadInBackground() {
        Log.w(TAG, "loadInBackground");
        ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
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
                    return mNASList.size() > 0;
            }
        }

        return false;
    }

    @Override
    protected void onStopLoading() {
        Log.w(TAG, "onStopLoading");
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
        byte[] ipByteArray = new byte[] {
                (byte)ipAddress,
                (byte)(ipAddress >>> 8),
                (byte)(ipAddress >>> 16),
                (byte)(ipAddress >>> 24),
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
            Log.w(TAG, "Server Scan" );

            for (ServiceInfo info : serviceInfos) {
                boolean isMyNAS = false;

                // Method 1 : Compare the text for the service
                byte[] textBytes = info.getTextBytes();
                for (int idx = 0; idx < textBytes.length; idx++) {
                    int length = (int)textBytes[idx];
                    int start = idx + 1;
                    String keyValuePair = new String(textBytes, start, length);
                    if(keyValuePair.equals(KEY_VALUE_PAIR))
                    {
                        isMyNAS = true;
                        break;
                    }
                    idx += length;
                }

                // Method 2 : Compare the name of the server.
                String server = info.getServer();
                Log.w(TAG, "Server: " + server);
                if (server.contains(SERVER)) {
                    isMyNAS = true;
                }

                if (isMyNAS) {
                    HashMap<String, String> nas = new HashMap<String, String>();
                    if(info.getInet4Addresses().length == 0)
                        continue;

                    String name = info.getServer();
                    String end = ".local.";
                    if(name.endsWith(end)){
                        name = name.substring(0, name.length() - end.length());
                    }

                    nas.put("nasId", "-1");
                    nas.put("nickname", name);
                    nas.put("hostname", info.getInet4Addresses()[0].getHostAddress());
                    Log.w(TAG, "nickname: " + nas.get("nickname"));
                    Log.w(TAG, "hostname: " + nas.get("hostname"));
                    if (mNASList.contains(nas))
                        continue;
                    mNASList.add(nas);
                }
            }

            retry--;
            if((mNASList.size() == 0) && (retry > 0)){
                Log.w(TAG, "Server Scan empty, retry times : " + retry );
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

    public ArrayList<HashMap<String, String>> getList() {
        return mNASList;
    }
}
