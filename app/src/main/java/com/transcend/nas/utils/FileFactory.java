package com.transcend.nas.utils;

import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.util.Log;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASApp;
import com.transcend.nas.management.FileInfo;
import com.tutk.IOTC.P2PService;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ike_lee on 2016/5/23.
 */
public class FileFactory {

    private static final String TAG = FileFactory.class.getSimpleName();
    private static FileFactory mFileFactory;
    private static final Object mMute = new Object();
    private List<String> mNotificationList;
    private Map<String, String> mRealPathMap;
    private int RealPathMapLifeCycle = 10;
    private ArrayList<FileInfo> mFileList;
    private int mFileIndex = -1;
    private ArrayList<FileInfo> mMusicList;
    private int mMusicIndex = -1;
    private MediaPlayer mMediaPlayer;
    private MediaMetadataRetriever mMediaMetadataRetriever;
    private ArrayList<MediaPlayerListener> mMediaPlayerListener;

    public enum MediaPlayerStatus {
        NEW, LOAD, PLAY, PAUSE, STOP, PREV, NEXT, ERROR
    }

    public interface MediaPlayerListener {
        public void onMusicChange(MediaPlayerStatus status);
    }

    public FileFactory() {
        mNotificationList = new ArrayList<String>();
        mRealPathMap = new HashMap<String, String>();
        mFileList = new ArrayList<FileInfo>();
        mMusicList = new ArrayList<FileInfo>();
        mMediaPlayerListener = new ArrayList<MediaPlayerListener>();
    }

    public static FileFactory getInstance() {
        synchronized (mMute) {
            if (mFileFactory == null)
                mFileFactory = new FileFactory();
        }
        return mFileFactory;
    }

    public void addFileTypeSortRule(ArrayList<FileInfo> fileList) {
        ArrayList<FileInfo> tmp = new ArrayList<FileInfo>();
        for (FileInfo file : fileList) {
            tmp.add(file);
        }
        fileList.clear();

        for (FileInfo file : tmp) {
            if (file.type == FileInfo.TYPE.DIR) {
                fileList.add(file);
            }
        }

        for (FileInfo file : tmp) {
            if (file.type != FileInfo.TYPE.DIR) {
                fileList.add(file);
            }
        }
    }

    public void addFolderFilterRule(String path, ArrayList<FileInfo> fileList) {
        if (NASApp.ROOT_SMB.equals(path)) {
            for (FileInfo file : fileList) {
                if (file.name.equals("homes")) {
                    fileList.remove(file);
                    break;
                }
            }

            for (FileInfo file : fileList) {
                if (file.name.equals("Public")) {
                    fileList.remove(file);
                    fileList.add(0, file);
                    break;
                }
            }

            Server server = ServerManager.INSTANCE.getCurrentServer();
            String username = server.getUsername();
            if (username != null && !username.equals("admin")) {
                ArrayList<FileInfo> tmp = new ArrayList<>();
                for (FileInfo file : fileList) {
                    if (file.name.equals("Public") || file.name.equals(username))
                        tmp.add(file);
                }

                fileList.clear();
                for (FileInfo file : tmp) {
                    fileList.add(file);
                }
            }

            for (FileInfo file : fileList) {
                if (file.time.startsWith("1970/01/01")) {
                    file.time = "";
                }
            }
        }
    }

    public String getPhotoPath(boolean thumbnail, String path) {
        String url;
        if (path.startsWith(NASApp.ROOT_STG)) {
            url = "file://" + path;
        } else {
            Server server = ServerManager.INSTANCE.getCurrentServer();
            String hostname = server.getHostname();
            String p2pIp = P2PService.getInstance().getP2PIP();
            if (hostname.contains(p2pIp)) {
                hostname = p2pIp + ":" + P2PService.getInstance().getP2PPort(P2PService.P2PProtocalType.HTTP);
            }
            String username = server.getUsername();
            String hash = server.getHash();
            String filepath;
            if (path.startsWith(Server.HOME))
                filepath = Server.USER_DAV_HOME + path.replaceFirst(Server.HOME, "/");
            else if (path.startsWith("/" + username + "/"))
                filepath = Server.USER_DAV_HOME + path.replaceFirst("/" + username + "/", "/");
            else {
                if (username.equals("admin")) {
                    for (String key : mRealPathMap.keySet()) {
                        if (path.startsWith(key)) {
                            path = path.replaceFirst(key, mRealPathMap.get(key));
                            break;
                        }
                    }
                    filepath = Server.DEVICE_DAV_HOME + path.replaceFirst("/home/", "/");
                } else {
                    String newPath = "";
                    String[] paths = path.replaceFirst("/", "").split("/");
                    int length = paths.length;
                    for (int i = 0; i < length; i++) {
                        if (i == 0)
                            newPath = "/" + paths[i].toLowerCase();
                        else
                            newPath = newPath + "/" + paths[i];
                    }
                    filepath = "/dav" + newPath;
                }
            }

            if (thumbnail)
                url = "http://" + hostname + filepath + "?session=" + hash + "&thumbnail";
            else
                url = "http://" + hostname + filepath + "?session=" + hash + "&webview";
        }
        return url;
    }

    public String getFileSize(String path) {
        File file = new File(path);
        long length = 0;
        if (file != null && file.exists()) {
            if (file.isDirectory()) {
                for (File f : file.listFiles()) {
                    if (f.isDirectory()) {
                        for (File child : f.listFiles()) {
                            length = length + child.length();
                        }
                    } else {
                        length += f.length();
                    }
                }
            } else {
                length = file.length();
            }
        }
        return getFileSize(length);
    }

    public String getFileSize(Long size) {
        //calculator the file size
        String s = " MB";
        double sizeMB = (double) size / 1024 / 1024;
        if (sizeMB < 1) {
            sizeMB = (double) size / 1024;
            s = " KB";
        } else if (sizeMB >= 1000) {
            sizeMB = (double) sizeMB / 1024;
            s = " GB";
            if (sizeMB >= 1000) {
                sizeMB = (double) sizeMB / 1024;
                s = " TB";
            }
        }

        //format the size
        DecimalFormat df = new DecimalFormat("#.##");
        String formatSize = df.format(sizeMB) + s;

        return formatSize;
    }

    public int getNotificationID() {
        int id = 1;
        if (mNotificationList.size() > 0) {
            String value = mNotificationList.get(mNotificationList.size() - 1);
            id = Integer.parseInt(value) + 1;
            mNotificationList.add(Integer.toString(id));
        } else {
            mNotificationList.add(Integer.toString(id));
        }
        return id;
    }

    public void releaseNotificationID(int id) {
        String value = "" + id;
        mNotificationList.remove(value);
    }

    public void addRealPathToMap(String path, String realPath) {
        if (mRealPathMap == null)
            mRealPathMap = new HashMap<String, String>();
        if (!path.startsWith("/"))
            path = "/" + path + "/";
        if (!realPath.endsWith("/"))
            realPath = realPath + "/";
        mRealPathMap.put(path, realPath);
    }

    public int getRealPathMapSize() {
        int size = 0;
        if (mRealPathMap != null)
            size = mRealPathMap.size();
        return size;
    }

    public String getRealPathKeyFromMap(String path) {
        String result = "";
        if (mRealPathMap != null) {
            for (String key : mRealPathMap.keySet()) {
                if (path.startsWith(key)) {
                    result = key;
                    break;
                }
            }
        }
        return result;
    }

    public String getRealPathFromMap(String path) {
        String realPath = "";
        if (mRealPathMap != null) {
            for (String key : mRealPathMap.keySet()) {
                if (path.startsWith(key)) {
                    realPath = mRealPathMap.get(key);
                    break;
                }
            }
        }
        return realPath;
    }

    public boolean checkRealPathMapLifeCycle() {
        RealPathMapLifeCycle--;
        if (RealPathMapLifeCycle > 0)
            return true;
        else {
            RealPathMapLifeCycle = 10;
            return false;
        }
    }

    public void cleanRealPathMap() {
        if (mRealPathMap != null)
            mRealPathMap.clear();
    }

    public void setFileList(ArrayList<FileInfo> list) {
        if (mFileList == null)
            mFileList = new ArrayList<FileInfo>();
        mFileList.clear();

        for (FileInfo info : list) {
            mFileList.add(info);
        }
    }

    public ArrayList<FileInfo> getFileList() {
        return mFileList;
    }

    public void setMusicList(ArrayList<FileInfo> list) {
        if (mMusicList == null)
            mMusicList = new ArrayList<FileInfo>();
        mMusicList.clear();

        for (FileInfo info : list) {
            mMusicList.add(info);
        }
    }

    public ArrayList<FileInfo> getMusicList() {
        return mMusicList;
    }

    public void setMusicIndex(int index) {
        mMusicIndex = index;
    }

    public int getMusicIndex() {
        return mMusicIndex;
    }

    public void addMediaPlayerListener(MediaPlayerListener listener) {
        if (mMediaPlayerListener == null)
            mMediaPlayerListener = new ArrayList<MediaPlayerListener>();

        if (!mMediaPlayerListener.contains(listener)) {
            mMediaPlayerListener.add(listener);
        }
    }

    public void removeMediaPlayerListener(MediaPlayerListener listener) {
        if (mMediaPlayerListener != null) {
            mMediaPlayerListener.remove(listener);
        }
    }

    public void notifyMediaPlayerListener(MediaPlayerStatus status) {
        Log.d(TAG, "MediaPlayerListener Notify: " + status.toString());
        if (mMediaPlayerListener != null && mMediaPlayerListener.size() > 0) {
            Log.d(TAG, "MediaPlayerListener Size: " + mMediaPlayerListener.size());
            for (MediaPlayerListener listener : mMediaPlayerListener) {
                listener.onMusicChange(status);
            }
        } else {
            //empty lister, reset mediaInfo
            Log.d(TAG, "MediaPlayerListener Size: Empty, release media item");
            if (mMediaPlayer != null) {
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
            if (mMediaMetadataRetriever != null) {
                mMediaMetadataRetriever.release();
                mMediaMetadataRetriever = null;
            }
            mFileIndex = -1;
        }
    }

    public void setMediaInfo(MediaPlayer mediaPlayer, MediaMetadataRetriever mediaMetadataRetriever) {
        mMediaPlayer = mediaPlayer;
        mMediaMetadataRetriever = mediaMetadataRetriever;
    }

    public MediaPlayer getMediaPlayer() {
        return mMediaPlayer;
    }

    public MediaMetadataRetriever getMediaMetadataRetriever() {
        return mMediaMetadataRetriever;
    }
}
