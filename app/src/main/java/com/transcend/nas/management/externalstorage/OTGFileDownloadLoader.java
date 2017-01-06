package com.transcend.nas.management.externalstorage;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import com.transcend.nas.R;
import com.transcend.nas.common.CustomNotificationManager;
import com.transcend.nas.management.SmbAbstractLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import jcifs.smb.SmbFile;

/**
 * Created by steve_su on 2017/1/3.
 */

public class OTGFileDownloadLoader extends SmbAbstractLoader {
    private static final String TAG = OTGFileDownloadLoader.class.getSimpleName();

    private List<String> mSrcs;
    private String mDest;

    private OutputStream mOS;
    private InputStream mIS;

    private DocumentFile mDestFileItem;
    private int mCount;

    public OTGFileDownloadLoader(Context context, List<String> srcs, String dest, DocumentFile destFileItem) {
        super(context);
        mSrcs = srcs;
        mDest = dest;
        mNotificationID = CustomNotificationManager.getInstance().queryNotificationID();
        mType = getContext().getString(R.string.download);
        mTotal = mSrcs.size();
        mCurrent = 0;
        mDestFileItem = destFileItem;
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
                downloadDirectoryTask(mActivity, source, mDestFileItem);
            else
                downloadFileTask(mActivity, source, mDestFileItem);
            mCurrent++;
        }
        updateResult(mType, getContext().getString(R.string.done), mDest);
        return true;
    }

    private void downloadDirectoryTask(Context context, SmbFile srcFileItem, DocumentFile destFileItem) throws IOException {
        try {
            Log.d(TAG, "srcFileItem.getName(): "+ srcFileItem.getName());
            String dirName;
            if (mCount == 0) {
                dirName = createLocalUniqueName(srcFileItem, mDest);
            } else {
                dirName = srcFileItem.getName().split("/")[0];
            }
            mCount++;

            DocumentFile destDirectory = destFileItem.createDirectory(dirName);
            Log.d(TAG, "destDirectory.getName(): "+ destDirectory.getName());
            SmbFile[] files = srcFileItem.listFiles();
            for (SmbFile file : files) {
                Log.d(TAG, "file.getPath(): "+ file.getPath());
                if (file.isDirectory()) {
                    downloadDirectoryTask(mActivity, file, destDirectory);
                } else {
                    downloadFileTask(mActivity, file, destDirectory);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void downloadFileTask(Context context, SmbFile srcFileItem, DocumentFile destFileItem) throws IOException {
        Log.d(TAG, "[Enter] downloadFileTask()");
        try {
            DocumentFile destfile = destFileItem.createFile(null, srcFileItem.getName());
            int total = (int) srcFileItem.length();
//                startProgressWatcher(destfile, total);
            downloadFile(context, srcFileItem, destfile);
//                closeProgressWatcher();
//                updateProgress(destfile.getName(), total, total);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean downloadFile(Context context, SmbFile srcFileItem, DocumentFile destFileItem) throws IOException {
        if (srcFileItem.isFile()) {
            OutputStream out;
            InputStream in;
            ContentResolver resolver = context.getContentResolver();
            try {
                String urlPath = srcFileItem.getURL().getPath();
                in = resolver.openInputStream(Uri.parse(urlPath));
                out = resolver.openOutputStream(destFileItem.getUri());
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "IOException =================================================================");

            }
            return true;
        } else if (srcFileItem.isDirectory()) {
            return true;
        } else {
            try {
                throw new Exception("item is not a file");
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

    }

}
