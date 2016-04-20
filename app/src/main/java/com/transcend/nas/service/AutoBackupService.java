package com.transcend.nas.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.management.FileInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jcifs.smb.SmbException;

/**
 * Created by ike_lee on 2016/3/22.
 */
public class AutoBackupService extends Service implements RecursiveFileObserver.RecursiveFileChanged, AutoBackupTask.AutoBackupTaskCallback {

    private static final String TAG = "AutoBackupService";
    private static final int NOTIFICATION_ID = 1;
    private ConnectivityManager connectivityManager;
    private AutoBackupHelper mHelper;
    private NetworkInfo info;
    private RecursiveFileObserver mLocalFileObserver;
    private Handler mHandler;
    private HashSet<String> jbCache = new HashSet();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate() executed");

        mHelper = new AutoBackupHelper(getApplicationContext(), Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath());

        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mReceiver, mFilter);

        mLocalFileObserver = new RecursiveFileObserver(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath());
        mLocalFileObserver.addListener(this);
        mLocalFileObserver.startWatching();

        mHandler = new Handler();
        initBackup();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() executed");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        mHelper.onDestroy();
        mHelper = null;
        mLocalFileObserver.removeListener();
        mLocalFileObserver.stopWatching();
        mLocalFileObserver = null;
        AutoBackupQueue.getInstance().onDestroy();
        Log.d(TAG, "onDestroy() executed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                boolean wifi_only = NASPref.getBackupScenario(getApplicationContext()).equals("WIFI_ONLY");
                connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                info = connectivityManager.getActiveNetworkInfo();
                if (info != null && info.isAvailable()) {
                    String name = info.getTypeName();
                    Log.d(TAG, "Current network name：" + name);

                    int type = info.getType();
                    switch (type) {
                        case ConnectivityManager.TYPE_WIFI:
                            initBackup();
                            break;
                        case ConnectivityManager.TYPE_MOBILE:
                            if(wifi_only) {
                                Log.d(TAG, "Clean upload task due to wifi only network");
                                AutoBackupQueue.getInstance().cleanUploadTask();
                            }
                            else
                                initBackup();
                            break;
                        default:
                            break;
                    }
                } else {
                    Log.d(TAG, "Clean upload task due to no network");
                    AutoBackupQueue.getInstance().cleanUploadTask();
                }
            }
        }
    };

    private void initBackup(){
        mHelper.init();
        ArrayList<String> list = mHelper.getNeedUploadImageList(true);
        if(list != null && list.size() > 0) {
            Log.d(TAG, "Clean upload task due to init backup");
            AutoBackupQueue.getInstance().cleanUploadTask();
            if (canAddTaskToQueue())
                addBackupTaskToQueue(list, 1);
            else
                showOnceNotification("Backup Fail", "can't connect to NAS", NOTIFICATION_ID);
        } else {
            //showOnceNotification("Backup Success", "Already backup all images", NOTIFICATION_ID);
        }
    }

    private void showProgressNotification(String title, int id, int TotalItem, int FinishItem) {
        final int notifyID = id; // 通知的識別號碼
        final boolean autoCancel = true; // 點擊通知後是否要自動移除掉通知
        final int progressMax = TotalItem; // 進度條的最大值，通常都是設為100。若是設為0，且indeterminate為false的話，表示不使用進度條
        final int progress = FinishItem; // 進度值
        final boolean indeterminate = false; // 是否為不確定的進度，如果不確定的話，進度條將不會明確顯示目前的進度。若是設為false，且progressMax為0的話，表示不使用進度條

        final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE); // 取得系統的通知服務
        final Notification notification = new Notification.Builder(getApplicationContext())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setProgress(progressMax, progress, indeterminate)
                .setAutoCancel(autoCancel)
                .build();
        notificationManager.notify(notifyID, notification); // 發送通知
    }

    private void showOnceNotification(String title, String contentText, int id) {
        final int notifyID = id; // 通知的識別號碼
        final boolean autoCancel = true; // 點擊通知後是否要自動移除掉通知

        final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE); // 取得系統的通知服務
        final Notification notification = new Notification.Builder(getApplicationContext())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(contentText)
                .setAutoCancel(autoCancel)
                .build();
        notificationManager.notify(notifyID, notification); // 發送通知
    }

    @Override
    public void onRecursiveFileChanged(int event, String path) {
        String[] detail = path.split("/");
        int length = detail.length;
        if (length > 0 && detail[length-1].startsWith(".")) {
            Log.d(TAG, "tmp file : " + path);
            return;
        }

        switch (event) {
            case FileObserver.CLOSE_WRITE:
                File pictureFile = new File(path);
                if(pictureFile.exists() && canAddTaskToQueue()) {
                    ArrayList<String> paths = new ArrayList<String>();
                    paths.add(path);
                    addBackupTaskToQueue(paths, 1);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onAutoBackupTaskPerFinished(AutoBackupTask task, int total, int progress) {
        //show backup success notification
        String value = "Backup File " + Integer.toString(progress) + "/" + Integer.toString(total);
        showProgressNotification(value, NOTIFICATION_ID, total, progress);

        //add the finished backup task to database
        String path = task.getFilePaths().get(progress - 1);
        addBackupTaskToDatabase(path);
    }

    @Override
    public void onAutoBackupTaskFinished(AutoBackupTask task) {
        int size = task.getFilePaths().size();
        if (size > 1) {
            showOnceNotification("Backup success", "Success backup " + size + " Images", NOTIFICATION_ID);
        }
        else if (size == 1){
            showOnceNotification("Backup success", task.getFileUniqueName(), NOTIFICATION_ID);
            String path = task.getFilePaths().get(0);
            addBackupTaskToDatabase(path);
        }
        else {
            //TODO : check why path size is 0
            showOnceNotification("Backup success!", task.getFileUniqueName(), NOTIFICATION_ID);
        }

        AutoBackupQueue.getInstance().removeUploadTask(task);
        mHandler.postDelayed(runnable, 200);
    }

    @Override
    public void onAutoBackupTaskFail(AutoBackupTask task, Exception e) {
        if(e instanceof SmbException){
            String message = ((SmbException) e).getMessage();
            if(message.equals("0xC000007F")){
                showOnceNotification("Backup Fail", "No enough disk space", NOTIFICATION_ID);
                AutoBackupQueue.getInstance().cleanUploadTask();
                return;
            }
        }

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            showOnceNotification("Backup Fail", task.getFileUniqueName(), NOTIFICATION_ID);
            //same async task object can't execute twice, remove the task from queue
            ArrayList<String> paths = task.getFilePaths();
            AutoBackupQueue.getInstance().removeUploadTask(task);

            //Check the task retry count to determine add the task to queue or not
            if (task.checkRetryCount()) {
                if(paths.size() > 1) {
                    ArrayList<String> list = mHelper.filterUploadImageList(paths);
                    addBackupTaskToQueue(list, task.getRetryCount() - 1);
                }
                else {
                    addBackupTaskToQueue(paths, task.getRetryCount() - 1);
                }
            }
            //TODO: SMB TIMEOUT
        } else {
            showOnceNotification("Backup Fail", "can't connect to NAS", NOTIFICATION_ID);
            AutoBackupQueue.getInstance().cleanUploadTask();
        }
    }

    private boolean canAddTaskToQueue(){
        boolean need = true;
        boolean wifi_only = NASPref.getBackupScenario(getApplicationContext()).equals("WIFI_ONLY");
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            int type = info.getType();
            switch (type) {
                case ConnectivityManager.TYPE_MOBILE:
                    if(wifi_only)
                        need = false;
                    break;
                default:
                    break;
            }
        } else {
            need = false;
        }
        return need;
    }

    private void addBackupTaskToQueue(ArrayList<String> paths, int retry) {
        String des = NASPref.getBackupLocation(getApplicationContext());
        AutoBackupTask task = new AutoBackupTask(getApplicationContext(), paths, des);
        task.addListener(this);
        task.setRetryCount(retry);
        AutoBackupQueue.getInstance().addUploadTask(task);
        mHandler.postDelayed(runnable, 200);
    }

    private void addBackupTaskToDatabase(String path){
        String hostname = NASPref.getHostname(getApplicationContext());
        File file = new File(path);
        mHelper.insertTask(file.getName(), file.getPath(), Long.toString(file.lastModified()), hostname);
    }

    private Runnable runnable = new Runnable() {
        public void run() {
            AutoBackupQueue.getInstance().doUploadTask();
        }
    };
}
