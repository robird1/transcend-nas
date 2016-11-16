package com.transcend.nas.common;

import android.util.Log;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASApp;
import com.transcend.nas.firmware_api.ShareFolderManager;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.service.TwonkyManager;
import com.tutk.IOTC.P2PService;

import java.io.File;
import java.text.DecimalFormat;
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
    private ArrayList<FileInfo> mFileList;

    public FileFactory() {
        mNotificationList = new ArrayList<String>();
        mFileList = new ArrayList<FileInfo>();
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

        for (FileInfo file : tmp) {
            if (file.type == FileInfo.TYPE.DIR) {
                fileList.add(file);
            }
        }

        for (FileInfo file : tmp) {
            if (file.type != FileInfo.TYPE.DIR) {
                fileList.add(file);
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

            Server server = ServerManager.INSTANCE.getCurrentServer();
            String username = server.getUsername();
            if (username != null && !username.equals("admin")) {
                ArrayList<FileInfo> tmp = new ArrayList<>();
                for (FileInfo file : fileList) {
                    if (file.name.equals("Public") || file.name.equals(username))
                        tmp.add(file);
                }

                fileList.clear();
                for (FileInfo file : tmp) {
                    fileList.add(file);
                }
            }

            for (FileInfo file : fileList) {
                if (file.time.startsWith("1970/01/01")) {
                    file.time = "";
                }
            }
        }
    }

    public String getPhotoPath(boolean thumbnail, String path) {
        String url = "";
        if (path.startsWith(NASApp.ROOT_STG)) {
            url = "file://" + path;
        } else {
            //First, try twonky image, and try webdav image when twonky image empty
            url = TwonkyManager.getInstance().getUrlFromPath(thumbnail, path);
            if(null == url || "".equals(url)) {
                Server server = ServerManager.INSTANCE.getCurrentServer();
                String hostname = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
                String username = server.getUsername();
                String hash = server.getHash();
                String filepath;
                if (path.startsWith(Server.HOME))
                    filepath = Server.USER_DAV_HOME + path.replaceFirst(Server.HOME, "/");
                else if (path.startsWith("/" + username + "/"))
                    filepath = Server.USER_DAV_HOME + path.replaceFirst("/" + username + "/", "/");
                else {
                    if (username.equals("admin")) {
                        path = ShareFolderManager.getInstance().getRealPath(path);
                        filepath = Server.DEVICE_DAV_HOME + path.replaceFirst("/home/", "/");
                    } else {
                        String newPath = "";
                        String[] paths = path.replaceFirst("/", "").split("/");
                        int length = paths.length;
                        for (int i = 0; i < length; i++) {
                            if (i == 0)
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
            }
            Log.d(TAG, "path : " + path + ", url : " + url);
        }
        return url;
    }

    public String getFileSize(String path) {
        File file = new File(path);
        long length = 0;
        if (file != null && file.exists()) {
            if (file.isDirectory()) {
                for (File f : file.listFiles()) {
                    if (f.isDirectory()) {
                        for (File child : f.listFiles()) {
                            length = length + child.length();
                        }
                    } else {
                        length += f.length();
                    }
                }
            } else {
                length = file.length();
            }
        }
        return getFileSize(length);
    }

    public String getFileSize(Long size) {
        //calculator the file size
        String s = " MB";
        double sizeMB = (double) size / 1024 / 1024;
        if (sizeMB < 1) {
            sizeMB = (double) size / 1024;
            s = " KB";
        } else if (sizeMB >= 1000) {
            sizeMB = (double) sizeMB / 1024;
            s = " GB";
            if (sizeMB >= 1000) {
                sizeMB = (double) sizeMB / 1024;
                s = " TB";
            }
        }

        //format the size
        DecimalFormat df = new DecimalFormat("#.##");
        String formatSize = df.format(sizeMB) + s;

        return formatSize;
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

    public void setFileList(ArrayList<FileInfo> list) {
        if (mFileList == null)
            mFileList = new ArrayList<FileInfo>();
        mFileList.clear();

        for (FileInfo info : list) {
            mFileList.add(info);
        }
    }

    public ArrayList<FileInfo> getFileList() {
        return mFileList;
    }
}
