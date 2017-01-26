package com.transcend.nas.management.externalstorage;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import com.transcend.nas.R;
import com.transcend.nas.common.CustomNotificationManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Created by steve_su on 2016/12/30.
 */

public class OTGFileMoveLoader extends AbstractOTGMoveLoader {

    private static final String TAG = OTGFileMoveLoader.class.getSimpleName();

    private Activity mActivity;
    private ArrayList<DocumentFile> mSrcDocumentFileList;
    private DocumentFile mDesDocumentFile;

    public OTGFileMoveLoader(Context context, ArrayList<DocumentFile> src, DocumentFile des) {
        super(context);
        mActivity = (Activity) context;
        mSrcDocumentFileList = src;
        mDesDocumentFile = des;

        setType(getContext().getString(R.string.move));
        mTotal = src.size();
        mCurrent = 0;
        mNotificationID = CustomNotificationManager.getInstance().queryNotificationID();
    }

    @Override
    public Boolean loadInBackground() {
        try {
            return move();
        } catch (IOException e) {
            e.printStackTrace();
            closeProgressWatcher();
            updateResult(getContext().getString(R.string.error), DocumentFileHelper.getPath(mActivity, mDesDocumentFile.getUri()));
        }
        return false;
    }

    private boolean move() throws IOException {
        updateProgress(getContext().getResources().getString(R.string.loading), 0, 0);
        for (DocumentFile file : mSrcDocumentFileList) {
            if (file.isDirectory()) {
                moveDirectoryTask(mActivity, file, mDesDocumentFile);
            } else {
                moveFileTask(mActivity, file, mDesDocumentFile);
            }
            mCurrent++;
        }
        updateResult(getContext().getString(R.string.done), DocumentFileHelper.getPath(mActivity, mDesDocumentFile.getUri()));
        return true;
    }

    private void moveDirectoryTask(Context context, DocumentFile srcFileItem, DocumentFile destFileItem) throws IOException {
        DocumentFile destDirectory = destFileItem.createDirectory(createUniqueName(srcFileItem, destFileItem));
        DocumentFile[] files = srcFileItem.listFiles();
        mTotal += files.length;

        for (DocumentFile file : files) {
            if (file.isDirectory()) {
                moveDirectoryTask(mActivity, file, destDirectory);
            } else {//is file
                moveFileTask(mActivity, file, destDirectory);
            }
            mCurrent++;
        }
        srcFileItem.delete();
    }

    private void moveFileTask(Context context, DocumentFile srcFileItem, DocumentFile destFileItem) throws IOException {
        DocumentFile destfile = destFileItem.createFile(srcFileItem.getType(), createUniqueName(srcFileItem, destFileItem));
        int total = (int) srcFileItem.length();
        startProgressWatcher(destfile, total);
        moveFile(context, srcFileItem, destfile);
        closeProgressWatcher();
        updateProgress(destfile.getName(), total, total);
    }

    public boolean moveFile(Context context, DocumentFile srcFileItem, DocumentFile destFileItem) {
        if (srcFileItem.isFile()) {
            OutputStream out;
            InputStream in;
            ContentResolver resolver = context.getContentResolver();
            try {
                in = resolver.openInputStream(srcFileItem.getUri());
                out = resolver.openOutputStream(destFileItem.getUri());
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
                srcFileItem.delete();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "IOException ===========================================================================");

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

