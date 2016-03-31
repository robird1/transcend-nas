package com.transcend.nas.service;

/**
 * Created by ike_lee on 2016/3/28.
 */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static android.provider.BaseColumns._ID;//這個是資料庫都會有個唯一的ID

public class MyDBHelper extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "backup";  //表格名稱
    public static final String NAME = "name";
    public static final String PATH = "path";
    public static final String LAST_MODIFY = "last_modify";
    public static final String DESTINATION = "destination";
    private final static String DATABASE_NAME = "transcend.db";  //資料庫名稱
    private final static int DATABASE_VERSION = 1;  //資料庫版本

    public MyDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String INIT_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                NAME + " TEXT NOT NULL, " + PATH + " TEXT NOT NULL, " + LAST_MODIFY + " TEXT NOT NULL, " + DESTINATION  + " TEXT NOT NULL);";
        db.execSQL(INIT_TABLE);

    }

    @Override
    //刪除table
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
        db.execSQL(DROP_TABLE);
        onCreate(db);
    }

}