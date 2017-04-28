package com.transcend.nas.service;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.transcend.nas.NASApp;
import com.transcend.nas.connection.LoginHelper;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.firmware.ShareFolderManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;

/**
 * Created by ike_lee on 2016/3/21.
 */
public class FileRecentManager {
    private static final String TAG = FileRecentManager.class.getSimpleName();
    private static FileRecentManager mFileRecentManager;
    private static final Object mMute = new Object();
    private Context mContext;
    private int mActionTypeCount;
    private int mFileTypeCount;

    public static FileRecentManager getInstance() {
        synchronized (mMute) {
            if (mFileRecentManager == null)
                mFileRecentManager = new FileRecentManager();
        }
        return mFileRecentManager;
    }

    public FileRecentManager() {
        mContext = NASApp.getContext();
        mActionTypeCount = FileRecentInfo.ActionType.values().length;
        mFileTypeCount = FileInfo.TYPE.values().length;
    }

    public void setAction(FileRecentInfo item) {
        setAction(null, item);
    }

    public void setAction(FileRecentInfo oldItem, FileRecentInfo newItem) {
        if (newItem == null) {
            Log.d(TAG, "Action don't have necessary information");
            return;
        }

        //if(oldItem != null)
        //    Log.d(TAG, "setAction org : " + oldItem.toString());
        //if(newItem != null)
        //    Log.d(TAG, "setAction new : " + newItem.toString());

        FileRecentInfo item = oldItem != null ? oldItem : newItem;

        boolean exist = existAction(item);
        if (!exist) {
            ContentValues cv = new ContentValues();
            cv.put(MyDBHelper.RECENT_USER, newItem.id);
            cv.put(MyDBHelper.NAME, newItem.info.name);
            cv.put(MyDBHelper.PATH, newItem.info.path);
            cv.put(MyDBHelper.TYPE, newItem.info.type.ordinal());
            cv.put(MyDBHelper.REAL_PATH, ShareFolderManager.getInstance().getRealPath(newItem.info.path));
            cv.put(MyDBHelper.SIZE, newItem.info.size);
            cv.put(MyDBHelper.LAST_MODIFY, newItem.info.time);
            cv.put(MyDBHelper.RECENT_ACTION, newItem.actionType.ordinal());
            cv.put(MyDBHelper.RECENT_ACTION_TIME, doGenerateTime(newItem.actionTime));
            long count = MyDBManager.getInstance(mContext).insert(MyDBHelper.TABLE_RECENT, null, cv);
        } else {
            if (oldItem != null)
                reviseAction(oldItem, newItem);
            else
                updateAction(newItem);
        }
    }

    public ArrayList<FileRecentInfo> getAction(int userId, String path, int size) {
        if (userId < 0)
            return null;

        String folderUrl = "";
        if(path != null && "".equals(path))
            folderUrl = "' AND" + MyDBHelper.PATH + " LIKE " + path + "%";

        LinkedHashMap<String, String> days = doGenerateSearchFileMap();
        ArrayList<FileRecentInfo> result = new ArrayList<>();
        Cursor c = null;
        try {
            for (String key : days.keySet()) {
                String url = "select * from " + MyDBHelper.TABLE_RECENT + " WHERE " + MyDBHelper.RECENT_USER + "='" + userId
                        + folderUrl
                        + "' AND " + MyDBHelper.RECENT_ACTION_TIME + key
                        + " ORDER by " + MyDBHelper.RECENT_ACTION_TIME + " DESC" + (size >= 0 ? " LIMIT " + size : "");
                c = MyDBManager.getInstance(mContext).rawQuery(url, null);
                if (c.moveToFirst()) {
                    do {
                        FileRecentInfo action = new FileRecentInfo();
                        FileInfo info = new FileInfo();
                        info.name = c.getString(c.getColumnIndex(MyDBHelper.NAME));
                        info.path = c.getString(c.getColumnIndex(MyDBHelper.PATH));
                        info.time = c.getString(c.getColumnIndex(MyDBHelper.LAST_MODIFY));
                        info.size = c.getLong(c.getColumnIndex(MyDBHelper.SIZE));
                        int typeIndex = c.getInt(c.getColumnIndex(MyDBHelper.TYPE));
                        if (mFileTypeCount > typeIndex && typeIndex >= 0)
                            info.type = FileInfo.TYPE.values()[typeIndex];

                        action.id = userId;
                        action.info = info;
                        action.realPath = c.getString(c.getColumnIndex(MyDBHelper.REAL_PATH));
                        action.actionTime = c.getString(c.getColumnIndex(MyDBHelper.RECENT_ACTION_TIME));
                        int actionIndex = c.getInt(c.getColumnIndex(MyDBHelper.RECENT_ACTION));
                        if (mActionTypeCount > actionIndex && actionIndex >= 0)
                            action.actionType = FileRecentInfo.ActionType.values()[actionIndex];
                        result.add(action);
                    } while (c.moveToNext());
                }
            }
            c.close();
            c = null;
        } catch (IllegalStateException e) {
            MyDBManager.getInstance(mContext).init(mContext);
        } finally {
            if (c != null)
                c.close();
        }
        return result;
    }

    public ArrayList<FileRecentInfo> getAction(LoginHelper.LoginInfo user, String path, int size) {
        return getAction(FileRecentFactory.getUserID(mContext, user), path, size);
    }

    public ArrayList<FileRecentInfo> getAction(LoginHelper.LoginInfo user, int size) {
        return getAction(user, null, size);
    }

    public ArrayList<FileRecentInfo> getAction(String path, int size) {
        return getAction(FileRecentFactory.getCurrentUserID(mContext), path, size);
    }

    public ArrayList<FileRecentInfo> getAction(int size) {
        return getAction(FileRecentFactory.getCurrentUserID(mContext), null, size);
    }

    public void reviseAction(FileRecentInfo oldItem, FileRecentInfo newItem) {
        ContentValues cv = new ContentValues();
        cv.put(MyDBHelper.NAME, newItem.info.name);
        cv.put(MyDBHelper.PATH, newItem.info.path);
        cv.put(MyDBHelper.REAL_PATH, ShareFolderManager.getInstance().getRealPath(newItem.info.path));
        cv.put(MyDBHelper.LAST_MODIFY, newItem.info.time);
        cv.put(MyDBHelper.RECENT_ACTION, newItem.actionType.ordinal());
        cv.put(MyDBHelper.RECENT_ACTION_TIME, doGenerateTime(newItem.actionTime));
        String url = doGenerateSearchFileUrl(oldItem);
        int count = MyDBManager.getInstance(mContext).update(MyDBHelper.TABLE_RECENT, cv, url, null);
    }

    public void updateAction(FileRecentInfo item) {
        ContentValues cv = new ContentValues();
        cv.put(MyDBHelper.RECENT_ACTION, item.actionType.ordinal());
        cv.put(MyDBHelper.RECENT_ACTION_TIME, doGenerateTime(item.actionTime));
        String url = doGenerateSearchFileUrl(item);
        int count = MyDBManager.getInstance(mContext).update(MyDBHelper.TABLE_RECENT, cv, url, null);
    }

    public void deleteAction(FileRecentInfo item) {
        String url = doGenerateSearchFileUrl(item);
        int count = MyDBManager.getInstance(mContext).delete(MyDBHelper.TABLE_RECENT, url, null);
    }

    public void deleteAction(FileInfo item) {
        deleteAction(FileRecentFactory.create(mContext, item, null));
    }

    public void deleteAction(int userId) {
        String url = MyDBHelper.RECENT_USER + "='" + userId + "'";
        int count = MyDBManager.getInstance(mContext).delete(MyDBHelper.TABLE_RECENT, url, null);
    }

    public void deleteAction() {
        deleteAction(FileRecentFactory.getCurrentUserID(mContext));
    }

    private boolean existAction(FileRecentInfo item) {
        boolean exist = false;
        String url;
        Cursor c = null;
        url = "select * from " + MyDBHelper.TABLE_RECENT + " WHERE " + doGenerateSearchFileUrl(item);
        try {
            c = MyDBManager.getInstance(mContext).rawQuery(url, null);
            if (c.moveToFirst()) {
                exist = true;
            }
            c.close();
            c = null;
        } catch (IllegalStateException e) {
            MyDBManager.getInstance(mContext).init(mContext);
        } finally {
            if (c != null)
                c.close();
        }
        return exist;
    }

    private String doGenerateTime(String time) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(new Date(Long.parseLong(time)));
    }

    private String doGenerateSearchFileUrl(FileRecentInfo item) {
        String url = MyDBHelper.RECENT_USER + "='" + item.id
                + "' AND " + MyDBHelper.NAME + "='" + item.info.name.replace("'", "''")
                + "' AND " + MyDBHelper.PATH + "='" + item.info.path.replace("'", "''")
                + "' AND " + MyDBHelper.SIZE + "='" + item.info.size
                //+ "' AND " + MyDBHelper.REAL_PATH + "='" + item.realPath
                //+ "' AND " + MyDBHelper.LAST_MODIFY + "='" + item.info.time
                + "'";
        return url;
    }

    private LinkedHashMap<String, String> doGenerateSearchFileMap() {
        LinkedHashMap<String, String> days = new LinkedHashMap<>();

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        //int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        //int year = cal.get(Calendar.YEAR);
        cal.add(Calendar.DATE, 1);
        String tomorrow = dateFormat.format(cal.getTime());
        days.put(" < date('" + tomorrow + "')", "all");

        /*cal.add(Calendar.DATE, -7);
        String lastSevenDay = dateFormat.format(cal.getTime());
        //get last seven day action
        days.put(" BETWEEN " + "date('" + lastSevenDay + "') AND date('" + today + "')", "week");

        String firstDayOfYear = year + "-01-01";
        int compare = lastSevenDay.compareTo(firstDayOfYear);
        if (compare == 0) {
            days.put(" LIKE '" + firstDayOfYear + "%'", "year");
            days.put(" < date('" + firstDayOfYear + "')", "other");
        } else if (compare > 0) {
            days.put(" BETWEEN " + "date('" + firstDayOfYear + "') AND date('" + lastSevenDay + "')", "year");
            days.put(" < date('" + firstDayOfYear + "')", "other");
        } else {
            days.put("ignore", "");
            days.put(" <= date('" + lastSevenDay + "')", "other");
        }*/

        return days;
    }

}
