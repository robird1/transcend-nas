package com.transcend.nas.connection;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASPref;
import com.transcend.nas.utils.PrefUtil;

/**
 * Created by silverhsu on 15/12/30.
 */
public class AutoLinkLoader extends AsyncTaskLoader<Boolean> {

    private static final String TAG = AutoLinkLoader.class.getSimpleName();

    private ConnectivityManager mConnMgr;
    private Server mServer;

    public AutoLinkLoader(Context context) {
        super(context);
        mConnMgr = (ConnectivityManager)getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public Boolean loadInBackground() {
        if (checkNetworkAvailable()) {
            if (signInThroughInternet())
                return true;
            if (loginThroughIntranet())
                return true;
        }
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

    private boolean signInThroughInternet() {
        // record sign in preference if success
        return false;
    }

    private boolean loginThroughIntranet() {
        String hostname = NASPref.getLoginHostname(getContext());
        String username = NASPref.getLoginUsername(getContext());
        String password = NASPref.getLoginPassword(getContext());
        Log.w(TAG, "hostname: " + hostname);
        Log.w(TAG, "username: " + username);
        Log.w(TAG, "password: " + password);
        if (hostname.isEmpty() || username.isEmpty() || password.isEmpty())
            return false;
        mServer = new Server(hostname, username, password);
        boolean isConnected = mServer.connect();
        if (isConnected) updateServerManager();
        return isConnected;
    }

    private void updateServerManager() {
        ServerManager.INSTANCE.saveServer(mServer);
        ServerManager.INSTANCE.setCurrentServer(mServer);
    }

}
