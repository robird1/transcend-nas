package com.transcend.nas.management;

import android.content.Context;

import com.transcend.nas.R;
import com.transcend.nas.common.CustomNotificationManager;

import java.net.MalformedURLException;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * Created by silverhsu on 16/2/4.
 */
public class SmbFileCopyLoader extends SmbAbstractLoader {

    private List<String> mSrcs;
    private String mDest;

    public SmbFileCopyLoader(Context context, List<String> srcs, String dest) {
        super(context);
        mSrcs = srcs;
        mDest = dest;
        mNotificationID = CustomNotificationManager.getInstance().queryNotificationID();
        mType = getContext().getString(R.string.copy);
        mTotal = mSrcs.size();
        mCurrent = 0;
    }

    @Override
    public Boolean loadInBackground() {
        try {
            super.loadInBackground();
            return copy();
        } catch (Exception e) {
            e.printStackTrace();
            closeProgressWatcher();
            setException(e);
            updateResult(mType, getContext().getString(R.string.error), mDest);
        }
        return false;
    }

    private boolean copy() throws MalformedURLException, SmbException {
        for (String path : mSrcs) {
            if(!success)
                break;

            SmbFile source = new SmbFile(getSmbUrl(path));
            if (source.isDirectory())
                copyDirectory(source, getSmbUrl(mDest));
            else
                copyFile(source, getSmbUrl(mDest));
            mCurrent++;
        }

        if(success)
            updateResult(mType, getContext().getString(R.string.done), mDest);
        else
            updateResult(mType, getContext().getString(R.string.error), mDest);
        return true;
    }

    private void copyDirectory(SmbFile source, String destination) throws MalformedURLException, SmbException {
        String name = createRemoteUniqueName(source, destination);
        SmbFile target = new SmbFile(destination, name);
        target.mkdirs();
        SmbFile[] files = source.listFiles();
        for (SmbFile file : files) {
            if (!file.isHidden())
                mTotal++;
        }
        String path = target.getPath();
        for (SmbFile file : files) {
            if(!success)
                break;

            if(file.isHidden())
                continue;

            if (file.isDirectory())
                copyDirectory(file, path);
            else
                copyFile(file, path);
            mCurrent++;
        }
    }

    private void copyFile(SmbFile source, String destination) throws MalformedURLException, SmbException {
        String name = createRemoteUniqueName(source, destination);
        int total = source.getContentLength();
        updateProgress(mType, name, 0, total);

        SmbFile target = new SmbFile(destination, name);
        startProgressWatcher(name, destination, total);
        source.copyTo(target);
        closeProgressWatcher();

        updateProgress(mType, name, total, total);
    }
}
