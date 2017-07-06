package com.transcend.nas.management.firmwareupdate;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by steve_su on 2017/6/26.
 */

class FirmwareStatusLoader extends FirmwareLoader {
    private static final String TAG = FirmwareStatusLoader.class.getSimpleName();
    private Bundle mData;
    private String mReturnCode;
    private String mPercentage;

    FirmwareStatusLoader(Context context, Bundle args) {
        super(context);

        if (args == null)
            return;
        mData = args;
    }

    @Override
    protected String onRequestBody() {
        return "path="+ getEncodedPath()+ "&hash="+ getHash();
    }

    @Override
    protected String onRequestUrl() {
        return "http://"+ getHost()+"/nas/firmware/status";
    }

    private String getEncodedPath() {
        String path = mData.getString("remote_path");
        if (isDataValid(path))
            return path.replace("/", "%2F");
        else
            return "";
    }

    @Override
    protected boolean doParse(String response) {
        Log.d(TAG, "[Enter] doParse response: "+ response);
        boolean isSuccess = super.doParse(response);
        if (!isSuccess) {
            return false;
        }

        mReturnCode = parse(response, "<retcode>");
        switch(mReturnCode) {
            case "":
                mPercentage = parse(response, "<percentage>");
                break;
            case "0":
                mPercentage = "100";
                break;
            case "1":
            default:
                return false;
        }

        return true;
    }

    String getReturnCode() {
        return mReturnCode;
    }

    String getPercentage() {
        return mPercentage;
    }

    @Override
    protected Bundle getData() {
        return mData;
    }

}
