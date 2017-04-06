package com.transcend.nas.service;

/**
 * Created by ike_lee on 2016/3/28.
 */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static android.provider.BaseColumns._ID;//這個是資料庫都會有個唯一的ID

public class MyDBHelper extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "backup";  //表格名稱
    public static final String NAME = "name";
    public static final String PATH = "path";
    public static final String REAL_PATH = "real_path";
    public static final String TYPE = "type";
    public static final String SIZE = "size";
    public static final String LAST_MODIFY = "last_modify";
    public static final String DESTINATION = "destination";

    public static final String TABLE_TUTK = "tutk";  //表格名稱
    public static final String TUTK_EMAIL = "tutk_email";
    public static final String TUTK_HOSTNAME = "tutk_hostname";
    public static final String TUTK_USERNAME = "tutk_username";
    public static final String TUTK_PASSWORD = "tutk_password";
    public static final String TUTK_IP = "tutk_ip";
    public static final String TUTK_UUID = "tutk_uuid";
    public static final String TUTK_MACADDRESS = "tutk_macaddress";

    public static final String TABLE_RECENT = "recent"; //表格名稱
    public static final String RECENT_USER = "user";
    public static final String RECENT_ACTION = "action";
    public static final String RECENT_ACTION_TIME = "time";

    public static final String ID = _ID;

    private final static String DATABASE_NAME = "transcend.db";  //資料庫名稱
    private final static int DATABASE_VERSION = 3;  //資料庫版本

    public MyDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String INIT_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + NAME + " TEXT NOT NULL, " + PATH + " TEXT NOT NULL, " + LAST_MODIFY + " TEXT NOT NULL, " + DESTINATION  + " TEXT NOT NULL);";
        db.execSQL(INIT_TABLE);

        final String INIT_TUTK_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_TUTK + " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + TUTK_EMAIL + " TEXT NOT NULL, " + TUTK_HOSTNAME + " TEXT NOT NULL, " + TUTK_USERNAME + " TEXT NOT NULL, "
                + TUTK_PASSWORD + " TEXT NOT NULL, " + TUTK_IP + " TEXT NOT NULL, "+ TUTK_UUID + " TEXT NOT NULL, "
                + TUTK_MACADDRESS + " TEXT NOT NULL);";
        db.execSQL(INIT_TUTK_TABLE);

        final String INIT_RECENT_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_RECENT + " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + RECENT_USER + " INTEGER, "
                + NAME + " TEXT NOT NULL, " + PATH + " TEXT NOT NULL, " + REAL_PATH + " TEXT NOT NULL, "
                + SIZE + " TEXT NOT NULL, " + TYPE + " INTEGER, " + LAST_MODIFY + " TEXT NOT NULL, "
                + RECENT_ACTION  + " INTEGER, "+ RECENT_ACTION_TIME  + " TEXT NOT NULL);";
        db.execSQL(INIT_RECENT_TABLE);
    }

    @Override
    //刪除table
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
        //db.execSQL(DROP_TABLE);
        onCreate(db);
    }

}