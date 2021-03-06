package com.transcend.nas.management;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.transcend.nas.R;
import com.transcend.nas.common.CustomNotificationManager;
import com.transcend.nas.management.firmware.FileFactory;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * Created by silverhsu on 16/2/18.
 */
public class SmbFileShareLoader extends SmbAbstractLoader {

    private static final String TAG = SmbFileShareLoader.class.getSimpleName();
    private static final int BUFFER_SIZE = 4 * 1024;

    private List<String> mSrcs;
    private String mDest;
    private ArrayList<FileInfo> mShareList;

    private OutputStream mOS;
    private InputStream mIS;

    public SmbFileShareLoader(Context context, List<String> srcs, String dest) {
        super(context);
        mSrcs = srcs;
        mDest = dest;
        mShareList = new ArrayList<>();
    }

    @Override
    public Boolean loadInBackground() {
        try {
            super.loadInBackground();
            return download();
        } catch (Exception e) {
            e.printStackTrace();
            setException(e);
        } finally {
            try {
                if (mOS != null) mOS.close();
                if (mIS != null) mIS.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private boolean download() throws IOException {
        for (String path : mSrcs) {
            SmbFile source = new SmbFile(getSmbUrl(path));
            if (source.isDirectory())
                downloadDirectory(source, mDest);
            else
                downloadFile(source, mDest);
        }
        return true;
    }

    private void downloadDirectory(SmbFile source, String destination) throws IOException {
        String name = createLocalUniqueName(source, destination);
        File target = new File(destination, name);
        target.mkdirs();
        SmbFile[] files = source.listFiles();
        String path = target.getPath();
        for (SmbFile file : files) {
            if (file.isHidden())
                continue;

            if (file.isDirectory())
                downloadDirectory(file, path);
            else
                downloadFile(file, path);
        }
    }

    private void downloadFile(SmbFile source, String destination) throws IOException {
        boolean add = true;
        int total = source.getContentLength();
        int count = 0;
        String name = createLocalUniqueName(source, destination);
        File target = new File(destination, name);
        mOS = new BufferedOutputStream(new FileOutputStream(target));
        mIS = new BufferedInputStream(source.getInputStream());
        byte[] buffer = new byte[BUFFER_SIZE];
        int length = 0;
        while ((length = mIS.read(buffer)) != -1) {
            mOS.write(buffer, 0, length);
            count += length;
            if (isLoadInBackgroundCanceled()) {
                Log.d(TAG, "Share cancel");
                add = false;
                break;
            }
        }
        mOS.close();
        mIS.close();

        if (add) {
            FileInfo fileInfo = new FileInfo();
            fileInfo.path = target.getPath();
            fileInfo.name = target.getName();
            fileInfo.time = FileInfo.getTime(target.lastModified());
            fileInfo.type = FileInfo.getType(target.getPath());
            fileInfo.size = target.length();
            mShareList.add(fileInfo);
        } else {
            target.delete();
        }
    }


    public ArrayList<FileInfo> getShareList() {
        return mShareList;
    }
}
