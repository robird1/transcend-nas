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
 * Created by silverhsu on 16/2/18.
 */
public class LocalFileMoveLoader extends AsyncTaskLoader<Boolean> {

    private static final String TAG = LocalFileMoveLoader.class.getSimpleName();

    private Activity mActivity;
    private HandlerThread mThread;
    private Handler mHandler;
    private Runnable mWatcher;

    private List<String> mSrcs;
    private String mDest;

    public LocalFileMoveLoader(Context context, List<String> srcs, String dest) {
        super(context);
        mActivity = (Activity) context;
        mSrcs = srcs;
        mDest = dest;
    }

    @Override
    public Boolean loadInBackground() {
        try {
            return move();
        } catch (IOException e) {
            e.printStackTrace();
            closeProgressWatcher();
            updateResult("Error");
        }
        return false;
    }

    private boolean move() throws IOException {
        updateProgress(getContext().getResources().getString(R.string.loading), 0, 0);
        for (String path : mSrcs) {
            File source = new File(path);
            if (source.getParent().equals(mDest))
                continue;
            if (source.isDirectory())
                moveDirectory(source, mDest);
            else
                moveFile(source, mDest);
        }
        updateResult("Done");
        return true;
    }

    private void moveDirectory(File source, String destination) throws IOException  {
        String name = createUniqueName(source, destination);
        File target = new File(destination, name);
        target.mkdirs();
        File[] files = source.listFiles();
        String path = target.getPath();
        for (File file : files) {
            if (file.isDirectory())
                moveDirectory(file, path);
            else
                moveFile(file, path);
        }
        source.delete();
    }

    private void moveFile(File source, String destination) throws IOException {
        String name = createUniqueName(source, destination);
        File target = new File(destination, name);
        int total = (int) source.length();
        startProgressWatcher(target, total);
        source.renameTo(target);
        closeProgressWatcher();
        updateProgress(target.getName(), total, total);
    }

    private String createUniqueName(File source, String destination) throws MalformedURLException, SmbException {
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
            unique = String.format(prefix + " (%d)" + suffix, index++);
        }
        return unique;
    }

    private void startProgressWatcher(final File target, final int total) {
        mThread = new HandlerThread(TAG);
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
        mHandler.post(mWatcher = new Runnable() {
            @Override
            public void run() {
                int count = (int) target.length();
                if (mHandler != null) {
                    mHandler.postDelayed(mWatcher, 1000);
                    updateProgress(target.getName(), count, total);
                }
            }
        });
    }

    private void closeProgressWatcher() {
        if (mHandler != null) {
            mHandler.removeCallbacks(mWatcher);
            mHandler = null;
        }
        if (mThread != null) {
            mThread.quit();
            mThread = null;
        }
    }

    private void updateProgress(String name, int count, int total) {
        Log.w(TAG, "progress: " + count + "/" + total + ", " + name);

        int max = (count == total) ? 0 : 100;
        int progress = (total > 0) ? count / (total / 100) : 0;
        boolean indeterminate = (total == 0);
        int icon = R.mipmap.ic_launcher;

        String type = getContext().getResources().getString(R.string.move);
        String stat = String.format("%s / %s", MathUtil.getBytes(count), MathUtil.getBytes(total));
        String text = String.format("%s - %s", type, stat);
        String info = String.format("%d%%", progress);

        NotificationManager ntfMgr = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = mActivity.getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext());
        builder.setSmallIcon(icon);
        builder.setContentTitle(name);
        builder.setContentText(text);
        builder.setContentInfo(info);
        builder.setProgress(max, progress, indeterminate);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(true);
        ntfMgr.notify(0, builder.build());
    }

    private void updateResult(String result) {
        Log.w(TAG, "result: " + result);

        int icon = R.mipmap.ic_launcher;
        String name = getContext().getResources().getString(R.string.app_name);
        String type = getContext().getResources().getString(R.string.move);
        String text = String.format("%s - %s", type, result);

        NotificationManager ntfMgr = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = mActivity.getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext());
        builder.setSmallIcon(icon);
        builder.setContentTitle(name);
        builder.setContentText(text);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(true);
        ntfMgr.notify(0, builder.build());
    }

}
