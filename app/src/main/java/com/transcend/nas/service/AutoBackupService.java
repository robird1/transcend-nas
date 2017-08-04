package com.transcend.nas.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.hardware.Camera;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerInfo;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.common.CustomNotificationManager;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.settings.SettingBackupActivity;
import com.tutk.IOTC.P2PService;

import java.io.File;
import java.util.ArrayList;

import jcifs.smb.SmbException;

import static android.hardware.Camera.ACTION_NEW_PICTURE;
import static android.hardware.Camera.ACTION_NEW_VIDEO;

/**
 * Created by ike_lee on 2016/3/22.
 */
public class AutoBackupService extends Service implements RecursiveFileObserver.RecursiveFileChanged, AutoBackupTask.AutoBackupTaskCallback {

    private static final String TAG = "AutoBackupService";
    private static final int NOTIFICATION_ID = 0;
    private ConnectivityManager connectivityManager;
    private AutoBackupHelper mHelper;
    private NetworkInfo info;
    private RecursiveFileObserver mLocalFileObserver;
    private Handler mHandler;
    private Thread mThread;
    private NotificationCompat.Builder mBuilder;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate() executed");
        String backupSource = NASPref.getBackupSource(getApplicationContext());

        mHelper = new AutoBackupHelper(getApplicationContext(), backupSource);
        mLocalFileObserver = new RecursiveFileObserver(backupSource);
        mLocalFileObserver.addListener(this);
        mLocalFileObserver.startWatching();
        mHandler = new Handler();
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mReceiver, mFilter);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Camera.ACTION_NEW_PICTURE);
        filter.addAction(Camera.ACTION_NEW_VIDEO);
        try {
            filter.addDataType("image/*");
            filter.addDataType("video/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            e.printStackTrace();
        }
        registerReceiver(mCameraReceiver, filter);
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
        unregisterReceiver(mCameraReceiver);
        if (mThread != null)
            mThread.interrupt();
        mThread = null;
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

    private BroadcastReceiver mCameraReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_NEW_PICTURE) || action.equals(ACTION_NEW_VIDEO)) {
                Log.d(TAG, "onReceiveIntent : " + action);
                Uri imageUri = intent.getData();
                if (imageUri != null) {
                    String realPath = getRealPathFromURI(imageUri);
                    String backupSource = NASPref.getBackupSource(getApplicationContext());
                    if (realPath != null && realPath.contains(backupSource))
                        prepareBackupTask(realPath);
                    else
                        Log.d(TAG, "Ignore file name : " + realPath + ", due to different backup source");
                }
            }
        }
    };


    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    private boolean isWifiOnly() {
        boolean scenario = NASPref.getBackupScenario(getApplicationContext());
        return !scenario;
    }

    private void initBackup() {
        mThread = new Thread() {
            public void run() {
                if (mHelper == null) {
                    String backupSource = NASPref.getBackupSource(getApplicationContext());
                    mHelper = new AutoBackupHelper(getApplicationContext(), backupSource);
                }
                ArrayList<String> list = mHelper.getNeedUploadImageList(true);
                if (list == null) {
                    String title = getString(R.string.auto_backup);
                    String contentText = getString(R.string.msg_path_check);
                    CustomNotificationManager.updateResult(getApplicationContext(), NOTIFICATION_ID, title, contentText, null, SettingBackupActivity.class);
                    return;
                }

                if (list.size() > 0) {
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
        if (mBuilder == null) {
            mBuilder = CustomNotificationManager.createProgressBuilder(getApplicationContext(), id);
        }

        mBuilder.setProgress(TotalItem, FinishItem, false);
        mBuilder.setContentTitle(title);
        NotificationManager ntfMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        ntfMgr.notify(id, mBuilder.build());
    }

    private void showOnceNotification(String title, String contentText, int id) {
        CustomNotificationManager.updateResult(getApplicationContext(), id, title, contentText, NASPref.getBackupLocation(getApplicationContext()));
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
                prepareBackupTask(path);
                break;
            default:
                break;
        }
    }

    private void prepareBackupTask(String path) {
        if (path == null || "".equals(path))
            return;

        File pictureFile = new File(path);
        if (pictureFile.exists() && canAddTaskToQueue()) {
            boolean addVideo = NASPref.getBackupVideo(getApplicationContext());
            FileInfo.TYPE type = pictureFile.isFile() ? FileInfo.getType(path) : FileInfo.TYPE.DIR;
            if (type == FileInfo.TYPE.PHOTO) {
                ArrayList<String> paths = new ArrayList<String>();
                paths.add(path);
                addBackupTaskToQueue(path, paths, 1);
            } else if (type == FileInfo.TYPE.VIDEO && addVideo) {
                ArrayList<String> paths = new ArrayList<String>();
                paths.add(path);
                addBackupTaskToQueue(path, paths, 1);
            } else {
                Log.d(TAG, "Ignore file name : " + path + ", file type : " + type.toString());
            }
        }
    }

    @Override
    public void onAutoBackupTaskStarted(AutoBackupTask task, int total) {
        //show backup start notification
        String value = getString(R.string.auto_backup) + " " + String.format("(%s/%s)" , 0, total);
        showProgressNotification(value, NOTIFICATION_ID, total, 0);
    }

    @Override
    public void onAutoBackupTaskPerFinished(AutoBackupTask task, int total, int progress) {
        //show backup success notification
        if(total > 1) {
            String value = getString(R.string.auto_backup) + " " + String.format("(%s/%s) ", progress, total);
            showProgressNotification(value, NOTIFICATION_ID, total, progress);
        }

        //add the finished backup task to database
        String path = task.getFilePaths().get(progress - 1);
        addBackupTaskToDatabase(path);
    }

    @Override
    public void onAutoBackupTaskFinished(AutoBackupTask task) {
        int size = task.getFilePaths().size();
        if (size > 1) {
            showOnceNotification(getString(R.string.backup_success), String.format("(%s/%s)" , size, size), NOTIFICATION_ID);
        } else {
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
        boolean enable = NASPref.getBackupSetting(getApplicationContext());
        if (!enable) {
            Log.d(TAG, "Can't add to queue due to service disable");
            return false;
        }

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
        addBackupTaskToQueue(null, paths, retry);
    }

    private void addBackupTaskToQueue(String checkPath, ArrayList<String> paths, int retry) {
        boolean pass = AutoBackupQueue.getInstance().addTaskCheck(checkPath);
        if(!pass) {
            Log.d(TAG, "task already in the queue");
            return;
        }

        String errorPath = NASPref.getBackupErrorTask(getApplicationContext());
        String des = NASPref.getBackupLocation(getApplicationContext());
        AutoBackupTask task = new AutoBackupTask(getApplicationContext(), paths, des, errorPath);
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
        String macAddress = "";
        Server server = ServerManager.INSTANCE.getCurrentServer();
        ServerInfo info = server.getServerInfo();
        if (info != null)
            macAddress = info.mac;
        if (macAddress == null || "".equals(macAddress)) {
            macAddress = NASPref.getMacAddress(getApplicationContext());
        }

        File file = new File(path);
        if (mHelper == null) {
            String backupSource = NASPref.getBackupSource(getApplicationContext());
            mHelper = new AutoBackupHelper(getApplicationContext(), backupSource);
        }
        mHelper.insertTask(file.getName(), file.getPath(), Long.toString(file.lastModified()), macAddress);
    }

    private Runnable runnable = new Runnable() {
        public void run() {
            AutoBackupQueue.getInstance().doUploadTask();
        }
    };
}
