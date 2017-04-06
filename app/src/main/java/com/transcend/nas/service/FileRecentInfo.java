package com.transcend.nas.service;

import com.transcend.nas.management.FileInfo;

/**
 * Created by ike_lee on 2017/3/22.
 */
public class FileRecentInfo {
    private static final String TAG = FileRecentInfo.class.getSimpleName();

    public enum ActionType {
        OPEN, REVISE, UPLOAD
    }

    public int id;
    public FileInfo info;
    public ActionType actionType;
    public String actionTime;
    public String realPath;

    @Override
    public String toString() {
        String result = MyDBHelper.RECENT_USER + " " + id + ", "
                + MyDBHelper.REAL_PATH + " " + realPath + ", "
                + MyDBHelper.RECENT_ACTION + " " + actionType + ", "
                + MyDBHelper.RECENT_ACTION_TIME + " " + actionTime;

        if (info != null) {
            result += ", info:[ " + MyDBHelper.NAME + " " + info.name + ", "
                    + MyDBHelper.PATH + " " + info.path + ", "
                    + MyDBHelper.LAST_MODIFY + " " + info.time + ", "
                    + MyDBHelper.SIZE + " " + info.size + ", "
                    + MyDBHelper.TYPE + " " + info.type + " ]";
        }
        return result;
    }
}
