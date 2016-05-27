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

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.management.AutoBackupLoader;
import com.transcend.nas.management.FileInfo;
import com.tutk.IOTC.P2PService;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
    private Thread mThread;
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
        if (mThread != null)
            mThread.interrupt();
        mThread = null;
        if (mHelper != null)
            mHelper.onDestroy();
        mHelper = null;
        mLocalFileObserver.removeListener();
        mLocalFileObserver.stopWatching();
        mLocalFileObserver = null;
        if (AutoBackupQueue.getInstance().getUploadQueueSize() > 0) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(NOTIFICATION_ID);
        }
        AutoBackupQueue.getInstance().onDestroy(getApplicationContext());
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
                boolean wifi_only = isWifiOnly();
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
                            if (wifi_only) {
                                Log.d(TAG, "Clean upload task due to wifi only network");
                                AutoBackupQueue.getInstance().cleanUploadTask(getApplicationContext());
                                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                                notificationManager.cancel(NOTIFICATION_ID);
                            } else {
                                initBackup();
                            }
                            break;
                        default:
                            break;
                    }
                } else {
                    Log.d(TAG, "Clean upload task due to no network");
                    AutoBackupQueue.getInstance().cleanUploadTask(getApplicationContext());
                    NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancel(NOTIFICATION_ID);
                }
            }
        }
    };

    private boolean isWifiOnly() {
        String scenario = NASPref.getBackupScenario(getApplicationContext());
        String[] scenarios = getApplicationContext().getResources().getStringArray(R.array.backup_scenario_values);
        return Arrays.asList(scenarios).indexOf(scenario) == 1;
    }

    private void initBackup() {
        mThread = new Thread() {
            public void run() {
                if (mHelper == null)
                    mHelper = new AutoBackupHelper(getApplicationContext(), Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath());
                ArrayList<String> list = mHelper.getNeedUploadImageList(true);
                if (list != null && list.size() > 0) {
                    Log.d(TAG, "Clean upload task due to init backup");
                    AutoBackupQueue.getInstance().cleanUploadTask(getApplicationContext());
                    if (canAddTaskToQueue())
                        addBackupTaskToQueue(list, 1);
                } else {
                    //showOnceNotification(getString(R.string.backup_success), "Already backup all images", NOTIFICATION_ID);
                }
            }
        };
        mThread.start();
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
        if (length > 0 && detail[length - 1].startsWith(".")) {
            Log.d(TAG, "tmp file : " + path);
            return;
        }

        switch (event) {
            case FileObserver.CLOSE_WRITE:
                File pictureFile = new File(path);
                if (pictureFile.exists() && canAddTaskToQueue()) {
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
        String value = getString(R.string.backup_file) + " " + Integer.toString(progress) + "/" + Integer.toString(total);
        showProgressNotification(value, NOTIFICATION_ID, total, progress);

        //add the finished backup task to database
        String path = task.getFilePaths().get(progress - 1);
        addBackupTaskToDatabase(path);
    }

    @Override
    public void onAutoBackupTaskFinished(AutoBackupTask task) {
        int size = task.getFilePaths().size();
        if (size > 1) {
            showOnceNotification(getString(R.string.backup_success), getString(R.string.backup_file) + " " + size, NOTIFICATION_ID);
        } else if (size == 1) {
            showOnceNotification(getString(R.string.backup_success), task.getFileUniqueName(), NOTIFICATION_ID);
            String path = task.getFilePaths().get(0);
            addBackupTaskToDatabase(path);
        } else {
            //TODO : check why path size is 0
            showOnceNotification(getString(R.string.backup_success), task.getFileUniqueName(), NOTIFICATION_ID);
        }

        AutoBackupQueue.getInstance().removeUploadTask(task);
        Thread thread = new Thread() {
            public void run() {
                mHandler.postDelayed(runnable, 200);
            }
        };
        thread.start();
    }

    @Override
    public void onAutoBackupTaskFail(AutoBackupTask task, Exception e) {
        if (e instanceof SmbException) {
            String message = ((SmbException) e).getMessage();
            if (message.equals("0xC000007F")) {
                showOnceNotification(getString(R.string.backup_fail), getString(R.string.backup_fail_no_space), NOTIFICATION_ID);
                AutoBackupQueue.getInstance().cleanUploadTask(getApplicationContext());
                return;
            }
        }

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            //same async task object can't execute twice, remove the task from queue
            ArrayList<String> paths = task.getFilePaths();
            AutoBackupQueue.getInstance().removeUploadTask(task);

            //Check the task retry count to determine add the task to queue or not
            if (task.checkRetryCount()) {
                boolean success = doRemoteAccessLink(info.getType());
                Log.d(TAG, "try remote access : " + success);

                if (paths.size() > 1) {
                    ArrayList<String> list = mHelper.filterUploadImageList(paths);
                    addBackupTaskToQueue(list, task.getRetryCount() - 1);
                } else {
                    addBackupTaskToQueue(paths, task.getRetryCount() - 1);
                }
            } else {
                String name = task.getFileUniqueName();
                showOnceNotification(getString(R.string.backup_fail), name.equals("") ? getString(R.string.backup_fail_network_unreachable) : name, NOTIFICATION_ID);
            }
        } else {
            showOnceNotification(getString(R.string.backup_fail), getString(R.string.backup_fail_network_unreachable), NOTIFICATION_ID);
            AutoBackupQueue.getInstance().cleanUploadTask(getApplicationContext());
        }
    }

    private boolean doRemoteAccessLink(int type) {
        boolean success = false;
        boolean wifi_only = isWifiOnly();
        if (type == ConnectivityManager.TYPE_MOBILE && wifi_only) {
            return false;
        }

        String uuid = NASPref.getCloudUUID(getApplicationContext());
        //link error, construct the remote access link
        if (uuid != null && !uuid.equals("")) {
            P2PService.getInstance().stopP2PConnect();
            int result = P2PService.getInstance().startP2PConnect(uuid);
            if (result >= 0) {
                success = true;
            } else {
                P2PService.getInstance().stopP2PConnect();
            }
        }

        return success;
    }

    private boolean canAddTaskToQueue() {
        boolean need = true;
        boolean wifi_only = isWifiOnly();
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            int type = info.getType();
            switch (type) {
                case ConnectivityManager.TYPE_MOBILE:
                    need = !wifi_only;
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
        String errorPath = NASPref.getBackupErrorTask(getApplicationContext());
        String des = NASPref.getBackupLocation(getApplicationContext());
        AutoBackupTask task = new AutoBackupTask(getApplicationContext(), paths, des, P2PService.getInstance().isConnected(), errorPath);
        task.addListener(this);
        task.setRetryCount(retry);
        AutoBackupQueue.getInstance().addUploadTask(task);
        Thread thread = new Thread() {
            public void run() {
                mHandler.postDelayed(runnable, 200);
            }
        };
        thread.start();
    }

    private void addBackupTaskToDatabase(String path) {
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String hostname = server.getTutkUUID();
        if (hostname == null) {
            hostname = NASPref.getUUID(getApplicationContext());
        }

        File file = new File(path);
        if (mHelper == null)
            mHelper = new AutoBackupHelper(getApplicationContext(), Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath());
        mHelper.insertTask(file.getName(), file.getPath(), Long.toString(file.lastModified()), hostname);
    }

    private Runnable runnable = new Runnable() {
        public void run() {
            AutoBackupQueue.getInstance().doUploadTask();
        }
    };
}
