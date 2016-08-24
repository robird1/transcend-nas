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
import com.transcend.nas.common.FileFactory;
import com.transcend.nas.utils.MathUtil;
import com.tutk.IOTC.P2PService;

import org.apache.commons.io.FilenameUtils;

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
    protected int mNotificationID = 0;
    protected String mType = "";
    protected int mTotal = 0;
    protected int mCurrent = 0;
    protected HandlerThread mThread;
    protected Handler mHandler;
    protected Runnable mWatcher;
    protected boolean success = true;
    protected int mCount = 0;

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
        Log.w(TAG, "loadInBackground");
        // Restructure Remote Access
        //String p2pIP = P2PService.getInstance().getP2PIP();
        //if (mHostname.contains(p2pIP))
        //    P2PService.getInstance().reStartP2PConnect();
        return true;
    }

    protected boolean checkSambaService() {
        SambaStatus status = mServer.getServiceStatus(Server.Service.SAMBA);
        return status.isRunning;
    }

    protected boolean isValid(String str) {
        return (str != null) && (!str.isEmpty());
    }

    protected String getSmbUrl(String path) {
        StringBuilder builder = new StringBuilder();
        builder.append("smb://");
        if (isValid(mUsername) && isValid(mPassword)) {
            builder.append(mUsername);
            builder.append(":");
            builder.append(mPassword);
            builder.append("@");
        }
        String hostname = mHostname;
        String p2pIP = P2PService.getInstance().getP2PIP();
        if (hostname.contains(p2pIP))
            hostname = p2pIP + ":" + P2PService.getInstance().getP2PPort(P2PService.P2PProtocalType.SMB);
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

    protected void setException(Exception e){
        mException = e;
    }

    public String getExceptionMessage(){
        String message = getContext().getString(R.string.network_error);
        if(mException != null) {
            if (mException instanceof jcifs.smb.SmbAuthException) {
                message = getContext().getString(R.string.access_error);
            } else if (mException instanceof SmbException) {
                SmbException e = (SmbException) mException;
                String msg = e.getMessage();
                if(msg != null && msg.contains("Invalid operation")){
                    message = getContext().getString(R.string.operation_error);
                }
                else {
                    message = getContext().getString(R.string.network_error);
                }
            }
        }
        return message;
    }

    public String getType(){
        return mType;
    }

    protected int getSize(SmbFile file) {
        int total = 0;
        do {
            total = file.getContentLength();
            Log.w(TAG, "file size: " + total);
        } while (total == 0);
        return total;
    }

    protected String createUniqueName(SmbFile source, String destination) throws MalformedURLException, SmbException {
        final boolean isDirectory= source.isDirectory();
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

    protected void startProgressWatcher(final String title, final SmbFile target, final int total) {
        mCount = 0;
        mThread = new HandlerThread(TAG);
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
        mHandler.post(mWatcher = new Runnable() {
            @Override
            public void run() {
                int count = target.getContentLength();
                if (mHandler != null) {
                    mHandler.postDelayed(mWatcher, 1000);
                    updateProgress(mType, title, count, total);
                }

                if (count >= mCount)
                    mCount = count;
                else
                    success = false;

            }
        });
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
        Log.w(TAG, "progress: " + count + "/" + total + ", " + name);

        int max = (count == total) ? 0 : 100;
        int progress = (total > 100) ? count / (total / 100) : 0;
        boolean indeterminate = (total == 0);
        int icon = R.mipmap.ic_launcher;

        String title = mTotal > 0 ? String.format("(%s/%s) " + name, mCurrent, mTotal) : name;
        String stat = String.format("%s / %s", MathUtil.getBytes(count), MathUtil.getBytes(total));
        String text = String.format("%s - %s", type, stat);
        String info = String.format("%d%%", progress);

        NotificationManager ntfMgr = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = mActivity.getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext());
        builder.setSmallIcon(icon);
        builder.setContentTitle(title);
        builder.setContentText(text);
        builder.setContentInfo(info);
        builder.setProgress(max, progress, indeterminate);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(true);
        ntfMgr.notify(mNotificationID, builder.build());
    }

    protected void updateResult(String type, String result, String destination) {
        Log.w(TAG, "result: " + result);

        int icon = R.mipmap.ic_launcher;
        String name = getContext().getResources().getString(R.string.app_name);
        String text = String.format("%s - %s", type, result);

        NotificationManager ntfMgr = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent();
        intent.setClass(getContext(), FileManageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if(destination != null && !destination.equals(""))
            intent.putExtra("path", destination);

        PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext());
        builder.setSmallIcon(icon);
        builder.setContentTitle(name);
        builder.setContentText(text);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(true);
        ntfMgr.notify(mNotificationID, builder.build());
        FileFactory.getInstance().releaseNotificationID(mNotificationID);
    }

}
