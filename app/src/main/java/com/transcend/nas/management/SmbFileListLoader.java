package com.transcend.nas.management;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.transcend.nas.NASApp;
import com.transcend.nas.firmware_api.ShareFolderManager;
import com.transcend.nas.service.TwonkyManager;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

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
        try {
            super.loadInBackground();
            return updateFileList();
        } catch (Exception e) {
            e.printStackTrace();
            setException(e);
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

        if (mPath.equals(NASApp.ROOT_SMB)) {
            //shared folder is relative path, we need to get the real path.
            ShareFolderManager.getInstance().updateSharedFolder();

            //fix bug, lost shared folder problem
            checkSharedFolderList();

            //update twonky image
            TwonkyManager.getInstance().updateTwonky(mPath);
        }


        return true;
    }

    private void checkSharedFolderList() {
        List<String> folders = ShareFolderManager.getInstance().getAllKey();

        for (String folder : folders) {
            //folder format : "/folder_name/"
            int length = folder.length();
            if(length > 2) {
                String name = folder.substring(1, length-1);
                boolean addToSmbList = true;
                for (FileInfo info : mFileList) {
                    if (name.equals(info.name)) {
                        addToSmbList = false;
                        break;
                    }
                }

                if (addToSmbList) {
                    FileInfo fileInfo = new FileInfo();
                    fileInfo.path = TextUtils.concat(mPath, name + "/").toString();
                    fileInfo.name = name;
                    fileInfo.time = "1970/01/01";
                    fileInfo.type = FileInfo.TYPE.DIR;
                    fileInfo.size = Long.valueOf(0);
                    mFileList.add(fileInfo);
                }
                Log.d(TAG, "check shared folder : " + name + ", lost folder : " + addToSmbList);
            }
        }
    }

    public String getPath() {
        return mPath;
    }

    public ArrayList<FileInfo> getFileList() {
        return mFileList;
    }

}
