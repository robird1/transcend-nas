package com.transcend.nas.management;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.common.LoaderID;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileFilter;

/**
 * Created by steve_su on 2016/11/8.
 */

public class OpenWithUploadHandler{

    private static final String TAG = OpenWithUploadHandler.class.getSimpleName();
    static final String IS_OPEN_WITH_UPLOAD = "is_open_with_upload";
    private FileManageActivity mActivity;
    private FileInfo mSelectedFile;
    private String mLocalStoredPath;
    private SmbFileListLoader mSmbFileListLoader;
    private String mTempFilePath;

    public OpenWithUploadHandler(FileManageActivity activity, FileInfo fileInfo, String localPath, SmbFileListLoader loader)
    {
        mActivity = activity;
        mSelectedFile = fileInfo;
        mLocalStoredPath = localPath;
        mSmbFileListLoader = loader;

        mSelectedFile.setLastModifiedTime(fileInfo.time);

        Log.d(TAG, "fileInfo.time: "+ fileInfo.time);
        Log.d(TAG, "mLocalStoredPath: "+ mLocalStoredPath);

    }

    void showDialog()
    {
        Log.d(TAG, "[Enter] showDialog() ");

        WindowManager windowManager = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        params.format = PixelFormat.TRANSLUCENT;
        params.gravity = Gravity.CENTER;
        LayoutInflater inflater = LayoutInflater.from(mActivity.getApplication());
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.dialog_upload, null);
        setupClickListener(layout, windowManager);
        windowManager.addView(layout, params);

    }

    private void setupClickListener(final LinearLayout layout, final WindowManager windowManager)
    {
        View yesButton = layout.findViewById(R.id.button_yes);
        View noButton = layout.findViewById(R.id.button_no);
        if (yesButton != null)
        {
            yesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doProcess();
                    windowManager.removeView(layout);
                }
            });
        }
        if (noButton != null)
        {
            noButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    windowManager.removeView(layout);
                }
            });
        }

    }

    private void doProcess()
    {
        Log.d(TAG, "[Enter] doProcess() ");

        boolean isRemoteFileChanged = isRemoteFileChanged();
        if (isRemoteFileChanged)
        {
            // show a dialog to notify user whether to overrite the existing file if the last modified time is changed
            showDialogRemoteFileChanged();
        }
        else
        {
            // replace the existing same name file if the last modified time is identical
            upload();
        }
    }

    private boolean isRemoteFileChanged()
    {
        Log.d(TAG, "[Enter] isRemotFileChanged()");

        File source = new File(mLocalStoredPath);
//        Log.d(TAG, "source.getName(): "+ source.getName());

        SmbFile[] files = getDestinationDirFiles();

        for (SmbFile file : files)
        {
            Log.d(TAG, "file.getName(): " + file.getName());

            boolean isEqual = file.getName().equals(source.getName());

            if (isEqual) {

                Log.d(TAG, "file.getDate() long: " + file.getDate());
                Log.d(TAG, "FileInfo.getTime(file.getDate()): " + FileInfo.getTime(file.getDate()));
                Log.d(TAG, "mSelectedFile.getLastModifiedTime(): " + mSelectedFile.getLastModifiedTime());

//                if (file.getDate() != mSelectedFile.getLastModifiedTime()) {
                String nasLastModifiedTime = FileInfo.getTime(file.getDate());
                if (! nasLastModifiedTime.equals(mSelectedFile.getLastModifiedTime())) {

                    Log.d(TAG, "The remote file has been modified!!!!");
                    return true;
                }
                else
                {
                    Log.d(TAG, "The remote file has not been modified...");
                    return false;
                }
            }
        }

        return false;
    }

    private void showDialogRemoteFileChanged()
    {
        Log.d(TAG, "[Enter] showDialogRemoteFileChanged()");

        String[] message = {mActivity.getResources().getString(R.string.dialog_remote_file_changed)};
        new AlertDialog.Builder(mActivity).setTitle(R.string.app_name).setItems(message, null).setPositiveButton(
                R.string.dialog_button_replace, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        upload();

                    }
                }).setNegativeButton(R.string.dialog_button_save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                saveToDownloadFolder();

            }
        }).setCancelable(false).create().show();
    }

    /**
     * This method will request a upload action which upload a new file with unique name and then
     * delete the old file and finally rename the new file to the old name.
     *
     */
    private void upload() {
        Bundle args = new Bundle();
        ArrayList sourceFilesList = new ArrayList();
        sourceFilesList.add(mLocalStoredPath);
        args.putStringArrayList("paths", sourceFilesList);        // source file to be uploaded
        args.putString("path", getRemoteFileDirPath());           // remote location
        args.putBoolean(IS_OPEN_WITH_UPLOAD, true);
        mActivity.getLoaderManager().restartLoader(LoaderID.LOCAL_FILE_UPLOAD_OPEN_WITH, args, mActivity).forceLoad();
    }

    private void saveToDownloadFolder()
    {
        Bundle args = new Bundle();
        ArrayList sourceFileList = new ArrayList();
        sourceFileList.add(mLocalStoredPath);
        args.putStringArrayList("paths", sourceFileList);                                // the copied file path
        args.putString("path", NASPref.getDownloadLocation(mActivity));                  // the default download location
        mActivity.getLoaderManager().restartLoader(LoaderID.LOCAL_FILE_COPY, args, mActivity).forceLoad();
    }

    /**
     * // TODO check this function
     *
     * @return
     */
    private SmbFile[] getDestinationDirFiles()
    {
        File source = new File(mLocalStoredPath);
        String destination = mSmbFileListLoader.getSmbUrl(getRemoteFileDirPath());

        final boolean isDirectory = source.isDirectory();
        SmbFile[] files = null;

        try {
            SmbFile dir = new SmbFile(destination);
            files = dir.listFiles(new SmbFileFilter() {
                @Override
                public boolean accept(SmbFile file) throws SmbException {
                    return file.isDirectory() == isDirectory;
                }
            });
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
        catch (SmbException e)
        {
            e.printStackTrace();
        }

        return files;
    }

    String getRemoteFileDirPath()
    {
        Log.d(TAG, "[Enter] getRemoteFileDirPath()");

        String path = null;
        if (mSelectedFile.path != null) {
            int index = mSelectedFile.path.lastIndexOf("/");
            path = mSelectedFile.path.subSequence(0, index + 1).toString();

            Log.d(TAG, "remote file dir: " + path);
        }

        return path;
    }

    void setTempFilePath(String path)
    {
        mTempFilePath = path;
    }

    String getTempFilePath()
    {
        return mTempFilePath;
    }

}
