package com.transcend.nas.management;

import android.content.Context;

import java.net.MalformedURLException;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * Created by ikelee on 17/3/29.
 */
public class RecentCheckLoader extends SmbAbstractLoader {

    private static final String TAG = RecentCheckLoader.class.getSimpleName();

    private String mPath;
    private boolean mExist = false;

    public RecentCheckLoader(Context context, String path) {
        super(context);
        mPath = format(path);
    }

    @Override
    public Boolean loadInBackground() {
        try {
            super.loadInBackground();
            mExist = existFile();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            setException(e);
        }
        return false;
    }

    private boolean existFile () throws MalformedURLException, SmbException {
        String url = super.getSmbUrl(mPath);
        SmbFile target = new SmbFile(url);
        return target.exists();
    }

    public boolean isExistFile(){
        return mExist;
    }
}
