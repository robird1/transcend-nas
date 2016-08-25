package com.transcend.nas.connection;

import android.app.Activity;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.Bundle;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.common.AnalysisFactory;
import com.transcend.nas.common.FileFactory;
import com.tutk.IOTC.P2PService;

/**
 * Created by silverhsu on 16/1/5.
 */
public class LoginLoader extends AsyncTaskLoader<Boolean> {

    private Server mServer;
    private String mError;
    private Bundle mArgs;
    private boolean mReplace = true;
    private Context mContext;

    public LoginLoader(Context context, Bundle args, boolean replaceServer) {
        super(context);
        mContext = context;
        mArgs = args;
        mReplace = replaceServer;
        String hostname = args.getString("hostname");
        String username = args.getString("username");
        String password = args.getString("password");
        mServer = new Server(hostname, username, password);
    }

    @Override
    public Boolean loadInBackground() {
        boolean success = mServer.connect();
        if (success) {
            if(mReplace) {
                updateServerManager();
                updateLoginPreference();
            }
            else{
                String uuid = mServer.getTutkUUID();
                if(uuid != null && !uuid.equals("")) {
                    mArgs.putString("nasUUID", uuid);
                    NASPref.setUUID(getContext(), uuid);
                }
                else
                    success = false;
            }
        }
        else{
            mError = mServer.getLoginError();
            AnalysisFactory.getInstance(mContext).sendConnectEvent(AnalysisFactory.ACTION.LOGINREMOTE, mError);
        }
        return success;
    }

    private void updateServerManager() {
        ServerManager.INSTANCE.saveServer(mServer);
        ServerManager.INSTANCE.setCurrentServer(mServer);
        NASPref.setSessionVerifiedTime(getContext(), Long.toString(System.currentTimeMillis()));
    }

    private void updateLoginPreference() {
        String p2pIp = P2PService.getInstance().getP2PIP();
        String hostname = mServer.getHostname();
        if(hostname.contains(p2pIp)) {
            AnalysisFactory.getInstance(mContext).sendConnectEvent(AnalysisFactory.ACTION.LOGINREMOTE, true);
            NASPref.setCloudUUID(getContext(), P2PService.getInstance().getTUTKUUID());
            NASPref.setCloudMode(getContext(), true);
        }
        else {
            AnalysisFactory.getInstance(mContext).sendConnectEvent(AnalysisFactory.ACTION.LOGINLOCAL, true);
            NASPref.setLocalHostname(getContext(), hostname);
            NASPref.setCloudMode(getContext(), false);
        }

        NASPref.setHostname(getContext(), hostname);
        NASPref.setUsername(getContext(), mServer.getUsername());
        NASPref.setPassword(getContext(), mServer.getPassword());
        NASPref.setUUID(getContext(), mServer.getTutkUUID());
        NASPref.setMacAddress(getContext(), mServer.getServerInfo().mac);

        FileFactory.getInstance().cleanRealPathMap();
    }

    public String getLoginError(){
        if(mError != null && !mError.equals(""))
            return mError;
        else
            return getContext().getString(R.string.network_error);
    }

    public Bundle getBundleArgs(){
        return mArgs;
    }
}
