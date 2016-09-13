package com.transcend.nas.connection_new;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Log;

import com.realtek.nasfun.api.SambaStatus;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerInfo;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASPref;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.service.MyDBHelper;
import com.tutk.IOTC.P2PService;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * Created by ike_lee on 2016/9/5.
 */
public class LoginHelper {
    private static final String TAG = "LoginHelper";
    private Context mContext;

    private MyDBHelper dbHelper;
    private SQLiteDatabase db;

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
        dbHelper = new MyDBHelper(context);
        db = dbHelper.getWritableDatabase();
    }

    public void onDestroy(){
        db.close();
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
            long id = db.insert(MyDBHelper.TABLE_TUTK, null, cv);
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
            c = db.rawQuery(url, null);
            if (c.moveToFirst()) {
                exist = true;
                item.ip = c.getString(c.getColumnIndex(MyDBHelper.TUTK_IP));
                item.username = c.getString(c.getColumnIndex(MyDBHelper.TUTK_USERNAME));
                item.password = c.getString(c.getColumnIndex(MyDBHelper.TUTK_PASSWORD));
            }
            c.close();
            c = null;
        } catch (IllegalStateException e){
            dbHelper = new MyDBHelper(mContext);
            db = dbHelper.getWritableDatabase();
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
        int count = db.update(MyDBHelper.TABLE_TUTK, cv, MyDBHelper.TUTK_EMAIL + "='" + item.email
                + "' AND " + MyDBHelper.TUTK_MACADDRESS + "='" + item.macAddress + "'", null);
    }

    public void deleteAccount(LoginInfo item){
        Log.d(TAG, item.email +"," + item.uuid);
        int count = db.delete(MyDBHelper.TABLE_TUTK, MyDBHelper.TUTK_EMAIL + "='" + item.email
                + "' AND " + MyDBHelper.TUTK_UUID + "='" + item.uuid + "'", null);
    }

    private boolean existAccount(LoginInfo item){
        boolean exist = false;
        String url;
        Cursor c = null;
        url = "select * from " + MyDBHelper.TABLE_TUTK + " WHERE " + MyDBHelper.TUTK_EMAIL + "='" + item.email
                + "' AND " + MyDBHelper.TUTK_MACADDRESS + "='" + item.macAddress + "'";
        try {
            c = db.rawQuery(url, null);
            exist = c.getCount() > 0;
            c.close();
            c = null;
        } catch (IllegalStateException e){
            dbHelper = new MyDBHelper(mContext);
            db = dbHelper.getWritableDatabase();
        } finally {
            if(c != null)
                c.close();
        }
        return exist;
    }
}
