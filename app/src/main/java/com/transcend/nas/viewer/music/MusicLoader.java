package com.transcend.nas.viewer.music;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.transcend.nas.utils.MediaFactory;

/**
 * Created by ikelee on 16/7/4.
 */
public class MusicLoader extends AsyncTask<String, String, Boolean> {
    private static final String TAG = MusicLoader.class.getSimpleName();

    private Context mContext;
    private String mPath;
    private MediaPlayer mMediaPlayer;
    private MediaMetadataRetriever mMediaMetadataRetriever;
    private MusicLoaderCallBack mListener;

    public MusicLoader(Context context, String paths) {
        mContext = context;
        mPath = paths;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if(mListener != null){
            if(result)
                mListener.onMusicLoadFinish(this);
            else
                mListener.onMusicLoadFail();
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        if(mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if(mMediaMetadataRetriever != null) {
            mMediaMetadataRetriever.release();
            mMediaMetadataRetriever = null;
        }
    }

    @Override
    protected Boolean doInBackground(String... params) {
        try {
            Uri uri = MediaFactory.createUri(mPath);
            mMediaPlayer = MediaPlayer.create(mContext, uri);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mMediaMetadataRetriever = MediaFactory.getMediaMetadataRetriever(mPath, uri);
            }
        } catch (Exception e){
            e.printStackTrace();
            if(mMediaPlayer != null){
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
            if(mMediaMetadataRetriever != null) {
                mMediaMetadataRetriever.release();
                mMediaMetadataRetriever = null;
            }
            return false;
        }
        return true;
    }

    public MediaPlayer getMediaPlayer(){
        return mMediaPlayer;
    }

    public void setListener(MusicLoaderCallBack listener){
        mListener = listener;
    }

    public MediaMetadataRetriever getMediaMetadataRetriever(){
        return mMediaMetadataRetriever;
    }


    public interface MusicLoaderCallBack{
        public void onMusicLoadFinish(MusicLoader loader);
        public void onMusicLoadFail();
    }

}
