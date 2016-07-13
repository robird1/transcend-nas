package com.transcend.nas.viewer.music;

import android.app.ActivityManager;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.utils.FileFactory;

import org.apache.commons.io.FilenameUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by ikelee on 16/7/1.
 */
public class MusicActivity extends AppCompatActivity implements FileFactory.MediaPlayerListener {

    public static final int REQUEST_CODE = MusicActivity.class.hashCode() & 0xFFFF;
    private static final String TAG = MusicActivity.class.getSimpleName();
    private static final List<String> MUSIC_SUPPORT_FORMAT = Arrays.asList("3gp", "mp4", "m4a", "aac", "ts", "3gp",
            "flac", "mp3", "mid", "xmf", "mxmf", "ogg", "mkv", "wav");

    public static enum MUSIC_MODE {
        NORMAL, REPEAT, ONE
    }

    public static boolean checkFormatSupportOrNot(String path) {
        String ext = FilenameUtils.getExtension(path);
        if (ext != null) {
            return MUSIC_SUPPORT_FORMAT.contains(ext.toLowerCase());
        }
        return false;
    }

    private RelativeLayout mProgressView;
    private Toolbar mHeaderBar;
    private ImageView musicImage;
    private ImageView musicPrev;
    private ImageView musicNext;
    private ImageView musicPlay;
    private ImageView musicMode;
    private TextView musicStart;
    private TextView musicEnd;
    private TextView musicTitle;
    private TextView musicAlbum;
    private SeekBar musicSeekBar;

    private String mPath;
    private String mMode;
    private String mRoot;
    private ArrayList<FileInfo> mList;
    private int mCurrentIndex = -1;
    private MUSIC_MODE mMusicMode = MUSIC_MODE.NORMAL;
    private boolean evenDelete = false;
    private boolean isPlay = false;

    private MediaPlayer mMediaPlayer;
    private MediaMetadataRetriever mMediaMetadataRetriever;
    private Handler myHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w(TAG, "onCreate");
        setContentView(R.layout.activity_music);
        initData();
        initHeaderBar();
        initPager();
        updateMusicMode(true);

        FileFactory.getInstance().addMediaPlayerListener(this);
        if (mCurrentIndex >= 0) {
            stopMusicService();
            FileFactory.getInstance().setMusicIndex(mCurrentIndex);
            startMusicService();
        } else {
            FileFactory.getInstance().setMusicIndex(-1);
            finish();
        }

    }

    @Override
    protected void onDestroy() {
        FileFactory.getInstance().removeMediaPlayerListener(this);
        myHandler.removeCallbacks(UpdateSongTime);
        super.onDestroy();
        Log.w(TAG, "onDestroy");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                doFinish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * INITIALIZATION
     */
    private void initData() {
        Bundle args = getIntent().getExtras();
        if (args != null) {
            mPath = args.getString("path");
            mMode = args.getString("mode");
            mRoot = args.getString("root");
            mList = FileFactory.getInstance().getMusicList();
            if (mList != null) {
                for (int i = 0; i < mList.size(); i++) {
                    if (mPath.equals(mList.get(i).path)) {
                        mCurrentIndex = i;
                        break;
                    }
                }
            }
        }
    }

    private void initHeaderBar() {
        mHeaderBar = (Toolbar) findViewById(R.id.music_header_bar);
        mHeaderBar.setTitle("");
        mHeaderBar.setNavigationIcon(R.drawable.ic_navigation_arrow_gray_24dp);
        setSupportActionBar(mHeaderBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void initPager() {
        mProgressView = (RelativeLayout) findViewById(R.id.music_progress_view);
        musicImage = (ImageView) findViewById(R.id.music_image);
        musicStart = (TextView) findViewById(R.id.music_start_time);
        musicEnd = (TextView) findViewById(R.id.music_end_time);
        musicTitle = (TextView) findViewById(R.id.music_title);
        musicAlbum = (TextView) findViewById(R.id.music_album);

        musicPrev = (ImageView) findViewById(R.id.music_previous);
        musicPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileFactory.getInstance().notifyMediaPlayerListener(FileFactory.MediaPlayerStatus.PREV);
            }
        });

        musicNext = (ImageView) findViewById(R.id.music_next);
        musicNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileFactory.getInstance().notifyMediaPlayerListener(FileFactory.MediaPlayerStatus.NEXT);
            }
        });

        musicPlay = (ImageView) findViewById(R.id.music_play);
        musicPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMediaPlayer != null) {
                    if (mMediaPlayer.isPlaying())
                        FileFactory.getInstance().notifyMediaPlayerListener(FileFactory.MediaPlayerStatus.PAUSE);
                    else
                        FileFactory.getInstance().notifyMediaPlayerListener(FileFactory.MediaPlayerStatus.PLAY);
                }
            }
        });

        musicMode = (ImageView) findViewById(R.id.music_mode);
        musicMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateMusicMode(false);
            }
        });

        musicSeekBar = (SeekBar) findViewById(R.id.music_seekbar);
        musicSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    double finalTime, time;
                    double startTime = progress;

                    if (mMediaPlayer != null) {
                        finalTime = mMediaPlayer.getDuration();
                        time = Math.min(startTime, finalTime);
                    } else {
                        time = startTime;
                    }

                    musicStart.setText(String.format("%02d:%02d",
                                    TimeUnit.MILLISECONDS.toMinutes((long) time),
                                    TimeUnit.MILLISECONDS.toSeconds((long) time) -
                                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
                                                    toMinutes((long) time)))
                    );
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                myHandler.removeCallbacks(UpdateSongTime);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mMediaPlayer != null) {
                    mMediaPlayer.seekTo(seekBar.getProgress());
                    myHandler.postDelayed(UpdateSongTime, 0);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        doFinish();
    }

    private void startMusicPlayer() {
        if(mMediaPlayer != null) {
            isPlay = true;
            myHandler.postDelayed(UpdateSongTime, 0);
            musicPlay.setImageResource(R.drawable.player_pause_button);
        }
    }

    private void pauseMusicPlayer() {
        if(mMediaPlayer != null) {
            myHandler.removeCallbacks(UpdateSongTime);
            musicPlay.setImageResource(R.drawable.player_play_button);
        }
    }

    private void updateMusicMode(boolean init) {
        if (init) {
            mMusicMode = NASPref.getMusicType(this);
            switch (mMusicMode) {
                case NORMAL:
                    musicMode.setImageResource(R.drawable.player_mode_normal);
                    break;
                case REPEAT:
                    musicMode.setImageResource(R.drawable.player_mode_repeat);
                    break;
                case ONE:
                    musicMode.setImageResource(R.drawable.player_mode_one);
                    break;
            }
        } else {
            switch (mMusicMode) {
                case NORMAL:
                    mMusicMode = MUSIC_MODE.REPEAT;
                    musicMode.setImageResource(R.drawable.player_mode_repeat);
                    break;
                case REPEAT:
                    mMusicMode = MUSIC_MODE.ONE;
                    musicMode.setImageResource(R.drawable.player_mode_one);
                    break;
                case ONE:
                    mMusicMode = MUSIC_MODE.NORMAL;
                    musicMode.setImageResource(R.drawable.player_mode_normal);
                    break;
            }
            NASPref.setMusicType(this, mMusicMode);
        }
    }

    private void updateStartTimeLabel() {
        if (mMediaPlayer != null) {
            try {
                double time = mMediaPlayer.getCurrentPosition();
                musicStart.setText(String.format("%02d:%02d",
                                TimeUnit.MILLISECONDS.toMinutes((long) time),
                                TimeUnit.MILLISECONDS.toSeconds((long) time) -
                                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
                                                toMinutes((long) time)))
                );
                musicSeekBar.setProgress((int) time);
            } catch (IllegalStateException e){
                e.printStackTrace();
                if(myHandler != null)
                    myHandler.removeCallbacks(UpdateSongTime);
            }
        }
    }

    private void updateEndTimeLabel() {
        if (mMediaPlayer != null) {
            double time = mMediaPlayer.getDuration();
            musicEnd.setText(String.format("%02d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes((long) time),
                            TimeUnit.MILLISECONDS.toSeconds((long) time) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) time)))
            );
            musicSeekBar.setMax((int) time);
        }
    }

    private void updateMusicInfo() {
        mMediaPlayer = FileFactory.getInstance().getMediaPlayer();
        mMediaMetadataRetriever = FileFactory.getInstance().getMediaMetadataRetriever();
        mCurrentIndex = FileFactory.getInstance().getMusicIndex();
        String title = mCurrentIndex >= 0 && mList.size() > mCurrentIndex ? mList.get(mCurrentIndex).name : "";
        if (title != null && !title.equals(""))
            musicTitle.setText(title);
        else
            musicTitle.setText(getString(R.string.unknown_title));

        if (mMediaMetadataRetriever != null) {
            try {
                String album = mMediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                if (album != null && !album.equals(""))
                    musicAlbum.setText(album);
                else
                    musicAlbum.setText(getString(R.string.unknown_album));

                byte[] bytes = mMediaMetadataRetriever.getEmbeddedPicture();
                if (bytes != null && bytes.length > 0) {
                    InputStream is = new ByteArrayInputStream(bytes);
                    Bitmap bm = BitmapFactory.decodeStream(is);
                    musicImage.setImageBitmap(bm);
                } else {
                    musicImage.setImageResource(R.drawable.player_album);
                }
            } catch (IllegalStateException e){
                e.printStackTrace();
                musicAlbum.setText(getString(R.string.unknown_album));
                musicImage.setImageResource(R.drawable.player_album);
            }
        } else {
            musicAlbum.setVisibility(View.GONE);
            musicImage.setImageResource(R.drawable.player_album);
        }

        updateStartTimeLabel();
        updateEndTimeLabel();
        startMusicPlayer();
        isPlay = true;
    }

    private void startMusicService() {
        Log.d(TAG, "Check service status");
        boolean isRunning = false;
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MusicService.class.getName().equals(service.service.getClassName())) {
                isRunning = true;
            }
        }

        Log.d(TAG, "Check service result : " + isRunning);
        if (!isRunning) {
            Intent intent = new Intent(this, MusicService.class);
            startService(intent);
        }
    }

    private void stopMusicService() {
        Log.d(TAG, "Check service status");
        boolean isRunning = false;
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MusicService.class.getName().equals(service.service.getClassName())) {
                isRunning = true;
            }
        }

        Log.d(TAG, "Check service result : " + isRunning);
        if (isRunning) {
            Intent intent = new Intent(this, MusicService.class);
            stopService(intent);
        }
    }

    private Runnable UpdateSongTime = new Runnable() {
        public void run() {
            updateStartTimeLabel();
            myHandler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onMusicChange(FileFactory.MediaPlayerStatus status) {
        Message m = new Message();
        m.what = status.ordinal();
        handler.sendMessage(m);
    }

    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            int index = msg.what;
            if (index == FileFactory.MediaPlayerStatus.LOAD.ordinal() ||
                    index == FileFactory.MediaPlayerStatus.NEXT.ordinal() ||
                    index == FileFactory.MediaPlayerStatus.PREV.ordinal()) {
                mProgressView.setVisibility(View.VISIBLE);
                pauseMusicPlayer();
            } else if (index == FileFactory.MediaPlayerStatus.NEW.ordinal()) {
                mProgressView.setVisibility(View.INVISIBLE);
                updateMusicInfo();
            } else if (index == FileFactory.MediaPlayerStatus.PLAY.ordinal()) {
                startMusicPlayer();
            } else if (index == FileFactory.MediaPlayerStatus.PAUSE.ordinal()) {
                pauseMusicPlayer();
                isPlay = false;
            } else if (index == FileFactory.MediaPlayerStatus.STOP.ordinal()) {
                mProgressView.setVisibility(View.INVISIBLE);
                pauseMusicPlayer();
                doFinish();
            } else if (index == FileFactory.MediaPlayerStatus.ERROR.ordinal()) {
                Toast.makeText(MusicActivity.this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                mProgressView.setVisibility(View.INVISIBLE);
                pauseMusicPlayer();
                doFinish();
            }
            super.handleMessage(msg);
        }
    };

    private void doFinish() {
        if(!isPlay)
            stopMusicService();

        Bundle bundle = new Bundle();
        bundle.putBoolean("delete", evenDelete);
        Intent intent = new Intent();
        intent.putExtras(bundle);
        setResult(RESULT_OK, intent);
        finish();
    }
}
