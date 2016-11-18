package com.transcend.nas.connection;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.Bundle;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerInfo;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.management.firmware.ShareFolderManager;

/**
 * Created by silverhsu on 16/1/5.
 */
public class LoginLoader extends AsyncTaskLoader<Boolean> {

    private Server mServer;
    private String mError;
    private Bundle mArgs;
    private boolean mReplace = true;
    private Context mContext;
    private LoginHelper mLoginHelper;

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
        boolean success = mServer.connect(true);
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
        }
        return success;
    }

    private void updateServerManager() {
        ServerManager.INSTANCE.saveServer(mServer);
        ServerManager.INSTANCE.setCurrentServer(mServer);
        NASPref.setSessionVerifiedTime(getContext(), Long.toString(System.currentTimeMillis()));
    }

    private void updateLoginPreference() {
        NASPref.setHostname(getContext(), mServer.getHostname());
        NASPref.setUsername(getContext(), mServer.getUsername());
        NASPref.setPassword(getContext(), mServer.getPassword());
        NASPref.setUUID(getContext(), mServer.getTutkUUID());
        NASPref.setCloudUUID(getContext(), mServer.getTutkUUID());
        NASPref.setMacAddress(getContext(), mServer.getServerInfo().mac);
        NASPref.setLocalHostname(getContext(), mServer.getServerInfo().ipAddress);

        ShareFolderManager.getInstance().cleanRealPathMap();

        if(NASPref.useNewLoginFlow){
            mLoginHelper = new LoginHelper(mContext);
            LoginHelper.LoginInfo account = new LoginHelper.LoginInfo();
            account.email = NASPref.getCloudUsername(mContext);
            account.hostname = mServer.getHostname();
            account.username = mServer.getUsername();
            account.password = mServer.getPassword();
            account.uuid = mServer.getTutkUUID();
            ServerInfo info = mServer.getServerInfo();
            account.macAddress = info.mac;
            account.ip = info.ipAddress;
            mLoginHelper.setAccount(account);
            mLoginHelper.onDestroy();
        }
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
