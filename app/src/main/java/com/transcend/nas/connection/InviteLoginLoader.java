package com.transcend.nas.connection;

import android.content.Context;
import android.os.Bundle;

/**
 * Created by steve_su on 2017/4/13.
 */

public class InviteLoginLoader extends LoginLoader {
    private String mUserName;
    private String mPassword;

    public InviteLoginLoader(Context context, Bundle args) {
        super(context, args, false);
        mUserName = (String) args.get("username");
        mPassword = (String) args.get("password");
    }

    @Override
    public Boolean loadInBackground() {
        return mServer.connect(true);
    }

    public String getUserName() {
        return mUserName;
    }

    public String getPassword() {
        return mPassword;
    }

}
