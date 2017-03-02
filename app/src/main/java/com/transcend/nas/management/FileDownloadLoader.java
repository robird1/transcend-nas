package com.transcend.nas.management;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.common.CustomNotificationManager;
import com.transcend.nas.management.download.AbstractDownloadManager;
import com.transcend.nas.management.download.DownloadFactory;
import com.tutk.IOTC.P2PService;

import java.io.File;
import java.io.IOException;
import java.util.List;

import jcifs.smb.SmbFile;

/**
 * Created by silverhsu on 16/2/18.
 */
public class FileDownloadLoader extends SmbAbstractLoader {

    private static final String TAG = FileDownloadLoader.class.getSimpleName();
    private Context mContext;
    private List<String> mSrcs;
    private String mDest;

//    private boolean mForbidden;
//    private Timer mTimer;

    public FileDownloadLoader(Context context, List<String> srcs, String dest) {
        super(context);
        mContext = context;
        mSrcs = srcs;
        mDest = dest;
        mNotificationID = CustomNotificationManager.getInstance().queryNotificationID(this);
        Log.d(TAG, "mNotificationID: "+ mNotificationID);
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
            setExceptionWithMessage(e, isDownloadDirectoryExist(getContext()) ? null : getContext().getString(R.string.download_location_error));
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
//        updateResult(mType, getContext().getString(R.string.done), mDest);
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
        Log.d(TAG, "[Enter] downloadFile");
//        int total = source.getContentLength();
//        int count = 0;

        String name = createLocalUniqueName(source, destination);
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String hostName = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.SMB);
        Log.d(TAG, "hostName: "+ hostName);
        String srcPath = source.getPath().split(hostName)[1];
        Log.d(TAG, "srcPath: "+ srcPath);

//        updateProgressPerSecond(name, count, total);
        Bundle data = new Bundle();
        data.putString(AbstractDownloadManager.KEY_SOURCE_PATH, srcPath);
        data.putString(AbstractDownloadManager.KEY_TARGET_PATH, destination);
        data.putString(AbstractDownloadManager.KEY_FILE_NAME, name);
        data.putInt(AbstractDownloadManager.KEY_TASK_ID, mNotificationID);
        DownloadFactory.getManager(mContext, DownloadFactory.Type.PERSIST).start(data);
    }

//    private void updateProgressPerSecond(String name, int count, int total) {
//        if (mForbidden)
//            return;
//        mForbidden = true;
//        updateProgress(mType, name, count, total);
//        mTimer = new Timer();
//        mTimer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                mForbidden = false;
//            }
//        }, 1000);
//    }


    public boolean isDownloadDirectoryExist(Context context) {
        boolean isExist = true;
        String location = NASPref.getDownloadLocation(context);
        File file = new File(location);
        if (!file.exists()) {
            isExist = false;
        } else {                                 // Enter this block if SD card has been removed
            File[] files = file.listFiles();
            if (files == null) {
                isExist = false;
            }
        }
        return isExist;
    }
}
