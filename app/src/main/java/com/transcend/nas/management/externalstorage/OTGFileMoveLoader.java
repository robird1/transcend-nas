package com.transcend.nas.management.externalstorage;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.transcend.nas.R;

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
//    private int mNotificationID = 0;

    public OTGFileMoveLoader(Context context, ArrayList<DocumentFile> src, DocumentFile des) {
        super(context);
        mActivity = (Activity) context;
        mSrcDocumentFileList = src;
        mDesDocumentFile = des;
//        mNotificationID = FileFactory.getInstance().getNotificationID();
    }

    @Override
    public Boolean loadInBackground() {
        try {
            return move();
        } catch (IOException e) {
            e.printStackTrace();
            closeProgressWatcher();
            updateResult(getContext().getString(R.string.error));
        }
        return false;
    }

    private boolean move() throws IOException {
        updateProgress(getContext().getResources().getString(R.string.loading), 0, 0);
        for (DocumentFile file : mSrcDocumentFileList) {
            if (file.isDirectory()) {
                copyDirectoryTask(mActivity, file, mDesDocumentFile);
            } else {
                copyFileTask(mActivity, file, mDesDocumentFile);
            }
        }
        updateResult(getContext().getString(R.string.done));
        return true;
    }

    private void copyDirectoryTask(Context context, DocumentFile srcFileItem, DocumentFile destFileItem) throws IOException {
        DocumentFile destDirectory = destFileItem.createDirectory(createUniqueName(srcFileItem, destFileItem));
        DocumentFile[] files = srcFileItem.listFiles();
        for (DocumentFile file : files) {
            if (file.isDirectory()) {
                copyDirectoryTask(mActivity, file, destDirectory);
            } else {//is file
                copyFileTask(mActivity, file, destDirectory);
            }
        }
        srcFileItem.delete();
    }

    private void copyFileTask(Context context, DocumentFile srcFileItem, DocumentFile destFileItem) throws IOException {
        DocumentFile destfile = destFileItem.createFile(srcFileItem.getType(), createUniqueName(srcFileItem, destFileItem));
        int total = (int) srcFileItem.length();
        startProgressWatcher(destfile, total);
        copyFile(context, srcFileItem, destfile);
        closeProgressWatcher();
        updateProgress(destfile.getName(), total, total);
    }

    public boolean copyFile(Context context, DocumentFile srcFileItem, DocumentFile destFileItem) {
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

    private void startProgressWatcher(final DocumentFile target, final int total) {
        mThread = new HandlerThread(TAG);
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
        mHandler.post(mWatcher = new Runnable() {
            @Override
            public void run() {
                int count = (int) target.length();
                if (mHandler != null) {
                    mHandler.postDelayed(mWatcher, 1000);
                    updateProgress(target.getName(), count, total);
                }
            }
        });
    }

    private void updateResult(String result) {
        Log.w(TAG, "result: " + result);

        int icon = R.mipmap.ic_launcher;
        String name = getContext().getResources().getString(R.string.app_name);
        String type = getContext().getResources().getString(R.string.move);
        String text = String.format("%s - %s", type, result);

        NotificationManager ntfMgr = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = mActivity.getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext());
        builder.setSmallIcon(icon);
        builder.setContentTitle(name);
        builder.setContentText(text);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(true);
//        ntfMgr.notify(mNotificationID, builder.build());
//        FileFactory.getInstance().releaseNotificationID(mNotificationID);
        ntfMgr.notify(0, builder.build());
    }

}

