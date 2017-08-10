package com.transcend.nas.management;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import jcifs.smb.SmbException;

/**
 * Created by silverhsu on 16/2/18.
 */
public class LocalAbstractLoader extends FileAbstractLoader {
    private static final String TAG = LocalAbstractLoader.class.getSimpleName();

    protected HandlerThread mThread;
    protected Handler mHandler;
    protected Runnable mWatcher;

    public LocalAbstractLoader(Context context) {
        super(context);
    }

    @Override
    public Boolean loadInBackground() {
        return true;
    }

    protected String createUniqueName(File source, String destination) throws MalformedURLException, SmbException {
        final boolean isDirectory = source.isDirectory();
        File dir = new File(destination);
        File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory() == isDirectory;
            }
        });
        List<String> names = new ArrayList<String>();
        for (File file : files) names.add(file.getName());
        String origin = source.getName();
        String unique = origin;
        String ext = FilenameUtils.getExtension(origin);
        String prefix = FilenameUtils.getBaseName(origin);
        String suffix = ext.isEmpty() ? "" : String.format(".%s", ext);
        int index = 2;
        while (names.contains(unique)) {
            unique = String.format(prefix + "_%d" + suffix, index++);
        }
        return unique;
    }

    protected void startProgressWatcher(final String title, final File target, final int total) {
        mThread = new HandlerThread(TAG);
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
        mHandler.postDelayed(mWatcher = new Runnable() {
            @Override
            public void run() {
                if(isLoadInBackgroundCanceled())
                    return;

                if(target != null) {
                    int count = (int) target.length();
                    updateProgress(title, count, total);
                }

                if (mHandler != null) {
                    mHandler.postDelayed(mWatcher, 1000);
                }
            }
        }, 1000);
    }

    protected void closeProgressWatcher() {
        if (mHandler != null) {
            mHandler.removeCallbacks(mWatcher);
            mHandler = null;
        }
        if (mThread != null) {
            mThread.quit();
            mThread = null;
        }
    }
}
