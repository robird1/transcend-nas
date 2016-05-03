package com.transcend.nas.connection;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.realtek.nasfun.api.HttpClientManager;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.tutk.IOTC.P2PService;
import com.tutk.IOTC.P2PTunnelAPIs;

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
 * Created by silverhsu on 15/12/30.
 */
public class AutoLinkLoader extends AsyncTaskLoader<Boolean> {

    private static final String TAG = AutoLinkLoader.class.getSimpleName();

    private ConnectivityManager mConnMgr;
    private Server mServer;
    private String mUrl = null;
    private boolean isWizard = false;
    private LinkType mLinkType = LinkType.NO_LINK;

    public enum LinkType {
        NO_LINK, INTRANET, INTERNET
    }

    public AutoLinkLoader(Context context) {
        super(context);
        mConnMgr = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public Boolean loadInBackground() {
        if (checkNetworkAvailable()) {
            if(doWizardCheck(true)){
                Log.d(TAG, "Intranet Wizard : " + isWizard);
                if(isWizard){
                    if (loginThroughIntranet()) {
                        mLinkType = LinkType.INTRANET;
                        return true;
                    }
                }
                else{
                    mLinkType = LinkType.NO_LINK;
                    return true;
                }
            }

            if(doWizardCheck(false)) {
                Log.d(TAG, "Internet Wizard : " + isWizard);
                if(isWizard){
                    if (signInThroughInternet()) {
                        mLinkType = LinkType.INTERNET;
                        return true;
                    }
                }
                else{
                    mLinkType = LinkType.NO_LINK;
                    return true;
                }
            }
        }

        mLinkType = LinkType.NO_LINK;
        return false;
    }

    private boolean checkNetworkAvailable() {
        NetworkInfo info = mConnMgr.getActiveNetworkInfo();
        if (info == null)
            return false;
        boolean isWiFi = (info.getType() == ConnectivityManager.TYPE_WIFI);
        boolean isMobile = (info.getType() == ConnectivityManager.TYPE_MOBILE);
        Log.w(TAG, "Wi-Fi: " + isWiFi);
        Log.w(TAG, "Mobile: " + isMobile);
        return (isWiFi || isMobile);
    }

    private boolean doWizardCheck(boolean isIntranet) {
        isWizard = false;
        boolean success = false;
        if (isIntranet) {
            String local = NASPref.getLocalHostname(getContext());
            if (local != null && !local.equals(""))
                mUrl = "http://" + local + "/nas/get/register";
        } else {
            String uuid = NASPref.getCloudUUID(getContext());
            if (!uuid.equals("")) {
                P2PService.getInstance().stopP2PConnect();
                int result = P2PService.getInstance().startP2PConnect(uuid);
                if (result >= 0) {
                    mUrl = "http://" + P2PService.getInstance().getP2PIP() + ":" + P2PService.getInstance().getP2PPort(P2PService.P2PProtocalType.HTTP) + "/nas/get/register";
                } else {
                    P2PService.getInstance().stopP2PConnect();
                }
            }
        }

        if (mUrl != null) {
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
                                        break;
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
        }

        Log.d(TAG, "isWizard: " + isWizard);
        return success;
    }

    private boolean signInThroughInternet() {
        Log.d(TAG, "Internet: start");
        String hostname = NASPref.getHostname(getContext());
        String username = NASPref.getUsername(getContext());
        String password = NASPref.getPassword(getContext());
        if (hostname.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Log.d(TAG, "Internet fail, due to : " + hostname + ", " + username + "," + password);
            return false;
        }

        String uuid = NASPref.getCloudUUID(getContext());
        if (!uuid.equals("")) {
            P2PService.getInstance().stopP2PConnect();
            int result = P2PService.getInstance().startP2PConnect(uuid);
            if (result >= 0) {
                hostname = P2PService.getInstance().getP2PIP() + ":" + P2PService.getInstance().getP2PPort(P2PService.P2PProtocalType.HTTP);
                mServer = new Server(hostname, username, password);
                boolean isConnected = mServer.connect();
                if (isConnected) {
                    updateServerManager();
                    NASPref.setHostname(getContext(), hostname);
                } else {
                    P2PService.getInstance().stopP2PConnect();
                }
                Log.d(TAG, "Internet: " + isConnected);
                Log.d(TAG, "Internet ip: " + hostname);
                Log.d(TAG, "Internet username: " + username);
                Log.d(TAG, "Internet password: " + password);
                return isConnected;
            } else {
                P2PService.getInstance().stopP2PConnect();
            }
        }
        Log.d(TAG, "Internet fail, due to : empty uuid");
        return false;
    }

    private boolean loginThroughIntranet() {
        Log.d(TAG, "Intranet: start");
        String hostname = NASPref.getLocalHostname(getContext());
        String username = NASPref.getUsername(getContext());
        String password = NASPref.getPassword(getContext());
        if (hostname.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Log.d(TAG, "Intranet fail, due to : " + hostname + ", " + username + "," + password);
            return false;
        }
        mServer = new Server(hostname, username, password);
        boolean isConnected = mServer.connect();
        if (isConnected) {
            updateServerManager();
            NASPref.setHostname(getContext(), hostname);
        }
        Log.d(TAG, "Intranet: " + isConnected);
        Log.d(TAG, "Intranet ip: " + hostname);
        Log.d(TAG, "Intranet username: " + username);
        Log.d(TAG, "Intranet password: " + password);
        return isConnected;
    }

    private void updateServerManager() {
        ServerManager.INSTANCE.saveServer(mServer);
        ServerManager.INSTANCE.setCurrentServer(mServer);
    }

    public LinkType getLinkType() {
        return mLinkType;
    }

    public boolean isWizard() {
        return isWizard;
    }

}
