package com.transcend.nas.viewer.music;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.transcend.nas.utils.FileFactory;

/**
 * Created by ike_lee on 2016/7/11.
 */
public class MusicReceiver extends BroadcastReceiver {
    private static final String TAG = "MusicReceiver";
    public static final String MUSIC_PREV = "transcend_music_prev";
    public static final String MUSIC_NEXT = "transcend_music_next";
    public static final String MUSIC_PLAY = "transcend_music_play";
    public static final String MUSIC_PAUSE = "transcend_music_pause";
    public static final String MUSIC_CLOSE = "transcend_music_close";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null) {
            String action = intent.getAction();
            Log.d(TAG,"onReceiver: " + action);
            if(MUSIC_PREV.equals(action)){
                FileFactory.getInstance().notifyMediaPlayerListener(FileFactory.MediaPlayerStatus.PREV);
            }else if(MUSIC_NEXT.equals(action)) {
                FileFactory.getInstance().notifyMediaPlayerListener(FileFactory.MediaPlayerStatus.NEXT);
            }else if(MUSIC_PLAY.equals(action)){
                FileFactory.getInstance().notifyMediaPlayerListener(FileFactory.MediaPlayerStatus.PLAY);
            }else if(MUSIC_PAUSE.equals(action)){
                FileFactory.getInstance().notifyMediaPlayerListener(FileFactory.MediaPlayerStatus.PAUSE);
            }else if(MUSIC_CLOSE.equals(action)){
                FileFactory.getInstance().notifyMediaPlayerListener(FileFactory.MediaPlayerStatus.STOP);
            }
        }
    }
}


