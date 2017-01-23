package com.transcend.nas.management;

import android.content.Context;

import com.transcend.nas.R;
import com.transcend.nas.common.CustomNotificationManager;
import com.transcend.nas.viewer.document.FileDownloadManager;

import java.io.File;
import java.io.IOException;
import java.util.List;

import jcifs.smb.SmbFile;

/**
 * Created by silverhsu on 16/2/18.
 */
public class FileDownloadLoader extends SmbAbstractLoader {

    private Context mContext;
    private List<String> mSrcs;
    private String mDest;

    public FileDownloadLoader(Context context, List<String> srcs, String dest) {
        super(context);
        mContext = context;
        mSrcs = srcs;
        mDest = dest;
        mNotificationID = CustomNotificationManager.getInstance().queryNotificationID();
        mType = getContext().getString(R.string.download);
        mTotal = mSrcs.size();
        mCurrent = 0;
    }

    @Override
    public Boolean loadInBackground() {
        try {
            super.loadInBackground();
            return download();
        } catch (Exception e) {
            e.printStackTrace();
            setException(e);
            updateResult(mType, getContext().getString(R.string.error), mDest);
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
            mCurrent++;
        }
        updateResult(mType, getContext().getString(R.string.done), mDest);
        return true;
    }

    private void downloadDirectory(SmbFile source, String destination) throws IOException {
        String name = createLocalUniqueName(source, destination);
        File target = new File(destination, name);
        target.mkdirs();
        SmbFile[] files = source.listFiles();
        for (SmbFile file : files) {
            if (!file.isHidden())
                mTotal++;
        }

        String path = target.getPath();
        for (SmbFile file : files) {
            if(file.isHidden())
                continue;

            if (file.isDirectory())
                downloadDirectory(file, path);
            else
                downloadFile(file, path);
            mCurrent++;
        }
    }

    private void downloadFile(SmbFile source, String destination) throws IOException {
        String name = createLocalUniqueName(source, destination);
        String srcPath = source.getPath().split(mServer.getHostname())[1];
        FileDownloadManager.getInstance(mContext).start(mContext, srcPath, destination, name);
    }

}
