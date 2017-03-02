package com.transcend.nas.common;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.transcend.nas.R;
import com.transcend.nas.management.FileManageActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ike_lee on 2016/5/23.
 */
public class CustomNotificationManager {

    private static final String TAG = CustomNotificationManager.class.getSimpleName();
    private static final boolean enableDeleteNotification = true;
    private static CustomNotificationManager mCustomNotificationManager;
    private static final Object mMute = new Object();
    private List<String> mNotificationList;
    private Map<String, AsyncTaskLoader<Boolean>> mTaskList;

    public CustomNotificationManager() {
        mNotificationList = new ArrayList<String>();
        mTaskList = new HashMap<>();
    }

    public static CustomNotificationManager getInstance() {
        synchronized (mMute) {
            if (mCustomNotificationManager == null)
                mCustomNotificationManager = new CustomNotificationManager();
        }
        return mCustomNotificationManager;
    }

    public int queryNotificationID(AsyncTaskLoader loader) {
        int id = 1;
        if (mNotificationList.size() > 0) {
            String value = mNotificationList.get(mNotificationList.size() - 1);
            id = Integer.parseInt(value) + 1;
        }

        mNotificationList.add(Integer.toString(id));
        mTaskList.put(Integer.toString(id), loader);
        return id;
    }

    public void releaseNotificationID(int id) {
        String value = "" + id;
        mNotificationList.remove(value);
        mTaskList.remove(value);
        Log.d(TAG, "notification size : " + mNotificationList.size() + ", task size : " + mTaskList.size());
    }

    public void cancelTask(int id) {
        String value = "" + id;
        AsyncTaskLoader<Boolean> loader = mTaskList.get(value);
        if (loader != null) {
            loader.cancelLoad();
        }

        releaseNotificationID(id);
    }

    public void cancelAllTask() {
        for (String id : mTaskList.keySet()) {
            cancelTask(Integer.parseInt(id));
        }

        mNotificationList.clear();
        mTaskList.clear();
    }

    public static NotificationCompat.Builder createProgressBuilder(Context context, Activity activity, int notificationID) {
        //create builder
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mBuilder.setAutoCancel(true);

        //add content intent
        Intent intent = activity.getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent);

        //add delete intent
        if(enableDeleteNotification) {
            Intent delete = new Intent(context, CustomNotificationReceiver.class);
            delete.setAction(CustomNotificationReceiver.NOTIFICATION_CANCEL);
            delete.putExtra(CustomNotificationReceiver.NOTIFICATION_KEY, notificationID);
            PendingIntent deleteIntent = PendingIntent.getBroadcast(context, 0, delete, PendingIntent.FLAG_CANCEL_CURRENT);
            mBuilder.setDeleteIntent(deleteIntent);
        }

        return mBuilder;
    }

    public static void updateResult(Context context, int notificationID, String type, String result, String destination) {
        Log.w(TAG, "result: " + result);

        int icon = R.mipmap.ic_launcher;
        String name = context.getResources().getString(R.string.app_name);
        String text = String.format("%s - %s", type, result);

        NotificationManager ntfMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent();
        intent.setClass(context, FileManageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (destination != null && !destination.equals(""))
            intent.putExtra("path", destination);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(icon);
        builder.setContentTitle(name);
        builder.setContentText(text);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(true);
        ntfMgr.notify(notificationID, builder.build());
        CustomNotificationManager.getInstance().releaseNotificationID(notificationID);
    }
}
