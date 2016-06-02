package com.transcend.nas.utils;

import android.util.Log;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASApp;
import com.transcend.nas.management.FileInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by ike_lee on 2016/5/23.
 */
public class FileFactory {

    private static final String TAG = FileFactory.class.getSimpleName();
    private static FileFactory mFileFactory;
    private static final Object mMute = new Object();
    private List<String> mNotificationList;
    private Map<String, String> mRealPathMap;
    private int RealPathMapLifeCycle = 10;

    public FileFactory() {
        mNotificationList = new ArrayList<String>();
        mRealPathMap = new HashMap<String, String>();
    }

    public static FileFactory getInstance() {
        synchronized (mMute) {
            if (mFileFactory == null)
                mFileFactory = new FileFactory();
        }
        return mFileFactory;
    }

    public void addFileTypeSortRule(ArrayList<FileInfo> fileList) {
        ArrayList<FileInfo> tmp = new ArrayList<FileInfo>();
        for (FileInfo file : fileList) {
            tmp.add(file);
        }
        fileList.clear();
        for (int i = 0; i < 2; i++) {
            if (i == 0) {
                for (FileInfo file : tmp) {
                    if (file.type == FileInfo.TYPE.DIR) {
                        fileList.add(file);
                    }
                }
            }

            if (i == 1) {
                for (FileInfo file : tmp) {
                    if (file.type != FileInfo.TYPE.DIR) {
                        fileList.add(file);
                    }
                }
            }
        }

    }

    public void addFolderFilterRule(String path, ArrayList<FileInfo> fileList) {
        if (NASApp.ROOT_SMB.equals(path)) {
            for (FileInfo file : fileList) {
                if (file.name.equals("homes")) {
                    fileList.remove(file);
                    break;
                }
            }

            for (FileInfo file : fileList) {
                if (file.name.equals("Public")) {
                    fileList.remove(file);
                    fileList.add(0, file);
                    break;
                }
            }

            for (FileInfo file : fileList) {
                if (file.time.startsWith("1970/01/01")) {
                    file.time = "";
                }
            }
        }
    }

    public void addSelfFilterRule(String path, ArrayList<FileInfo> fileList) {
        if (NASApp.ROOT_SMB.equals(path)) {
            for (FileInfo file : fileList) {
                if (file.path.endsWith(path)) {
                    fileList.remove(file);
                    break;
                }
            }
        }
    }

    public String getPhotoPath(boolean thumbnail, String path) {
        String url;
        if (path.startsWith(NASApp.ROOT_STG)) {
            url = "file://" + path;
        } else {
            Server server = ServerManager.INSTANCE.getCurrentServer();
            String hostname = server.getHostname();
            String username = server.getUsername();
            String hash = server.getHash();
            String filepath;
            if (path.startsWith(Server.HOME))
                filepath = Server.USER_DAV_HOME + path.replaceFirst(Server.HOME, "/");
            else if (path.startsWith("/" + username + "/"))
                filepath = Server.USER_DAV_HOME + path.replaceFirst("/" + username + "/", "/");
            else {
                if(username.equals("admin")) {
                    for (String key : mRealPathMap.keySet()) {
                        if (path.startsWith(key)) {
                            path = path.replaceFirst(key, mRealPathMap.get(key));
                            break;
                        }
                    }
                    filepath = Server.DEVICE_DAV_HOME + path.replaceFirst("/home/", "/");
                }
                else{
                    String newPath = "";
                    String[] paths = path.replaceFirst("/", "").split("/");
                    int length = paths.length;
                    for(int i = 0; i< length; i++){
                        if(i==0)
                            newPath = "/" + paths[i].toLowerCase();
                        else
                            newPath = newPath + "/" + paths[i];
                    }
                    filepath = "/dav" + newPath;
                }
            }

            if (thumbnail)
                url = "http://" + hostname + filepath + "?session=" + hash + "&thumbnail";
            else
                url = "http://" + hostname + filepath + "?session=" + hash + "&webview";
            Log.d(TAG, url);
        }
        return url;
    }

    public int getNotificationID() {
        int id = 1;
        if (mNotificationList.size() > 0) {
            String value = mNotificationList.get(mNotificationList.size() - 1);
            id = Integer.parseInt(value) + 1;
            mNotificationList.add(Integer.toString(id));
        } else {
            mNotificationList.add(Integer.toString(id));
        }
        return id;
    }

    public void releaseNotificationID(int id) {
        String value = "" + id;
        mNotificationList.remove(value);
    }

    public void addRealPathToMap(String path, String realPath) {
        if (mRealPathMap == null)
            mRealPathMap = new HashMap<String, String>();
        if (!path.startsWith("/"))
            path = "/" + path + "/";
        if(!realPath.endsWith("/"))
            realPath = realPath + "/";
        mRealPathMap.put(path, realPath);
    }

    public int getRealPathMapSize(){
        int size = 0;
        if(mRealPathMap != null)
            size = mRealPathMap.size();
        return size;
    }

    public String getRealPathKeyFromMap(String path) {
        String result = "";
        if (mRealPathMap != null) {
            for (String key : mRealPathMap.keySet()) {
                if (path.startsWith(key)) {
                    result = key;
                    break;
                }
            }
        }
        return result;
    }

    public String getRealPathFromMap(String path) {
        String realPath = "";
        if (mRealPathMap != null) {
            for (String key : mRealPathMap.keySet()) {
                if (path.startsWith(key)) {
                    realPath = mRealPathMap.get(key);
                    break;
                }
            }
        }
        return realPath;
    }

    public boolean checkRealPathMapLifeCycle(){
        RealPathMapLifeCycle--;
        if(RealPathMapLifeCycle > 0)
            return true;
        else{
            RealPathMapLifeCycle = 10;
            return false;
        }
    }

    public void cleanRealPathMap() {
        if (mRealPathMap != null)
            mRealPathMap.clear();
    }
}
