package com.transcend.nas.management;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.realtek.nasfun.api.SambaStatus;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.R;
import com.transcend.nas.common.CustomNotificationManager;
import com.transcend.nas.common.CustomNotificationReceiver;
import com.transcend.nas.common.ManageFactory;
import com.transcend.nas.utils.MathUtil;
import com.tutk.IOTC.P2PService;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileFilter;

/**
 * Created by silverhsu on 16/1/20.
 */
public abstract class SmbAbstractLoader extends AsyncTaskLoader<Boolean> {

    private static final String TAG = SmbAbstractLoader.class.getSimpleName();
    protected Activity mActivity;
    protected Server mServer;
    protected String mUsername;
    protected String mPassword;
    protected String mHostname;
    protected Exception mException;
    protected String mExceptionMessage;
    protected int mNotificationID = 0;
    protected String mType = "";
    protected int mTotal = 0;
    protected int mCurrent = 0;
    protected HandlerThread mThread;
    protected Handler mHandler;
    protected Runnable mWatcher;
    protected boolean success = true;
    protected int mCount = 0;
    private String[] mLoadingString = {".", "..", "..."};
    private NotificationCompat.Builder mBuilder;

    public SmbAbstractLoader(Context context) {
        super(context);
        mActivity = (Activity) context;
        //System.setProperty("jcifs.smb.client.dfs.disabled", "true");
        System.setProperty("jcifs.smb.client.soTimeout", "5000");
        System.setProperty("jcifs.smb.client.responseTimeout", "5000");
        mServer = ServerManager.INSTANCE.getCurrentServer();
        mHostname = mServer.getHostname();
        mUsername = mServer.getUsername();
        mPassword = mServer.getPassword();
    }

    @Override
    public Boolean loadInBackground() {
        return true;
    }

    protected boolean checkSambaService() {
        SambaStatus status = mServer.getServiceStatus(Server.Service.SAMBA);
        return status.isRunning;
    }

    protected boolean isValid(String str) {
        return (str != null) && (!str.isEmpty());
    }

    public String getSmbUrl(String path) {
        StringBuilder builder = new StringBuilder();
        builder.append("smb://");
        if (isValid(mUsername) && isValid(mPassword)) {
            builder.append(mUsername);
            builder.append(":");
            builder.append(mPassword);
            builder.append("@");
        }
        String hostname = P2PService.getInstance().getIP(mHostname, P2PService.P2PProtocalType.SMB);
        builder.append(hostname);
        if (isValid(path))
            builder.append(path);
        return builder.toString();
    }

    protected String format(String path) {
        StringBuilder builder = new StringBuilder();
        if (!path.startsWith("/"))
            builder.append("/");
        builder.append(path);
        if (!path.endsWith("/"))
            builder.append("/");
        return builder.toString();
    }

    protected void setExceptionWithMessage(Exception e, String message) {
        mException = e;
        mExceptionMessage = message;
    }

    protected void setException(Exception e) {
        setExceptionWithMessage(e, null);
    }

    public String getExceptionMessage() {
        String message = getContext().getString(R.string.network_error);
        if (mException != null) {
            if (mException instanceof jcifs.smb.SmbAuthException) {
                message = getContext().getString(R.string.access_error);
            } else if (mException instanceof FileNotFoundException) {
                message = getContext().getString(R.string.operation_error);
            } else if (mException instanceof SmbException) {
                SmbException e = (SmbException) mException;
                String msg = e.getMessage();
                if (msg != null && msg.contains("Invalid operation")) {
                    message = getContext().getString(R.string.operation_error);
                } else if (msg != null && msg.contains("path specified")) {
                    message = msg;
                } else {
                    if (mExceptionMessage != null && "".equals(mExceptionMessage)) {
                        message = mExceptionMessage;
                        mExceptionMessage = null;
                    } else {
                        message = getContext().getString(R.string.network_error);
                    }
                }
            }
        }
        return message;
    }

    public void setType(String type) {
        mType = type;
    }

    public String getType() {
        return mType;
    }

    protected String createRemoteUniqueName(SmbFile source, String destination) throws MalformedURLException, SmbException {
        final boolean isDirectory = source.isDirectory();
        SmbFile dir = new SmbFile(destination);
        SmbFile[] files = dir.listFiles(new SmbFileFilter() {
            @Override
            public boolean accept(SmbFile file) throws SmbException {
                return file.isDirectory() == isDirectory;
            }
        });
        List<String> names = new ArrayList<String>();
        for (SmbFile file : files) names.add(file.getName());
        String origin = source.getName();
        String unique = origin;
        String ext = FilenameUtils.getExtension(origin);
        String prefix = FilenameUtils.getBaseName(origin.replace("/", ""));
        String suffix = isDirectory ? "/" : ext.isEmpty() ? "" : String.format(".%s", ext);
        int index = 2;
        while (names.contains(unique)) {
            unique = String.format(prefix + "_%d" + suffix, index++);
        }
        Log.w(TAG, "unique name: " + unique);
        return unique;
    }

    protected String createLocalUniqueName(SmbFile source, String destination) throws MalformedURLException, SmbException {
        final boolean isDirectory = source.isDirectory();
        File dir = new File(destination);
        File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory() == isDirectory;
            }
        });
        List<String> names = new ArrayList<String>();
        for (File file : files) names.add(file.getName());
        String origin = source.getName().replace("/", ""); // remove last character "/"
        String unique = origin;
        String ext = FilenameUtils.getExtension(origin);
        String prefix = FilenameUtils.getBaseName(origin);
        String suffix = ext.isEmpty() ? "" : String.format(".%s", ext);
        int index = 2;
        while (names.contains(unique)) {
            unique = String.format(prefix + "_%d" + suffix, index++);
        }
        Log.w(TAG, "unique name: " + unique);
        return unique;
    }

    protected void startProgressWatcher(final String title, final String destination, final int total) {
        mCount = 0;
        mThread = new HandlerThread(TAG);
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
        mHandler.postDelayed(mWatcher = new Runnable() {
            @Override
            public void run() {
                if (isLoadInBackgroundCanceled())
                    return;

                try {
                    SmbFile target = new SmbFile(destination, title);
                    int count = target.getContentLength();
                    updateProgress(mType, title, count, total);

                    if (count >= mCount)
                        mCount = count;
                    else
                        success = false;
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }

                if (mHandler != null) {
                    mHandler.postDelayed(mWatcher, 1000);
                }

            }
        }, 1000);
    }

    protected void closeProgressWatcher() {
        if (mHandler != null) {
            mHandler.removeCallbacks(mWatcher);
            mHandler = null;
        }
        if (mThread != null) {
            mThread.quit();
            mThread = null;
        }
    }

    protected void updateProgress(String type, String name, int count, int total) {
        updateProgress(type, name, count, total, true);
    }

    protected void updateProgress(String type, String name, int count, int total, boolean showProgress) {
        if (isLoadInBackgroundCanceled()) {
            return;
        }

        Log.w(TAG, mNotificationID + " progress: " + count + "/" + total + ", " + name);
        if (mBuilder == null) {
            mBuilder = CustomNotificationManager.createProgressBuilder(getContext(), mActivity, mNotificationID);
        }

        if (showProgress) {
            int max = 100;
            int progress = (total > 100) ? count / (total / 100) : 0;
            boolean indeterminate = (total == 0);

            String stat = String.format("%s / %s", MathUtil.getBytes(count), MathUtil.getBytes(total));
            String text = String.format("%s - %s", type, stat);
            String info = String.format("%d%%", progress);

            mBuilder.setContentText(text);
            mBuilder.setContentInfo(info);
            mBuilder.setProgress(max, progress, indeterminate);
        } else {
            String loading = mLoadingString[mCurrent % mLoadingString.length];
            mBuilder.setContentText(String.format("%s%s", type, loading));
        }

        String title = mTotal > 1 ? String.format("(%s/%s) " + name, mCurrent, mTotal) : name;
        mBuilder.setContentTitle(title);

        NotificationManager ntfMgr = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        ntfMgr.notify(mNotificationID, mBuilder.build());
    }

    protected void updateResult(String type, String result, String destination) {
        if (isLoadInBackgroundCanceled()) {
            return;
        }

        CustomNotificationManager.updateResult(getContext(), mNotificationID, type, result, destination);
    }

}
