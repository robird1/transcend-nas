package com.transcend.nas.service;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.realtek.nasfun.api.SambaStatus;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASPref;
import com.transcend.nas.management.FileInfo;
import com.tutk.IOTC.P2PService;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * Created by ike_lee on 2016/3/25.
 */
public class AutoBackupHelper {
    private static final String TAG = "AutoBackupService";
    private Context mContext;
    private String mPath;

    private MyDBHelper dbHelper;
    private SQLiteDatabase db;

    private Server mServer;
    private String mUsername;
    private String mPassword;
    private String mHostname;
    private String mTutkuuid;

    public AutoBackupHelper(Context context, String path) {
        mContext = context;
        mPath = path;
        dbHelper = new MyDBHelper(context);
        db = dbHelper.getWritableDatabase();
        updateServerInfo();
    }

    public void init() {
        try {
            createNewFolder();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (SmbException e) {
            e.printStackTrace();
        }
    }

    public void onDestroy(){
        db.close();
    }

    public void updateServerInfo(){
        mServer = ServerManager.INSTANCE.getCurrentServer();
        mUsername = mServer.getUsername();
        mPassword = mServer.getPassword();
        mHostname = mServer.getHostname();
        mTutkuuid = mServer.getTutkUUID();
        if(mTutkuuid == null){
            mTutkuuid = NASPref.getUUID(mContext);
        }
        String p2pIP = P2PService.getInstance().getP2PIP();
        if(mHostname.contains(p2pIP)){
            mHostname = p2pIP + ":" + P2PService.getInstance().getP2PPort(P2PService.P2PProtocalType.SMB);
        }
    }

    public ArrayList<String> getNeedUploadImageList(boolean filter) {
        ArrayList<String> list = searchAllImage(mPath);
        if(filter){
            return filterUploadImageList(list);
        }
        return list;
    }

    public ArrayList<String> filterUploadImageList(ArrayList<String> paths){
        ArrayList<String> fileList = new ArrayList<String>();
        for(String path : paths){
            if(!existTask(MyDBHelper.PATH, path))
                fileList.add(path);

        }
        return fileList;
    }

    private boolean checkSambaService() {
        SambaStatus status = mServer.getServiceStatus(Server.Service.SAMBA);
        return status.isRunning;
    }

    private boolean isValid(String str) {
        return (str != null) && (!str.isEmpty());
    }

    private String getSmbUrl(String path) {
        StringBuilder builder = new StringBuilder();
        builder.append("smb://");
        if (isValid(mUsername) && isValid(mPassword)) {
            builder.append(mUsername);
            builder.append(":");
            builder.append(mPassword);
            builder.append("@");
        }
        builder.append(mHostname);
        if (isValid(path))
            builder.append(path);
        return builder.toString();
    }

    private String format(String path) {
        StringBuilder builder = new StringBuilder();
        if (!path.startsWith("/"))
            builder.append("/");
        builder.append(path);
        if (!path.endsWith("/"))
            builder.append("/");
        return builder.toString();
    }

    private boolean createNewFolder() throws MalformedURLException, SmbException {
        String path = NASPref.getBackupLocation(mContext);
        String url = getSmbUrl(path);
        SmbFile target = new SmbFile(url);
        if (!target.exists()) {
            target.mkdirs();
            return true;
        }
        return false;
    }

    private ArrayList<String> searchAllImage(String path) {
        if (path == null)
            return null;
        File dir = new File(path);
        if (!dir.exists())
            return null;

        ArrayList<String> fileList = new ArrayList<String>();

        File files[] = dir.listFiles();
        for (File file : files) {
            if (file.isHidden())
                continue;
            FileInfo fileInfo = new FileInfo();
            fileInfo.path = file.getPath();
            fileInfo.name = file.getName();
            fileInfo.time = FileInfo.getTime(file.lastModified());
            fileInfo.type = file.isFile() ? FileInfo.getType(file.getPath()) : FileInfo.TYPE.DIR;
            fileInfo.size = file.length();
            if (fileInfo.type == FileInfo.TYPE.DIR) {
                List<String> list = searchAllImage(fileInfo.path);
                for (String item : list) {
                    fileList.add(item);
                }
            } else
                fileList.add(fileInfo.path);
        }
        //Collections.sort(fileList, FileInfoSort.comparator(mContext));
        return fileList;
    }

    public void insertTask(String name, String path, String modify, String destination){
        ContentValues cv = new ContentValues();
        cv.put(MyDBHelper.NAME, name);
        cv.put(MyDBHelper.PATH, path);
        cv.put(MyDBHelper.LAST_MODIFY, modify);
        cv.put(MyDBHelper.DESTINATION, destination);
        long id = db.insert(MyDBHelper.TABLE_NAME, null, cv);
    }

    public void UpdateTask(String name,String key, String value){
        ContentValues cv = new ContentValues();
        cv.put(key, value);
        int count = db.update(MyDBHelper.TABLE_NAME, cv, MyDBHelper.NAME + "=" + name, null);
    }

    public void DeleteTask(String name){
        int count = db.delete(MyDBHelper.TABLE_NAME, MyDBHelper.NAME + "=" + name, null);
    }

    public boolean existTask(String key, String value){
        boolean exist = false;
        String url;
        Cursor c = null;
        url = "select * from " + MyDBHelper.TABLE_NAME + " WHERE " + key + "='" + value + "' AND " + MyDBHelper.DESTINATION + "='" + mTutkuuid + "'";
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
