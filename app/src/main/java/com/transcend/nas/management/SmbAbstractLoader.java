package com.transcend.nas.management;

import android.content.AsyncTaskLoader;
import android.content.Context;

import com.realtek.nasfun.api.SambaStatus;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;

/**
 * Created by silverhsu on 16/1/20.
 */
public abstract class SmbAbstractLoader extends AsyncTaskLoader<Boolean> {

    private Server mServer;
    private String mUsername;
    private String mPassword;
    private String mHostname;

    public SmbAbstractLoader(Context context) {
        super(context);
        //System.setProperty("jcifs.smb.client.dfs.disabled", "true");
        mServer = ServerManager.INSTANCE.getCurrentServer();
        mUsername = mServer.getUsername();
        mPassword = mServer.getPassword();
        mHostname = mServer.getHostname();
        // TODO: P2P case
        //if (P2PService.getInstance().isConnected())
        //    mHostname = P2PService.getInstance().P2PGetSmbIP();
    }

    protected boolean checkSambaService() {
        SambaStatus status = mServer.getServiceStatus(Server.Service.SAMBA);
        return status.isRunning;
    }

    protected boolean isValid(String str) {
        return (str != null) && (!str.isEmpty());
    }

    protected String getSmbUrl(String path) {
        StringBuilder builder = new StringBuilder();
        builder.append("smb://");
        if (isValid(mUsername) && isValid(mPassword)) {
            builder.append(mUsername);
            builder.append(":");
            builder.append(mPassword);
            builder.append("@");
        }
        builder.append(mHostname);
        if (isValid(path))
            builder.append(path);
        return builder.toString();
    }

    protected String format(String path) {
        StringBuilder builder = new StringBuilder();
        if (!path.startsWith("/"))
            builder.append("/");
        builder.append(path);
        if (!path.endsWith("/"))
            builder.append("/");
        return builder.toString();
    }

}
