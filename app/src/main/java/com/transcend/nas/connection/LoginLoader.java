package com.transcend.nas.connection;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.Bundle;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASPref;
import com.tutk.IOTC.P2PService;

/**
 * Created by silverhsu on 16/1/5.
 */
public class LoginLoader extends AsyncTaskLoader<Boolean> {

    private Server mServer;

    public LoginLoader(Context context, Bundle args) {
        super(context);
        String hostname = args.getString("hostname");
        String username = args.getString("username");
        String password = args.getString("password");
        mServer = new Server(hostname, username, password);
    }

    @Override
    public Boolean loadInBackground() {
        boolean success = mServer.connect();
        if (success) {
            updateServerManager();
            updateLoginPreference();
        }
        return success;
    }

    private void updateServerManager() {
        ServerManager.INSTANCE.saveServer(mServer);
        ServerManager.INSTANCE.setCurrentServer(mServer);
    }

    private void updateLoginPreference() {
        String p2pIp = P2PService.getInstance().getP2PIP();
        if(mServer.getHostname().contains(p2pIp))
            NASPref.setCloudUUID(getContext(), P2PService.getInstance().getTUTKUUID());
        else
            NASPref.setLocalHostname(getContext(), mServer.getHostname());

        NASPref.setHostname(getContext(), mServer.getHostname());
        NASPref.setUsername(getContext(), mServer.getUsername());
        NASPref.setPassword(getContext(), mServer.getPassword());
    }
}
