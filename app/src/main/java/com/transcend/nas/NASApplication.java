package com.transcend.nas;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.realtek.nasfun.api.ServerManager;

import java.io.File;

/**
 * Created by silverhsu on 16/1/6.
 */
public class NASApplication extends Application {

    private static final String TAG = NASApplication.class.getSimpleName();

    private  static Context mContext;

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
        ImageLoaderConfiguration config = ImageLoaderConfiguration.createDefault(this);
        ImageLoader.getInstance().init(config);
    }


    private void initServerManager() {
        ServerManager.INSTANCE.setPreference(getSharedPreferences(TAG, Context.MODE_PRIVATE));
    }

    private void createDownloadsDirectory() {
        String downloadsPath = NASPref.getDownloadsPath(mContext);
        if (downloadsPath != null) {
            File file = new File(downloadsPath);
            if (file.exists())
                return;
        }
        String appName = getResources().getString(R.string.app_name);
        String downloads = getResources().getString(R.string.downloads_name);
        File storageDirectory = Environment.getExternalStorageDirectory();
        File appDirectory = new File(storageDirectory, appName);
        appDirectory.mkdirs();
        File appDownloadsDirectory = new File(appDirectory, downloads);
        appDownloadsDirectory.mkdirs();
        if (appDownloadsDirectory.exists()) {
            downloadsPath = appDownloadsDirectory.getPath();
            NASPref.setDownloadsPath(mContext, downloadsPath);
        }
    }

    public static Context getContext() {
        return mContext;
    }
}
