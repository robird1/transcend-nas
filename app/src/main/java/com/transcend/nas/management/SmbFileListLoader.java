package com.transcend.nas.management;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.realtek.nasfun.api.SambaStatus;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASPref;
import com.tutk.IOTC.P2PService;

import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.SimpleFormatter;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * Created by silverhsu on 16/1/7.
 */
public class SmbFileListLoader extends SmbAbstractLoader {

    private static final String TAG = SmbFileListLoader.class.getSimpleName();

    private ArrayList<FileInfo> mFileList;
    private String mPath;

    public SmbFileListLoader(Context context, String path) {
        super(context);
        mFileList = new ArrayList<FileInfo>();
        mPath = format(path);
    }

    @Override
    public Boolean loadInBackground() {
        Log.w(TAG, "loadInBackground");
        try {
            super.loadInBackground();
            return updateFileList();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean updateFileList() throws MalformedURLException, SmbException {
        String url = super.getSmbUrl(mPath);
        SmbFile target = new SmbFile(url);
        if (target.isFile())
            return false;

        SmbFile[] files = target.listFiles();
        Log.w(TAG, "SmbFile[] size: " + files.length);
        for (SmbFile file : files) {
            if (file.isHidden())
                continue;
            FileInfo fileInfo = new FileInfo();
            fileInfo.path = TextUtils.concat(mPath, file.getName()).toString();
            fileInfo.name = file.getName().replace("/", "");
            fileInfo.time = FileInfo.getTime(file.getLastModified());
            fileInfo.type = file.isFile() ? FileInfo.getType(file.getPath()) : FileInfo.TYPE.DIR;
            fileInfo.size = file.length();
            mFileList.add(fileInfo);
        }
        Log.w(TAG, "mFileList size: " + mFileList.size());
        return true;
    }

    public String getPath() {
        return mPath;
    }

    public ArrayList<FileInfo> getFileList() {
        return mFileList;
    }

}
