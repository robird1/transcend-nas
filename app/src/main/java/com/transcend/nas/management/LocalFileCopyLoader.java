package com.transcend.nas.management;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.transcend.nas.R;
import com.transcend.nas.common.CustomNotificationManager;
import com.transcend.nas.management.firmware.FileFactory;
import com.transcend.nas.utils.MathUtil;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import jcifs.smb.SmbException;

/**
 * Created by silverhsu on 16/2/16.
 */
public class LocalFileCopyLoader extends LocalAbstractLoader {

    private static final String TAG = LocalFileCopyLoader.class.getSimpleName();
    private List<String> mSrcs;
    private String mDest;

    public LocalFileCopyLoader(Context context, List<String> srcs, String dest) {
        super(context);
        setType(getContext().getString(R.string.copy));
        mSrcs = srcs;
        mDest = dest;
        mTotal = mSrcs.size();
        mCurrent = 0;
        mNotificationID = CustomNotificationManager.getInstance().queryNotificationID();
    }

    @Override
    public Boolean loadInBackground() {
        try {
            return copy();
        } catch (IOException e) {
            e.printStackTrace();
            closeProgressWatcher();
            updateResult(getContext().getString(R.string.error), mDest);
        }
        return false;
    }

    private boolean copy() throws IOException {
        updateProgress(getContext().getResources().getString(R.string.loading), 0, 0);
        for (String path : mSrcs) {
            File source = new File(path);
            if (source.isDirectory())
                copyDirectory(source, mDest);
            else
                copyFile(source, mDest);
            mCurrent++;
        }
        updateResult(getContext().getString(R.string.done), mDest);
        return true;
    }

    private void copyDirectory(File source, String destination) throws IOException  {
        String name = createUniqueName(source, destination);
        File target = new File(destination, name);
        target.mkdirs();
        File[] files = source.listFiles();
        for (File file : files) {
            if (!file.isHidden())
                mTotal++;
        }

        String path = target.getPath();
        for (File file : files) {
            if(file.isHidden())
                continue;

            if (file.isDirectory())
                copyDirectory(file, path);
            else
                copyFile(file, path);
            mCurrent++;
        }
    }

    private void copyFile(File source, String destination) throws IOException {
        String name = createUniqueName(source, destination);
        File target = new File(destination, name);
        int total = (int) source.length();
        startProgressWatcher(name, target, total);
        FileUtils.copyFile(source, target);
        closeProgressWatcher();
        updateProgress(name, total, total);
    }
}
