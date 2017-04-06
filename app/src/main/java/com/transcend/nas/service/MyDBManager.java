package com.transcend.nas.service;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.transcend.nas.NASApp;

/**
 * Created by ike_lee on 2016/9/5.
 */
public class MyDBManager {
    private static final String TAG = MyDBManager.class.getSimpleName();
    private static MyDBManager mDBManager;
    private static final Object mMute = new Object();

    private MyDBHelper dbHelper;
    private SQLiteDatabase db;

    public MyDBManager(Context context) {
        init(context);
    }

    public static MyDBManager getInstance(Context context) {
        synchronized (mMute) {
            if (mDBManager == null)
                mDBManager = new MyDBManager(context);
        }
        return mDBManager;
    }

    public void init(Context context){
        dbHelper = new MyDBHelper(context);
        db = dbHelper.getWritableDatabase();
    }

    public void onDestroy(){
        if(db != null)
            db.close();
    }

    public long insert(String table, String nullColumnHack, ContentValues values) {
        Log.d(TAG, "insert : " + values.toString());
        return db.insert(table, nullColumnHack, values);
    }

    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        Log.d(TAG, "update : " + values.toString());
        return db.update(table, values, whereClause, whereArgs);
    }

    public int delete(String table, String whereClause, String[] whereArgs) {
        Log.d(TAG, "delete : " + whereClause);
        return db.delete(table, whereClause, whereArgs);
    }

    public Cursor rawQuery(String sql, String[] selectionArgs){
        Log.d(TAG, "sql : " + sql);
        return db.rawQuery(sql, selectionArgs);
    }
}
