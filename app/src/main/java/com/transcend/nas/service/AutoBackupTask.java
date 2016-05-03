package com.transcend.nas.service;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.tutk.IOTC.P2PService;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileFilter;

/**
 * Created by ike_lee on 2016/3/23.
 */
public class AutoBackupTask extends AsyncTask<String, String, Boolean>
{
    private static final String TAG = "AutoBackupService";
    private static final int BUFFER_SIZE = 4 * 1024;

    private int retryCount = 0;
    private List<String> mSrcs;
    private String mDest;
    private String uniqureName = "";

    private OutputStream mOS;
    private InputStream mIS;

    private Server mServer;
    private String mUsername;
    private String mPassword;
    private String mHostname;
    private AutoBackupTaskCallback mListener;

    public AutoBackupTask(Context context, List<String> srcs, String dest, boolean isRemoteAccess) {
        mSrcs = srcs;
        mDest = dest;
        updateServerInfo(isRemoteAccess);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    public void updateServerInfo(boolean isRemoteAccess){
        mServer = ServerManager.INSTANCE.getCurrentServer();
        mUsername = mServer.getUsername();
        mPassword = mServer.getPassword();
        if(isRemoteAccess)
            mHostname = P2PService.getInstance().getP2PIP() + ":" + P2PService.getInstance().getP2PPort(P2PService.P2PProtocalType.SMB);
        else
            mHostname = mServer.getHostname();
    }

    @Override
    protected Boolean doInBackground(String... params) {
        Boolean result = true;
        try {
            return upload();
        } catch (Exception e) {
            result = false;
            if(mListener != null) {
                mListener.onAutoBackupTaskFail(this, e);
            }
            e.printStackTrace();
        } finally {
            try {
                if (mOS != null) mOS.close();
                if (mIS != null) mIS.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if(mListener != null) {
            if(result)
                mListener.onAutoBackupTaskFinished(this);
            //else
            //    mListener.onAutoBackupTaskFail(this, e);
        }
    }

    public void addListener(AutoBackupTaskCallback listener){
        mListener = listener;
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
        builder.append(mHostname);
        if (isValid(path))
            builder.append(path);

        Log.d(TAG, "Auto backup task url : " + builder.toString());
        return builder.toString();
    }

    private boolean upload() throws IOException {
        int size = mSrcs.size();
        for (int i = 0; i < size; i++) {
            String path = mSrcs.get(i);
            File source = new File(path);
            if (source.isDirectory())
                uploadDirectory(source, getSmbUrl(mDest));
            else {
                uploadFile(source, getSmbUrl(mDest));
                if(mListener != null)
                    mListener.onAutoBackupTaskPerFinished(this,size,i+1);
            }
        }
        return true;
    }

    private void uploadDirectory(File source, String destination) throws IOException {
        String name = createUniqueName(source, destination);
        SmbFile target = new SmbFile(destination, name);
        target.mkdirs();
        File[] files = source.listFiles();
        String path = target.getPath();
        for (File file : files) {
            if (file.isDirectory())
                uploadDirectory(file, path);
            else
                uploadFile(file, path);
        }
    }

    private void uploadFile(File source, String destination) throws IOException {
        int total = (int)source.length();
        int count = 0;
        String name = createUniqueName(source, destination);
        SmbFile target = new SmbFile(destination, name);
        mOS = new BufferedOutputStream(target.getOutputStream());
        mIS = new BufferedInputStream(new FileInputStream(source));
        byte[] buffer = new byte[BUFFER_SIZE];
        int length = 0;
        while ((length = mIS.read(buffer)) != -1) {
            mOS.write(buffer, 0, length);
            count += length;
        }
        mOS.close();
        mIS.close();
        target.setLastModified(source.lastModified());
    }

    private String createUniqueName(File source, String destination) throws MalformedURLException, SmbException {
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
        String unique = isDirectory ? String.format("%s/", origin) : origin;
        String ext = FilenameUtils.getExtension(origin);
        String prefix = FilenameUtils.getBaseName(origin);
        String suffix = isDirectory ? "/" : ext.isEmpty() ? "" : String.format(".%s", ext);
        int index = 2;
        while (names.contains(unique)) {
            unique = String.format(prefix + " (%d)" + suffix, index++);
        }
        Log.w(TAG, "unique name: " + unique);
        uniqureName = unique;
        return unique;
    }

    public String getFileUniqueName(){
        return uniqureName;
    }

    public void setRetryCount(int count){
        retryCount = count;
    }

    public int getRetryCount(){
        return retryCount;
    }

    public boolean checkRetryCount(){
        return retryCount > 0;
    }

    public ArrayList<String> getFilePaths(){
        return (ArrayList<String>) mSrcs;
    }

    public interface AutoBackupTaskCallback{
        public void onAutoBackupTaskPerFinished(AutoBackupTask task, int total, int progress);
        public void onAutoBackupTaskFinished(AutoBackupTask task);
        public void onAutoBackupTaskFail(AutoBackupTask task, Exception e);
    }
}
