package com.transcend.nas.viewer.music;

import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.util.Log;

import com.transcend.nas.management.FileInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by ike_lee on 2016/11/16.
 */
public class MusicManager {

    private static final String TAG = MusicManager.class.getSimpleName();
    private static MusicManager mMusicManager;
    private static final Object mMute = new Object();

    private ArrayList<FileInfo> mMusicList;
    private ArrayList<FileInfo> mShuffleList;
    private int mMusicIndex = -1;
    private boolean isRemoteAction = false;
    private MediaPlayer mMediaPlayer;
    private MediaMetadataRetriever mMediaMetadataRetriever;
    private ArrayList<MediaPlayerListener> mMediaPlayerListener;

    public enum MediaPlayerStatus {
        NEW, LOAD, PLAY, PAUSE, STOP, PREV, NEXT, SHUFFLE, ERROR
    }

    public interface MediaPlayerListener {
        public void onMusicChange(MediaPlayerStatus status);
    }

    public MusicManager() {
        mMusicList = new ArrayList<>();
        mShuffleList = new ArrayList<>();
        mMediaPlayerListener = new ArrayList<MediaPlayerListener>();
    }

    public static MusicManager getInstance() {
        synchronized (mMute) {
            if (mMusicManager == null)
                mMusicManager = new MusicManager();
        }
        return mMusicManager;
    }

    public void setMusicList(ArrayList<FileInfo> list) {
        setMusicList(list, 0, false);
    }

    public void setMusicList(ArrayList<FileInfo> list, int index, boolean remote) {
        if (mMusicList == null)
            mMusicList = new ArrayList<FileInfo>();
        mMusicList.clear();

        if (mShuffleList == null)
            mShuffleList = new ArrayList<FileInfo>();
        mShuffleList.clear();

        for (FileInfo info : list) {
            mMusicList.add(info);
        }

        mMusicIndex = index;
        isRemoteAction = remote;
    }

    public ArrayList<FileInfo> getMusicList() {
        return mMusicList;
    }

    public int getMusicIndex(FileInfo info) {
        if (info != null && mMusicList != null) {
            int index = -1;
            for (FileInfo tmp : mMusicList) {
                index++;
                if (tmp.path.equals(info.path) && tmp.name.equals(info.name))
                    return index;
            }
        }
        return -1;
    }

    public void setShuffleList(int index) {
        if (mShuffleList == null)
            mShuffleList = new ArrayList<FileInfo>();
        mShuffleList.clear();

        if (mMusicList != null && mMusicList.size() > 0) {
            int length = mMusicList.size();
            for (int i = 0; i < length; i++) {
                if (i != index)
                    mShuffleList.add(mMusicList.get(i));
            }
            Collections.shuffle(mShuffleList);
            if (length > index && index >= 0)
                mShuffleList.add(0, mMusicList.get(index));
        }
    }

    public ArrayList<FileInfo> getShuffleList() {
        return mShuffleList;
    }

    public int getMusicListSize() {
        int size = 0;
        if (mMusicList != null)
            size = mMusicList.size();
        return size;
    }

    public void setMusicIndex(int index) {
        mMusicIndex = index;
    }

    public int getMusicIndex() {
        return mMusicIndex;
    }

    public boolean isRemoteAction() {
        return isRemoteAction;
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
