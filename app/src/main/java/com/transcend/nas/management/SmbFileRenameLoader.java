package com.transcend.nas.management;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.transcend.nas.management.firmware.FileFactory;
import com.transcend.nas.management.firmware.ShareFolderManager;
import com.transcend.nas.service.FileRecentFactory;
import com.transcend.nas.service.FileRecentInfo;
import com.transcend.nas.service.FileRecentManager;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.net.MalformedURLException;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * Created by silverhsu on 16/1/30.
 */
public class SmbFileRenameLoader extends SmbAbstractLoader {

    private static final String TAG = SmbFileRenameLoader.class.getSimpleName();

    private String mPath;
    private String mName;

    public SmbFileRenameLoader(Context context, String path, String name) {
        super(context);
        mPath = path;
        mName = name;
    }

    @Override
    public Boolean loadInBackground() {
        try {
            super.loadInBackground();
            return rename();
        } catch (Exception e) {
            e.printStackTrace();
            setException(e);
        }
        return false;
    }

    private boolean rename() throws MalformedURLException, SmbException {
        String dir = new File(mPath).getParent();
        String path = new File(dir, mName).getAbsolutePath();
        SmbFile rename = new SmbFile(getSmbUrl(path));
        SmbFile target = new SmbFile(getSmbUrl(mPath));
        if (target.exists()) {
            //create old action info before rename
            FileRecentInfo oldAction = FileRecentFactory.create(getContext(), target, null);

            //start rename
            target.renameTo(rename);

            //create new action info
            FileRecentInfo newAction = FileRecentFactory.create(getContext(), rename, FileRecentInfo.ActionType.RENAME);

            //update action in db
            if (oldAction != null && newAction != null) {
                if(oldAction.info != null) {
                    String tmp = mPath.endsWith("/") ? mPath.substring(0, mPath.length() - 1) : mPath;
                    oldAction.info.path = tmp;
                    oldAction.realPath = ShareFolderManager.getInstance().getRealPath(tmp);
                }
                if(newAction.info != null) {
                    newAction.info.path = path;
                    newAction.realPath = ShareFolderManager.getInstance().getRealPath(path);
                }
                FileRecentManager.getInstance().setAction(oldAction, newAction);
                //TODO : update other action in this folder
            }
            return true;
        }
        return false;
    }

}
