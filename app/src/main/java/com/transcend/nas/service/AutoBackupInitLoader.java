package com.transcend.nas.service;

import android.app.Activity;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;

import com.transcend.nas.service.AutoBackupService;
import com.tutk.IOTC.P2PService;


/**
 * Created by ikelee on 06/4/16.
 */
public class AutoBackupInitLoader extends AsyncTaskLoader<Boolean> {
    public Activity mActivity;

    public AutoBackupInitLoader(Context context) {
        super(context);
        mActivity = (Activity) context;
    }

    @Override
    public Boolean loadInBackground() {
        Intent intent = new Intent(mActivity, AutoBackupService.class);
        mActivity.startService(intent);
        return true;
    }
}
