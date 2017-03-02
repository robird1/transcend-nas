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
import com.transcend.nas.common.CustomNotificationReceiver;
import com.transcend.nas.utils.MathUtil;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import jcifs.smb.SmbException;

/**
 * Created by silverhsu on 16/2/18.
 */
public class LocalAbstractLoader extends AsyncTaskLoader<Boolean> {

    private static final String TAG = LocalAbstractLoader.class.getSimpleName();
    private Activity mActivity;
    protected HandlerThread mThread;
    protected Handler mHandler;
    protected Runnable mWatcher;
    protected String mType = "";

    protected int mNotificationID = 0;
    protected int mTotal = 0;
    protected int mCurrent = 0;
    private String[] mLoadingString = {".","..","...","...."};
    private NotificationCompat.Builder mBuilder;

    public LocalAbstractLoader(Context context) {
        super(context);
        mActivity = (Activity) context;
    }

    @Override
    public Boolean loadInBackground() {
        return true;
    }

    protected void setType(String type){
        mType = type;
    }

    public String getType() {
        return mType;
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

    protected void updateProgress(String name, int count, int total){
        updateProgress(name, count, total, true);
    }

    protected void updateProgress(String name, int count, int total, boolean showProgress) {
        if(isLoadInBackgroundCanceled()) {
            return;
        }

        Log.w(TAG, mNotificationID + " progress: " + count + "/" + total + ", " + name);
        if (mBuilder == null) {
            mBuilder = CustomNotificationManager.createProgressBuilder(getContext(), mActivity, mNotificationID);
        }

        if(showProgress) {
            int max = 100;
            int progress = (total > 100) ? count / (total / 100) : 0;
            boolean indeterminate = (total == 0);

            String stat = String.format("%s / %s", MathUtil.getBytes(count), MathUtil.getBytes(total));
            String text = String.format("%s - %s", mType, stat);
            String info = String.format("%d%%", progress);

            mBuilder.setContentText(text);
            mBuilder.setContentInfo(info);
            mBuilder.setProgress(max, progress, indeterminate);
        } else {
            String loading = mLoadingString[mCurrent%mLoadingString.length];
            mBuilder.setContentText(String.format("%s%s", mType, loading));
        }

        String title = mTotal > 1 ? String.format("(%s/%s) " + name, mCurrent, mTotal) : name;
        mBuilder.setContentTitle(title);

        NotificationManager ntfMgr = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        ntfMgr.notify(mNotificationID, mBuilder.build());
    }

    protected void updateResult(String result, String destination) {
        if(isLoadInBackgroundCanceled()) {
            return;
        }

        CustomNotificationManager.updateResult(getContext(), mNotificationID, mType, result, destination);
    }

}
