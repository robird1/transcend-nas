package com.transcend.nas.management.firmwareupdate;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by steve_su on 2017/6/23.
 */

class FirmwareDownloadLoader extends FirmwareLoader {
    private static final String TAG = FirmwareDownloadLoader.class.getSimpleName();
    private String mRemoteImagePath = "";
    private String mLocalImagePath = "";

    FirmwareDownloadLoader(Context context) {
        super(context);
    }

    @Override
    protected String onRequestBody() {
        return "hash="+ getHash();
    }

    @Override
    protected String onRequestUrl() {
        return "http://"+ getHost()+"/nas/firmware/upgrade";
    }

    @Override
    protected boolean doParse(String response) {
        Log.d(TAG, "[Enter] doParse response: "+ response);
        boolean isSuccess = super.doParse(response);
        if (!isSuccess) {
            return false;
        }

        mRemoteImagePath = parse(response, "<path>");
        mLocalImagePath = parse(response, "<file>");
        return isDataValid(mRemoteImagePath, mLocalImagePath);
    }

    @Override
    protected Bundle getData() {
        Bundle bundle = new Bundle();
        if (isDataValid(mRemoteImagePath, mLocalImagePath)) {
            bundle.putString("type", "download");
            bundle.putString("remote_path", mRemoteImagePath);
            bundle.putString("local_path", mLocalImagePath);
        }
        return bundle;
    }

}
