package com.transcend.nas.management;

import android.content.Context;
import android.util.Log;

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
        }
        return false;
    }

    private boolean rename() throws MalformedURLException, SmbException {
        String dir = new File(mPath).getParent();
        String path = new File(dir, mName).getAbsolutePath();
        SmbFile rename = new SmbFile(getSmbUrl(path));
        SmbFile target = new SmbFile(getSmbUrl(mPath));
        if (target.exists()) {
            target.renameTo(rename);
            return true;
        }
        return false;
    }

}
