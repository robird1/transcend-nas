package com.transcend.nas.management.firmware;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASApp;
import com.transcend.nas.management.FileInfo;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Created by ike_lee on 2016/5/23.
 */
public class FileFactory {

    private static final String TAG = FileFactory.class.getSimpleName();
    private static FileFactory mFileFactory;
    private static final Object mMute = new Object();
    private ArrayList<FileInfo> mFileList;

    public FileFactory() {
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
                if (file.name.equals("USB")) {
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
