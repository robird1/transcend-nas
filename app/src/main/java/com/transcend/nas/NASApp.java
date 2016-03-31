package com.transcend.nas;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.service.AutoBackupService;

import java.io.File;

/**
 * Created by silverhsu on 16/1/6.
 */
public class NASApp extends Application {

    private static final String TAG = NASApp.class.getSimpleName();

    public static final String ROOT_SMB = "/";
    public static final String ROOT_STG = Environment.getExternalStorageDirectory().getAbsolutePath();

    public static final String MODE_SMB = "SMB";
    public static final String MODE_STG = "STG";

    public static final String ACT_COPY     = "COPY";
    public static final String ACT_MOVE     = "MOVE";
    public static final String ACT_DIRECT   = "DIRECT";
    public static final String ACT_UPLOAD   = "UPLOAD";
    public static final String ACT_DOWNLOAD = "DOWNLOAD";

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        initServerManager();
        initImageLoader();
        createDownloadsDirectory();

        // TODO: P2P case
        /*
        P2PService.getInstance().P2PConnectStop();
        boolean b = P2PService.getInstance().P2PConnectStart();
        boolean c = P2PService.getInstance().isConnected();
        */

    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        // TODO: P2P case
        //P2PService.getInstance().P2PConnectStop();
    }

    private void initImageLoader() {
        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
                .defaultDisplayImageOptions(options)
                //.diskCacheSize(104857600)
                .build();
        ImageLoader.getInstance().init(config);
    }

    private void initServerManager() {
        ServerManager.INSTANCE.setPreference(getSharedPreferences(TAG, Context.MODE_PRIVATE));
    }

    private void createDownloadsDirectory() {
        String location = NASPref.getDownloadLocation(this);
        File directory = new File(location);
        directory.mkdirs();
        if (directory.exists()) return;
        File dir = new File(Environment.getExternalStorageDirectory(), getString(R.string.app_name));
        File downloads = new File(dir, getString(R.string.downloads_name));
        if (downloads.mkdirs()) NASPref.setDownloadLocation(this, downloads.getAbsolutePath());
    }

    public static Context getContext() {
        return mContext;
    }

}
