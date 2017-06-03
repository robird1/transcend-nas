package com.transcend.nas.service;

import android.content.Context;
import android.util.Log;

import com.transcend.nas.NASPref;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ike_lee on 2016/3/22.
 */
public class AutoBackupQueue {
    private static String TAG = "AutoBackupQueue";
    private List<AutoBackupTask> mUploadQueue;
    private boolean isRunning = false;

    private static AutoBackupQueue mAutoBackupQueue;
    private static final Object mMute = new Object();

    public static AutoBackupQueue getInstance() {
        synchronized (mMute) {
            if (mAutoBackupQueue == null)
                mAutoBackupQueue = new AutoBackupQueue();
        }
        return mAutoBackupQueue;
    }

    public AutoBackupQueue() {
        mUploadQueue = new ArrayList<AutoBackupTask>();
    }

    public int getUploadQueueSize() {
        if (mUploadQueue != null)
            return mUploadQueue.size();
        return 0;
    }

    public boolean isTaskRunning() {
        return isRunning;
    }

    public void doUploadTask() {
        if (isRunning)
            return;

        AutoBackupTask task = getUploadTask();
        if(task != null){
            isRunning = true;
            task.execute();
        }
        else{
            isRunning = false;
        }
    }

    public AutoBackupTask getUploadTask() {
        if (mUploadQueue != null && mUploadQueue.size() > 0) {
            return mUploadQueue.get(0);
        }

        return null;
    }

    public boolean removeUploadTask(AutoBackupTask task) {
        for (AutoBackupTask tmp : mUploadQueue) {
            if (tmp == task) {
                mUploadQueue.remove(task);
                isRunning = false;
                return true;
            }
        }

        return false;
    }

    public boolean addTaskCheck(String checkPath) {
        if (mUploadQueue != null) {
            if (checkPath != null && !"".equals(checkPath)) {
                for (AutoBackupTask tmp : mUploadQueue) {
                    if (tmp != null) {
                        ArrayList<String> paths = tmp.getFilePaths();
                        if (paths != null && paths.size() > 0) {
                            String path = paths.get(0);
                            if (checkPath.equals(path)) {
                                return false;
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    public void addUploadTask(AutoBackupTask task) {
        if (mUploadQueue != null) {
            mUploadQueue.add(task);
            Log.d(TAG, "Auto backup, queue size: " + mUploadQueue.size() );
        }
    }

    public void cancelUploadTask(Context context) {
        if (mUploadQueue != null){
            if(isRunning && mUploadQueue.size() > 0) {
                AutoBackupTask task = mUploadQueue.get(0);
                NASPref.setBackupErrorTask(context, task.getFileUniqueName());
                Log.d(TAG, "Error task add : " + task.getFileUniqueName());
                task.cancel(true);
            }
        }
        isRunning = false;
    }

    public void  cleanUploadTask(Context context) {
        if (mUploadQueue != null) {
            cancelUploadTask(context);
            mUploadQueue.clear();
        }
    }

    public void onDestroy(Context context) {
        cleanUploadTask(context);
        mUploadQueue = null;
        mAutoBackupQueue = null;
    }
}
