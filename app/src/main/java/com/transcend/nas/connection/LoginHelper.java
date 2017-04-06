package com.transcend.nas.connection;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.transcend.nas.service.MyDBHelper;
import com.transcend.nas.service.MyDBManager;

/**
 * Created by ike_lee on 2016/9/5.
 */
public class LoginHelper {
    private static final String TAG = "LoginHelper";
    private Context mContext;

    public static class LoginInfo{
        public String email;
        public String hostname;
        public String username;
        public String password;
        public String ip;
        public String uuid;
        public String macAddress;

        public LoginInfo(){

        }
    }

    public LoginHelper(Context context) {
        mContext = context;
    }

    public void setAccount(LoginInfo item){
        boolean exist = existAccount(item);
        if(!exist) {
            ContentValues cv = new ContentValues();
            cv.put(MyDBHelper.TUTK_EMAIL, item.email);
            cv.put(MyDBHelper.TUTK_HOSTNAME, item.hostname);
            cv.put(MyDBHelper.TUTK_USERNAME, item.username);
            cv.put(MyDBHelper.TUTK_PASSWORD, item.password);
            cv.put(MyDBHelper.TUTK_IP, item.ip);
            cv.put(MyDBHelper.TUTK_UUID, item.uuid);
            cv.put(MyDBHelper.TUTK_MACADDRESS, item.macAddress);
            long id = MyDBManager.getInstance(mContext).insert(MyDBHelper.TABLE_TUTK, null, cv);
        } else {
            updateAccount(item);
        }
    }

    public boolean getAccount(LoginInfo item){
        boolean exist = false;
        String url = "select * from " + MyDBHelper.TABLE_TUTK + " WHERE " + MyDBHelper.TUTK_EMAIL + "='" + item.email + "'";
        Cursor c = null;
        if(item.macAddress != null && !item.macAddress.equals(""))
            url +=  " AND " + MyDBHelper.TUTK_MACADDRESS + "='" + item.macAddress + "'";
        else if(item.uuid != null && !item.uuid.equals(""))
            url +=  " AND " + MyDBHelper.TUTK_UUID + "='" + item.uuid + "'";
        else
            return false;

        try {
            c = MyDBManager.getInstance(mContext).rawQuery(url, null);
            if (c.moveToFirst()) {
                exist = true;
                item.ip = c.getString(c.getColumnIndex(MyDBHelper.TUTK_IP));
                item.username = c.getString(c.getColumnIndex(MyDBHelper.TUTK_USERNAME));
                item.password = c.getString(c.getColumnIndex(MyDBHelper.TUTK_PASSWORD));
            }
            c.close();
            c = null;
        } catch (IllegalStateException e){
            MyDBManager.getInstance(mContext).init(mContext);
        } finally {
            if(c != null)
                c.close();
        }
        return exist;
    }

    public void updateAccount(LoginInfo item){
        ContentValues cv = new ContentValues();
        cv.put(MyDBHelper.TUTK_USERNAME, item.username);
        cv.put(MyDBHelper.TUTK_PASSWORD, item.password);
        int count = MyDBManager.getInstance(mContext).update(MyDBHelper.TABLE_TUTK, cv, MyDBHelper.TUTK_EMAIL + "='" + item.email
                + "' AND " + MyDBHelper.TUTK_MACADDRESS + "='" + item.macAddress + "'", null);
    }

    public void deleteAccount(LoginInfo item){
        Log.d(TAG, item.email +"," + item.uuid);
        int count = MyDBManager.getInstance(mContext).delete(MyDBHelper.TABLE_TUTK, MyDBHelper.TUTK_EMAIL + "='" + item.email
                + "' AND " + MyDBHelper.TUTK_UUID + "='" + item.uuid + "'", null);
    }

    private boolean existAccount(LoginInfo item){
        boolean exist = false;
        String url;
        Cursor c = null;
        url = "select * from " + MyDBHelper.TABLE_TUTK + " WHERE " + MyDBHelper.TUTK_EMAIL + "='" + item.email
                + "' AND " + MyDBHelper.TUTK_MACADDRESS + "='" + item.macAddress + "'";
        try {
            c = MyDBManager.getInstance(mContext).rawQuery(url, null);
            exist = c.getCount() > 0;
            c.close();
            c = null;
        } catch (IllegalStateException e){
            MyDBManager.getInstance(mContext).init(mContext);
        } finally {
            if(c != null)
                c.close();
        }
        return exist;
    }

    public int getAccountID(LoginHelper.LoginInfo item) {
        int userID = -1;
        String url;
        Cursor c = null;
        url = "select * from " + MyDBHelper.TABLE_TUTK + " WHERE " + MyDBHelper.TUTK_EMAIL + "='" + item.email
                + "' AND " + MyDBHelper.TUTK_USERNAME + "='" + item.username + "'";
        if(item.macAddress != null && !item.macAddress.equals(""))
            url +=  " AND " + MyDBHelper.TUTK_MACADDRESS + "='" + item.macAddress + "'";
        if(item.uuid != null && !item.uuid.equals(""))
            url +=  " AND " + MyDBHelper.TUTK_UUID + "='" + item.uuid + "'";

        try {
            c = MyDBManager.getInstance(mContext).rawQuery(url, null);
            if (c.moveToFirst()) {
                userID = c.getInt(c.getColumnIndex(MyDBHelper.ID));
            }
            c.close();
            c = null;
        } catch (IllegalStateException e) {
            MyDBManager.getInstance(mContext).init(mContext);
        } finally {
            if (c != null)
                c.close();
        }
        return userID;
    }
}
