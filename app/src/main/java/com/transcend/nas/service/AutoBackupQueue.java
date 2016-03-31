package com.transcend.nas.service;

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

    public void addUploadTask(AutoBackupTask task) {
        if (mUploadQueue != null) {
            mUploadQueue.add(task);
        }
    }

    public void cancelUploadTask() {
        if (mUploadQueue != null){
            if(isRunning && mUploadQueue.size() > 0) {
                AutoBackupTask task = mUploadQueue.get(0);
                task.cancel(true);
            }
        }
        isRunning = false;
    }

    public void  cleanUploadTask() {
        if (mUploadQueue != null) {
            cancelUploadTask();
            mUploadQueue.clear();
        }
    }

    public void onDestroy() {
        if (mUploadQueue != null) {
            cancelUploadTask();
            mUploadQueue.clear();
            mUploadQueue = null;
        }
        mAutoBackupQueue = null;
    }
}
