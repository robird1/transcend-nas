package com.transcend.nas.management;

import android.content.Context;

import java.net.MalformedURLException;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * Created by silverhsu on 16/1/20.
 */
public class SmbFolderCreateLoader extends SmbAbstractLoader {
    private String mPath;

    public SmbFolderCreateLoader(Context context, String path) {
        super(context);
        mPath = path;
    }

    @Override
    public Boolean loadInBackground() {
        try {
            super.loadInBackground();
            return createNewFolder();
        } catch (Exception e) {
            e.printStackTrace();
            setException(e);
        }
        return false;
    }

    private boolean createNewFolder() throws MalformedURLException, SmbException {
        String url = super.getSmbUrl(mPath);
        SmbFile target = new SmbFile(url);
        if (!target.exists()) {
            target.mkdirs();
            return true;
        }
        return false;
    }
}
