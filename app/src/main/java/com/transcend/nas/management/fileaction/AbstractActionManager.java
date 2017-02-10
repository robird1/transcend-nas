package com.transcend.nas.management.fileaction;

import android.content.Loader;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.widget.RelativeLayout;

/**
 * Created by ike_lee on 2016/12/21.
 */
public abstract class AbstractActionManager {
    RelativeLayout mProgressLayout;
    public void setProgressLayout(RelativeLayout progressLayout){
        mProgressLayout = progressLayout;
    }

    abstract Loader<Boolean> onCreateLoader(int id, Bundle args);
    abstract boolean onLoadFinished(Loader<Boolean> loader, Boolean success);
    abstract void onLoaderReset(Loader<Boolean> loader);
}
