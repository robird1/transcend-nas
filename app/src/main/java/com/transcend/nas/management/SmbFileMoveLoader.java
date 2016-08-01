package com.transcend.nas.management;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.transcend.nas.R;
import com.transcend.nas.utils.FileFactory;
import com.transcend.nas.utils.MathUtil;

import org.apache.commons.io.FilenameUtils;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileFilter;

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
        mNotificationID = FileFactory.getInstance().getNotificationID();
        mType = getContext().getString(R.string.move);
    }

    @Override
    public Boolean loadInBackground() {
        try {
            super.loadInBackground();
            return move();
        } catch (Exception e) {
            e.printStackTrace();
            closeProgressWatcher();
            setException(e);
            updateResult(mType, getContext().getString(R.string.error), mDest);
        }
        return false;
    }

    private boolean move() throws MalformedURLException, SmbException {
        updateProgress(mType, getContext().getResources().getString(R.string.loading), 0, 0);
        for (String path : mSrcs) {
            SmbFile source = new SmbFile(getSmbUrl(path));
            if (source.getParent().endsWith(mDest))
                continue;
            if (source.isDirectory())
                moveDirectory(source, getSmbUrl(mDest));
            else
                moveFile(source, getSmbUrl(mDest));
        }
        updateResult(mType, getContext().getString(R.string.done), mDest);
        return true;
    }

    private void moveDirectory(SmbFile source, String destination) throws MalformedURLException, SmbException {
        String name = createUniqueName(source, destination);
        SmbFile target = new SmbFile(destination, name);
        target.mkdirs();
        SmbFile[] files = source.listFiles();
        String path = target.getPath();
        for (SmbFile file : files) {
            if(file.isHidden())
                continue;

            if (file.isDirectory())
                moveDirectory(file, path);
            else
                moveFile(file, path);
        }
        source.delete();
    }

    private void moveFile(SmbFile source, String destination) throws MalformedURLException, SmbException {
        String name = createUniqueName(source, destination);
        SmbFile target = new SmbFile(destination, name);
        int total = getSize(source);
        startProgressWatcher(target, total);
        source.renameTo(target);
        closeProgressWatcher();
        updateProgress(mType, target.getName(), total, total);
    }
}
