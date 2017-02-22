package com.transcend.nas.management;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.net.MalformedURLException;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * Created by silverhsu on 16/2/1.
 */
public class SmbFileDeleteLoader extends SmbAbstractLoader {

    private static final String TAG = SmbFileDeleteLoader.class.getSimpleName();

    private List<String> mPaths;
    private boolean mIsDeleteAfterUpload = false;

    public SmbFileDeleteLoader(Context context, List<String> paths) {
        super(context);
        mPaths = paths;
    }

    public SmbFileDeleteLoader(Context context, List<String> paths, boolean isDeleteAfterUpload) {
        this(context, paths);
        mIsDeleteAfterUpload = isDeleteAfterUpload;
    }

    @Override
    public Boolean loadInBackground() {
        try {
            super.loadInBackground();
            return delete();
        } catch (Exception e) {
            e.printStackTrace();
            setException(e);
        }
        return false;
    }

    private boolean delete() throws MalformedURLException, SmbException {
        for (String path : mPaths) {
            if(isLoadInBackgroundCanceled()) {
                Log.d(TAG, "Delete cancel");
                break;
            }

            SmbFile target = new SmbFile(getSmbUrl(path));
            target.delete();
        }
        return true;
    }

    public boolean isDeleteAfterUpload()
    {
        return mIsDeleteAfterUpload;
    }

}
