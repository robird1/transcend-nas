package com.transcend.nas.utils;

import android.util.Log;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASApp;
import com.transcend.nas.management.FileInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ike_lee on 2016/5/23.
 */
public class FileFactory {

    private static final String TAG = FileFactory.class.getSimpleName();
    private static FileFactory mFileFactory;
    private static final Object mMute = new Object();
    private List<String> mNotificationList;

    public FileFactory() {
        mNotificationList = new ArrayList<String>();
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
            if(i == 0) {
                for (FileInfo file : tmp) {
                    if (file.type == FileInfo.TYPE.DIR) {
                        fileList.add(file);
                    }
                }
            }

            if(i == 1) {
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
            else if (path.startsWith("/Public/"))
                filepath = Server.ADMIN_DAV_HOME + path.replaceFirst("/Public/", "/public/");
            else
                filepath = "/dav/devices/home" + path;

            if (thumbnail)
                url = "http://" + hostname + filepath + "?session=" + hash + "&thumbnail";
            else
                url = "http://" + hostname + filepath + "?session=" + hash + "&webview";
        }
        return url;
    }

    public int getNotificationID() {
        int id = 1;
        if (mNotificationList.size() > 0) {
            String value = mNotificationList.get(mNotificationList.size() - 1);
            id = Integer.parseInt(value) + 1;
            mNotificationList.add(Integer.toString(id));
        }
        else{
            mNotificationList.add(Integer.toString(id));
        }
        return id;
    }

    public void releaseNotificationID(int id) {
        String value = "" + id;
        mNotificationList.remove(value);
    }
}
