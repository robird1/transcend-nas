package com.transcend.nas.viewer.music;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.management.FileInfo;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Created by ike_lee on 2016/7/7.
 */
public class MusicService extends Service implements MusicLoader.MusicLoaderCallBack, MusicManager.MediaPlayerListener {

    private static final String TAG = "MusicService";
    private static int MUSIC_NOTIFICATION = 10000;
    private static int ERROR_NOTIFICATION = 10001;
    private MusicLoader mMusicLoader;
    private NotificationCompat.Builder mBuilder;
    private RemoteViews mRemoteViews;
    private RemoteViews mBigRemoteViews;
    private MediaPlayer mMediaPlayer;
    private MediaMetadataRetriever mMediaMetadataRetriever;
    private Notification mNotification;
    private Handler mHandler;
    private ArrayList<FileInfo> mFileList;
    private int mFileIndex = -1;
    private MusicActivity.MUSIC_MODE mMusicMode = MusicActivity.MUSIC_MODE.NORMAL;
    private boolean showProgress = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        if (showProgress)
            mHandler = new Handler();
        mFileList = MusicManager.getInstance().getMusicList();
        mFileIndex = MusicManager.getInstance().getMusicIndex();
        mRemoteViews = new RemoteViews(getPackageName(), R.layout.notification_music);

        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(R.drawable.ic_audiotrack_white_24dp);
        mBuilder.setOngoing(true);
        mNotification = mBuilder.build();
        mNotification.contentView = mRemoteViews;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mBigRemoteViews = new RemoteViews(getPackageName(), R.layout.notification_music_big);
            mNotification.bigContentView = mBigRemoteViews;
        }

        MusicManager.getInstance().addMediaPlayerListener(this);
        MusicManager.getInstance().notifyMediaPlayerListener(MusicManager.MediaPlayerStatus.LOAD);
    }

    @Override
    public void onDestroy() {
        MusicManager.getInstance().removeMediaPlayerListener(this);
        stopMusicLoader();
        stopMusicPlayer();
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void loadMusicPlayer(int index) {
        if (mFileList != null && mFileList.size() > index && index >= 0) {
            stopMusicLoader();
            mMusicLoader = new MusicLoader(getApplicationContext(), mFileList.get(index).path);
            mMusicLoader.setListener(this);
            mMusicLoader.execute();
        } else {
            stopMusicLoader();
            stopSelf();
        }
    }

    private void startMusicPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.start();
            if (mHandler != null)
                mHandler.postDelayed(runnable, 0);
        }

        setUpNotification(false, true, true);
    }

    private void pauseMusicPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
            if (mHandler != null)
                mHandler.removeCallbacks(runnable);
        }

        setUpNotification(false, false, true);
    }

    private void stopMusicPlayer() {
        setUpNotification(false, false, false);
        if (mHandler != null) {
            mHandler.removeCallbacks(runnable);
            mHandler = null;
        }
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (mMediaMetadataRetriever != null) {
            mMediaMetadataRetriever.release();
            mMediaMetadataRetriever = null;
        }
    }

    private void loadNextMusicWithMode() {
        if (mHandler != null)
            mHandler.removeCallbacks(runnable);

        int length = mFileList.size();
        mMusicMode = NASPref.getMusicType(getApplicationContext());
        switch (mMusicMode) {
            case NORMAL:
                if (mFileIndex == length - 1) {
                    MusicManager.getInstance().notifyMediaPlayerListener(MusicManager.MediaPlayerStatus.STOP);
                } else {
                    loadNextMusic();
                }
                break;
            case REPEAT:
                if (length == 1) {
                    mMediaPlayer.seekTo(0);
                    startMusicPlayer();
                } else {
                    loadNextMusic();
                }
                break;
            case ONE:
                mMediaPlayer.seekTo(0);
                startMusicPlayer();
                break;
        }
    }

    private void loadNextMusic() {
        if (mFileList != null && mFileList.size() > 1) {
            stopMusicPlayer();
            mFileIndex = (mFileIndex + 1) % mFileList.size();
            MusicManager.getInstance().notifyMediaPlayerListener(MusicManager.MediaPlayerStatus.LOAD);
        }
    }

    private void loadPrevMusic() {
        if (mFileList != null && mFileList.size() > 1) {
            stopMusicPlayer();
            if (mFileIndex == 0)
                mFileIndex = mFileList.size() - 1;
            else
                mFileIndex = mFileIndex - 1;
            MusicManager.getInstance().notifyMediaPlayerListener(MusicManager.MediaPlayerStatus.LOAD);
        }
    }

    private void setRemoteViews(String title, String artist, String album, Bitmap bm, PendingIntent intent) {
        //set up RemoteView
        mRemoteViews.setTextViewText(R.id.music_title, title);
        mRemoteViews.setTextViewText(R.id.music_album, artist + " - " + album);
        if (bm != null)
            mRemoteViews.setImageViewBitmap(R.id.music_image, bm);
        else
            mRemoteViews.setImageViewResource(R.id.music_image, R.drawable.ic_audiotrack_gray_big);

        mRemoteViews.setOnClickPendingIntent(R.id.music_close, intent);
    }

    private void setBigRemoteViews(String title, String artist, String album, Bitmap bm, PendingIntent intent) {
        if (mBigRemoteViews == null)
            return;

        mBigRemoteViews.setTextViewText(R.id.music_title, title);
        mBigRemoteViews.setTextViewText(R.id.music_album, artist + " - " + album);
        if (bm != null)
            mBigRemoteViews.setImageViewBitmap(R.id.music_image, bm);
        else
            mBigRemoteViews.setImageViewResource(R.id.music_image, R.drawable.ic_audiotrack_gray_big);

        mBigRemoteViews.setOnClickPendingIntent(R.id.music_close, intent);
    }

    private void setUpNotification(boolean init, boolean play, boolean addIntent) {
        String tmp = "";
        String title = getString(R.string.unknown_title);
        String artist = getString(R.string.unknown_artist);
        String album = getString(R.string.unknown_album);
        Bitmap bm = null;
        int icon = play ? R.drawable.player_pause_button : R.drawable.player_play_button;

        if(init) {
            if (mFileList != null && mFileIndex >= 0 && mFileList.size() > mFileIndex) {
                tmp = mFileList.get(mFileIndex).name;
                if (tmp != null && !tmp.equals(""))
                    title = tmp;
            }

            if (mMediaMetadataRetriever != null) {
                tmp = mMediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                if (tmp != null && !tmp.equals(""))
                    artist = tmp;

                tmp = mMediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                if (tmp != null && !tmp.equals(""))
                    album = tmp;

                byte[] bytes = mMediaMetadataRetriever.getEmbeddedPicture();
                if (bytes != null && bytes.length > 0) {
                    InputStream is = new ByteArrayInputStream(bytes);
                    bm = BitmapFactory.decodeStream(is);
                }
            } else {
                mRemoteViews.setViewVisibility(R.id.music_album, View.GONE);
                if (mBigRemoteViews != null) {
                    mBigRemoteViews.setViewVisibility(R.id.music_album, View.GONE);
                }
            }

            Intent intent = new Intent();
            intent.setAction(MusicReceiver.MUSIC_CLOSE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);

            setRemoteViews(title, artist, album, bm, pendingIntent);
            setBigRemoteViews(title, artist, album, bm, pendingIntent);
        }

        if(addIntent) {
            Intent intent1 = new Intent();
            intent1.setAction(play ? MusicReceiver.MUSIC_PAUSE : MusicReceiver.MUSIC_PLAY);
            PendingIntent pendingIntent1 = PendingIntent.getBroadcast(getApplicationContext(), 0, intent1, 0);

            Intent intent2 = new Intent();
            intent2.setAction(MusicReceiver.MUSIC_NEXT);
            PendingIntent pendingIntent2 = PendingIntent.getBroadcast(getApplicationContext(), 0, intent2, 0);

            mRemoteViews.setOnClickPendingIntent(R.id.music_play, pendingIntent1);
            mRemoteViews.setOnClickPendingIntent(R.id.music_next, pendingIntent2);
            if (mBigRemoteViews != null) {
                mBigRemoteViews.setOnClickPendingIntent(R.id.music_play, pendingIntent1);
                mBigRemoteViews.setOnClickPendingIntent(R.id.music_next, pendingIntent2);

                Intent intent = new Intent();
                intent.setAction(MusicReceiver.MUSIC_PREV);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);
                mBigRemoteViews.setOnClickPendingIntent(R.id.music_previous, pendingIntent);
            }
        }
        else{
            mRemoteViews.setOnClickPendingIntent(R.id.music_play, null);
            mRemoteViews.setOnClickPendingIntent(R.id.music_next, null);
            if (mBigRemoteViews != null) {
                mBigRemoteViews.setOnClickPendingIntent(R.id.music_play, null);
                mBigRemoteViews.setOnClickPendingIntent(R.id.music_next, null);
                mBigRemoteViews.setOnClickPendingIntent(R.id.music_previous, null);
            }
        }

        mRemoteViews.setImageViewResource(R.id.music_play, icon);
        if(mBigRemoteViews != null) {
            mBigRemoteViews.setImageViewResource(R.id.music_play, icon);
        }

        startForeground(MUSIC_NOTIFICATION, mNotification);
    }

    private void updateNotification() {
        String startTime = "";
        String endTime = "";

        if (mMediaPlayer != null) {
            double start = mMediaPlayer.getCurrentPosition();
            startTime = String.format("%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes((long) start),
                    TimeUnit.MILLISECONDS.toSeconds((long) start) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) start)));

            double end = mMediaPlayer.getDuration();
            endTime = String.format("%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes((long) end),
                    TimeUnit.MILLISECONDS.toSeconds((long) end) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) end)));
        }

        if (mMediaMetadataRetriever != null) {
            String title = mMediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            if (title != null && !title.equals("")) {
                mRemoteViews.setTextViewText(R.id.music_title, title + "   " + startTime + "/" + endTime);
                mBigRemoteViews.setTextViewText(R.id.music_title, title + "   " + startTime + "/" + endTime);
            } else {
                mRemoteViews.setTextViewText(R.id.music_title, "Unknown title" + "   " + startTime + "/" + endTime);
                mBigRemoteViews.setTextViewText(R.id.music_title, "Unknown title" + "   " + startTime + "/" + endTime);
            }
        }

        startForeground(MUSIC_NOTIFICATION, mNotification);
    }

    private void showNotificationResult(String type, String result) {
        int icon = R.mipmap.ic_launcher;
        String name = getString(R.string.app_name);
        String text = String.format("%s - %s", type, result);

        NotificationManager ntfMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
        builder.setSmallIcon(icon);
        builder.setContentTitle(name);
        builder.setContentText(text);
        builder.setAutoCancel(true);
        ntfMgr.notify(ERROR_NOTIFICATION, builder.build());
    }

    private void hideNotificationResult() {
        NotificationManager ntfMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        ntfMgr.cancel(ERROR_NOTIFICATION);
    }

    private void stopMusicLoader() {
        if (mMusicLoader != null) {
            if (!mMusicLoader.isCancelled()) {
                mMusicLoader.cancel(true);
            }
            mMusicLoader = null;
        }
    }

    @Override
    public void onMusicLoadFinish(MusicLoader loader) {
        mMediaPlayer = loader.getMediaPlayer();
        if (mMediaPlayer != null) {
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    loadNextMusicWithMode();
                }
            });

            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.d(TAG, "MediaPlayer onErrorListener : " + what);
                    MusicManager.getInstance().notifyMediaPlayerListener(MusicManager.MediaPlayerStatus.ERROR);
                    return false;
                }
            });
        }
        mMediaMetadataRetriever = loader.getMediaMetadataRetriever();

        MusicManager.getInstance().setMediaInfo(mMediaPlayer, mMediaMetadataRetriever);
        if (mMediaPlayer != null) {
            MusicManager.getInstance().setMusicIndex(mFileIndex);
            MusicManager.getInstance().notifyMediaPlayerListener(MusicManager.MediaPlayerStatus.NEW);
        } else {
            MusicManager.getInstance().setMusicIndex(-1);
            MusicManager.getInstance().notifyMediaPlayerListener(MusicManager.MediaPlayerStatus.ERROR);
        }
        mMusicLoader = null;
    }

    @Override
    public void onMusicLoadFail() {
        MusicManager.getInstance().notifyMediaPlayerListener(MusicManager.MediaPlayerStatus.ERROR);
        mMusicLoader = null;
    }

    private Runnable runnable = new Runnable() {
        public void run() {
            updateNotification();
            if (mHandler != null)
                mHandler.postDelayed(runnable, 1000);
        }
    };

    @Override
    public void onMusicChange(MusicManager.MediaPlayerStatus status) {
        switch (status) {
            case NEW:
                setUpNotification(true, true, true);
                startMusicPlayer();
                break;
            case LOAD:
                hideNotificationResult();
                loadMusicPlayer(mFileIndex);
                break;
            case PLAY:
                startMusicPlayer();
                break;
            case PAUSE:
                pauseMusicPlayer();
                break;
            case ERROR:
                showNotificationResult(getString(R.string.music), getString(R.string.error));
                stopMusicLoader();
                stopSelf();
                break;
            case STOP:
                stopMusicLoader();
                stopSelf();
                break;
            case PREV:
                loadPrevMusic();
                break;
            case NEXT:
                loadNextMusic();
                break;
        }
    }
}
