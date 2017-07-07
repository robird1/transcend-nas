package com.transcend.nas.management.firmwareupdate;

import android.content.Context;
import android.os.Bundle;

/**
 * Created by steve_su on 2017/6/27.
 */

class FirmwareUpgradeLoader extends FirmwareLoader {
    private String mLocalImagePath = "";
    private String mUpgradePath = "";

    public FirmwareUpgradeLoader(Context context, Bundle args) {
        super(context);

        if (args == null)
            return;
        mLocalImagePath = args.getString("local_path");
    }

    @Override
    protected String onRequestBody() {
        return "filepath="+ getEncodedPath()+ "&hash="+ getHash();
    }

    @Override
    protected String onRequestUrl() {
        return "http://"+ getHost()+"/nas/firmware/localupgrade";
    }

    private String getEncodedPath() {
        return mLocalImagePath.replace("/", "%2F");
    }

    @Override
    protected boolean doParse(String response) {
        boolean isSuccess = super.doParse(response);
        if (!isSuccess) {
            return false;
        }

        mUpgradePath = parse(response, "<path>");
        return isDataValid(mUpgradePath);
    }

    @Override
    protected Bundle getData() {
        Bundle bundle = new Bundle();
        if (isDataValid(mUpgradePath)) {
            bundle.putString("type", "upgrade");
            bundle.putString("upgrade_path", mUpgradePath);
        }
        return bundle;
    }

}
