package com.transcend.nas.management.externalstorage;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import com.transcend.nas.NASUtils;
import com.transcend.nas.R;
import com.transcend.nas.common.CustomNotificationManager;
import com.transcend.nas.management.SmbAbstractLoader;

import java.io.FileNotFoundException;
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
        String dirName = createLocalUniqueName(srcFileItem, getPath(context, destFileItem.getUri()));
        DocumentFile destDirectory = destFileItem.createDirectory(dirName);
        SmbFile[] files = srcFileItem.listFiles();
        for (SmbFile file : files) {
            Log.d(TAG, "file.getPath(): "+ file.getPath());
            if (file.isDirectory()) {
                downloadDirectoryTask(mActivity, file, destDirectory);
            } else {
                downloadFileTask(mActivity, file, destDirectory);
            }
        }
    }

    private void downloadFileTask(Context context, SmbFile srcFileItem, DocumentFile destFileItem) throws IOException {
        Log.d(TAG, "[Enter] downloadFileTask()");
        String fileName = createLocalUniqueName(srcFileItem, getPath(context, destFileItem.getUri()));
        DocumentFile destfile = destFileItem.createFile(null, fileName);
        int total = (int) srcFileItem.length();
//                startProgressWatcher(destfile, total);
        downloadFile(context, srcFileItem, destfile);
//                closeProgressWatcher();
//                updateProgress(destfile.getName(), total, total);
    }

    public boolean downloadFile(Context context, SmbFile srcFileItem, DocumentFile destFileItem) throws IOException {
        if (srcFileItem.isFile()) {
            try {
                InputStream in = srcFileItem.getInputStream();
                OutputStream out = context.getContentResolver().openOutputStream(destFileItem.getUri());
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.d(TAG, "[Enter] FileNotFoundException");
                throw new FileNotFoundException();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "[Enter] IOException");
                throw new IOException();
            }
            return true;
        } else if (srcFileItem.isDirectory()) {
            return true;
        } else {
            throw new FileNotFoundException("item is not a file");
        }
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    @TargetApi(19)
    public String getPath(final Context context, final Uri uri) {
        Log.d(TAG, "[Enter] getPath()");

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                } else {
                    Log.d(TAG, "split.length: "+ split.length);
                    String path = NASUtils.getSDLocation(context).concat("/");
                    if (split.length != 1) {
                        path = path.concat(split[1]);
                    }
                    Log.d(TAG, "destination path: "+ path);
                    return path;
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
