package com.transcend.nas.management;

import android.app.Activity;
import android.content.Context;

import com.transcend.nas.R;
import com.transcend.nas.common.CustomNotificationManager;

import java.net.MalformedURLException;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * Created by silverhsu on 16/2/17.
 */
public class SmbFileMoveLoader extends SmbAbstractLoader {

    private List<String> mSrcs;
    private String mDest;

    public SmbFileMoveLoader(Context context, List<String> srcs, String dest) {
        super(context);
        mActivity = (Activity) context;
        mSrcs = srcs;
        mDest = dest;
        mNotificationID = CustomNotificationManager.getInstance().queryNotificationID();
        mType = getContext().getString(R.string.move);
        mTotal = mSrcs.size();
        mCurrent = 0;
    }

    @Override
    public Boolean loadInBackground() {
        try {
            super.loadInBackground();
            return move();
        } catch (Exception e) {
            e.printStackTrace();
            setException(e);
            updateResult(mType, getContext().getString(R.string.error), mDest);
        }
        return false;
    }

    private boolean move() throws MalformedURLException, SmbException {
        for (String path : mSrcs) {
            SmbFile source = new SmbFile(getSmbUrl(path));
            if (source.getParent().endsWith(mDest))
                continue;
            if (source.isDirectory())
                moveDirectory(source, getSmbUrl(mDest));
            else
                moveFile(source, getSmbUrl(mDest));
            mCurrent++;
        }
        updateResult(mType, getContext().getString(R.string.done), mDest);
        return true;
    }

    private void moveDirectory(SmbFile source, String destination) throws MalformedURLException, SmbException {
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
            if(file.isHidden())
                continue;

            if (file.isDirectory())
                moveDirectory(file, path);
            else
                moveFile(file, path);
            mCurrent++;
        }
        source.delete();
    }

    private void moveFile(SmbFile source, String destination) throws MalformedURLException, SmbException {
        String name = createRemoteUniqueName(source, destination);
        SmbFile target = new SmbFile(destination, name);
        source.renameTo(target);
        updateProgress(mType, name, 0, 0, false);
    }
}
