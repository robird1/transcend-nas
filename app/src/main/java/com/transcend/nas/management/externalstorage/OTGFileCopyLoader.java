package com.transcend.nas.management.externalstorage;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.support.v4.provider.DocumentFile;

import com.transcend.nas.R;
import com.transcend.nas.common.CustomNotificationManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Created by steve_su on 2016/12/29.
 */

public class OTGFileCopyLoader extends AbstractOTGMoveLoader {

    private static final String TAG = OTGFileCopyLoader.class.getSimpleName();

    protected Activity mActivity;
    protected ArrayList<DocumentFile> mSrcDocumentFileList;
    protected DocumentFile mDesDocumentFile;

    public OTGFileCopyLoader(Context context, ArrayList<DocumentFile> src, DocumentFile des) {
        super(context);
        mActivity = (Activity) context;
        mSrcDocumentFileList = src;
        mDesDocumentFile = des;

        setType(getContext().getString(R.string.copy));
        mTotal = src.size();
        mCurrent = 0;
        mNotificationID = CustomNotificationManager.getInstance().queryNotificationID(this);
    }

    @Override
    public Boolean loadInBackground() {
        try {
            return copy();
        } catch (IOException e) {
            e.printStackTrace();
            closeProgressWatcher();
            updateResult(getContext().getString(R.string.error), DocumentFileHelper.getPath(mActivity, mDesDocumentFile.getUri()));
        }
        return false;
    }

    private boolean copy() throws IOException {
        for (DocumentFile file : mSrcDocumentFileList) {
            if (isLoadInBackgroundCanceled())
                return true;

            if (file.isDirectory()) {
                copyDirectoryTask(mActivity, file, mDesDocumentFile);
            } else {
                copyFileTask(mActivity, file, mDesDocumentFile);
            }
            mCurrent++;
        }
        updateResult(getContext().getString(R.string.done), DocumentFileHelper.getPath(mActivity, mDesDocumentFile.getUri()));
        return true;
    }

    public void copyDirectoryTask(Context context, DocumentFile srcFileItem, DocumentFile destFileItem) throws IOException {
        DocumentFile destDirectory = destFileItem.createDirectory(createUniqueName(srcFileItem, destFileItem));
        DocumentFile[] files = srcFileItem.listFiles();
        mTotal += files.length;

        for (DocumentFile file : files) {
            if (isLoadInBackgroundCanceled())
                return;

            if (file.isDirectory()) {
                copyDirectoryTask(context, file, destDirectory);
            } else {//is file
                copyFileTask(context, file, destDirectory);
            }
            mCurrent++;
        }
    }

    public void copyFileTask(Context context, DocumentFile srcFileItem, DocumentFile destFileItem) throws IOException {
        DocumentFile destfile = destFileItem.createFile(srcFileItem.getType(), createUniqueName(srcFileItem, destFileItem));
        String name = destfile.getName();
        int total = (int) srcFileItem.length();
        updateProgress(name, 0, total);

        startProgressWatcher(destfile, total);
        copyFile(context, srcFileItem, destfile);
        closeProgressWatcher();

        updateProgress(name, total, total);
    }

    private boolean copyFile(Context context, DocumentFile srcFileItem, DocumentFile destFileItem) throws IOException {
        if (srcFileItem.isFile()) {
            OutputStream out;
            InputStream in;
            ContentResolver resolver = context.getContentResolver();
            try {
                in = resolver.openInputStream(srcFileItem.getUri());
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
