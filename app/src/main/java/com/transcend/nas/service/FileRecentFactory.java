package com.transcend.nas.service;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.transcend.nas.NASPref;
import com.transcend.nas.connection.LoginHelper;
import com.transcend.nas.management.FileInfo;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * Created by ike_lee on 2017/3/22.
 */
public class FileRecentFactory {
    private static final String TAG = FileRecentFactory.class.getSimpleName();

    public static FileRecentInfo create(Context context, SmbFile file, FileRecentInfo.ActionType actionType) {
        try {
            FileInfo info = new FileInfo();
            info.path = file.getPath(); //TextUtils.concat(mPath, file.getName()).toString();
            info.name = file.getName().replace("/", "");
            info.time = FileInfo.getTime(file.getLastModified());
            info.type = file.isFile() ? FileInfo.getType(file.getPath()) : FileInfo.TYPE.DIR;
            info.size = file.length();
            return create(context, info, actionType, getCurrentTime());
        } catch (SmbException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static FileRecentInfo create(Context context, FileInfo info, FileRecentInfo.ActionType actionType) {
        return create(context, info, actionType, getCurrentTime());
    }

    public static FileRecentInfo create(Context context, FileInfo info, FileRecentInfo.ActionType actionType, String actionTime) {
        return create(context, getCurrentUserID(context), info, actionType, actionTime);
    }

    public static FileRecentInfo create(Context context, LoginHelper.LoginInfo user, FileInfo info, FileRecentInfo.ActionType actionType) {
        return create(context, user, info, actionType, getCurrentTime());
    }

    public static FileRecentInfo create(Context context, LoginHelper.LoginInfo user, FileInfo info, FileRecentInfo.ActionType actionType, String actionTime) {
        return create(context, getUserID(context, user), info, actionType, actionTime);
    }

    public static FileRecentInfo create(Context context, int userID, FileInfo info, FileRecentInfo.ActionType actionType, String actionTime) {
        if (info == null)
            return null;

        FileRecentInfo recent = new FileRecentInfo();
        recent.id = userID;
        recent.info = info;
        recent.actionType = actionType;
        recent.actionTime = actionTime;
        return recent;
    }

    public static int getCurrentUserID(Context context) {
        LoginHelper.LoginInfo user = new LoginHelper.LoginInfo();
        user.email = NASPref.getCloudUsername(context);
        user.username = NASPref.getUsername(context);
        user.macAddress = NASPref.getMacAddress(context);
        user.uuid = NASPref.getCloudUUID(context);
        return getUserID(context, user);
    }

    public static int getUserID(Context context, LoginHelper.LoginInfo user) {
        LoginHelper loginHelper = new LoginHelper(context);
        return loginHelper.getAccountID(user);
    }

    private static String getCurrentTime() {
        return "" + System.currentTimeMillis();
    }
}
