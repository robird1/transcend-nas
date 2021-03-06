package com.transcend.nas.tutk;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import com.transcend.nas.R;
import com.tutk.IOTC.P2PService;
import com.tutk.IOTC.P2PTunnelAPIs;


/**
 * Created by ikelee on 06/4/16.
 */
public class TutkLinkNasLoader extends TutkBasicLoader {

    private Bundle mArgs;
    private String mUUID;
    private String mError = "";
    private Context mContext;

    public TutkLinkNasLoader(Context context, Bundle args) {
        super(context);
        mContext = context;
        mArgs = args;
        mUUID = args.getString("hostname");
    }

    @Override
    public Boolean loadInBackground() {
        //check network status
        ConnectivityManager connectivityManager = (ConnectivityManager) mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            P2PService.getInstance().stopP2PConnect();
            int result = P2PService.getInstance().startP2PConnect(mUUID);
            if (result == P2PTunnelAPIs.TUNNEL_ER_INITIALIZED)
                mError = "Remote Access initial fail";
            else if (result == P2PTunnelAPIs.TUNNEL_ER_CONNECT)
                mError = "Sorry, we can't find the device";
            else if (result == P2PTunnelAPIs.TUNNEL_ER_UID_UNLICENSE)
                mError = "Sorry, this UID is illegal";
            else
                mError = mActivity.getString(R.string.network_error);;
            return result >= 0;
        }
        else {
            mError = mActivity.getString(R.string.network_error);
            return false;
        }
    }

    @Override
    protected boolean doParserResult(String result) {
        return true;
    }

    @Override
    protected String doGenerateUrl() {
        return null;
    }

    public String getNasUUID() {
        return mUUID;
    }

    public Bundle getBundleArgs() {
        return mArgs;
    }

    public String getError(){
        return mError;
    }

    public String getP2PHostname(){
        return P2PService.getInstance().getP2PIP() + ":" + P2PService.getInstance().getP2PPort(P2PService.P2PProtocalType.HTTP);
    }
}
