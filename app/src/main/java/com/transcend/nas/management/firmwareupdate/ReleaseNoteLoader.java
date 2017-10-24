package com.transcend.nas.management.firmwareupdate;

import android.content.Context;
import android.os.Bundle;

/**
 * Created by steve_su on 2017/10/18.
 */

public class ReleaseNoteLoader extends FirmwareLoader {
    private String mNote;
    private String mRemoteVersion;

    ReleaseNoteLoader(Context context) {
        super(context);
    }

    @Override
    protected String onRequestBody() {
        return "hash="+ getHash();
    }

    @Override
    protected String onRequestUrl() {
        return "http://"+ getHost()+"/nas/firmware/getversion";
    }

    @Override
    protected boolean doParse(String response) {
        boolean isSuccess = super.doParse(response);
        if (!isSuccess) {
            return false;
        }

        mNote = parse(response, "<remote_note>");
        mRemoteVersion = parse(response, "<remote_ver>");
        return isDataValid(mNote, mRemoteVersion);
    }

    @Override
    protected Bundle getData() {
        Bundle bundle = new Bundle();
        if (isDataValid(mNote, mRemoteVersion)) {
            bundle.putString("note", mNote);
            bundle.putString("remote_version", mRemoteVersion);
        }
        return bundle;
    }

}
