package com.transcend.nas.management;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.SyncAdapterType;
import android.util.Log;

import com.realtek.nasfun.api.SambaStatus;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.R;
import com.tutk.IOTC.P2PService;

/**
 * Created by silverhsu on 16/1/20.
 */
public abstract class SmbAbstractLoader extends AsyncTaskLoader<Boolean> {

    private Server mServer;
    private String mUsername;
    private String mPassword;
    private String mHostname;
    private Exception mException;

    public SmbAbstractLoader(Context context) {
        super(context);
        //System.setProperty("jcifs.smb.client.dfs.disabled", "true");
        System.setProperty("jcifs.smb.client.soTimeout", "5000");
        System.setProperty("jcifs.smb.client.responseTimeout", "5000");
        mServer = ServerManager.INSTANCE.getCurrentServer();
        mHostname = mServer.getHostname();
        mUsername = mServer.getUsername();
        mPassword = mServer.getPassword();
        String p2pIP = P2PService.getInstance().getP2PIP();
        if (mHostname.contains(p2pIP))
            mHostname = p2pIP + ":" + P2PService.getInstance().getP2PPort(P2PService.P2PProtocalType.SMB);
    }

    @Override
    public Boolean loadInBackground() {
        // Restructure Remote Access
        //String p2pIP = P2PService.getInstance().getP2PIP();
        //if (mHostname.contains(p2pIP))
        //    P2PService.getInstance().reStartP2PConnect();
        return true;
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

    protected void setException(Exception e){
        mException = e;
    }

    public String getExceptionMessage(){
        String message = getContext().getString(R.string.network_error);
        if(mException != null) {
            if (mException instanceof jcifs.smb.SmbAuthException) {
                message = getContext().getString(R.string.access_error);
            } else if (mException instanceof jcifs.smb.SmbException) {
                message = getContext().getString(R.string.network_error);
            }
        }
        return message;
    }

}
