package com.transcend.nas.connection;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.Bundle;

import com.realtek.nasfun.api.Server;
import com.transcend.nas.R;
import com.transcend.nas.management.firmware.FirmwareHelper;

/**
 * Created by silverhsu on 16/1/5.
 */
public class LoginLoader extends AsyncTaskLoader<Boolean> {

    private String mError;
    private Bundle mArgs;

    public LoginLoader(Context context, Bundle args) {
        super(context);
        mArgs = args;
    }

    @Override
    public Boolean loadInBackground() {
        String hostname = mArgs.getString("hostname");
        String username = mArgs.getString("username");
        String password = mArgs.getString("password");
        Server server = new Server(hostname, username, password);

        FirmwareHelper helper = new FirmwareHelper();
        boolean success = helper.doLogin(getContext(), server, true);
        if (!success)
            mError = helper.getResult();
        return success;
    }

    public String getLoginError() {
        if (mError != null && !mError.equals(""))
            return mError;
        else
            return getContext().getString(R.string.network_error);
    }

    public Bundle getBundleArgs() {
        return mArgs;
    }
}
