package com.transcend.nas;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.facebook.FacebookSdk;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.android.libraries.cast.companionlibrary.cast.CastConfiguration;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.service.LanCheckManager;

import java.io.File;
import java.util.Locale;

/**
 * Created by silverhsu on 16/1/6.
 */
public class NASApp extends MultiDexApplication {

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
    public static final String ACT_PICK_UPLOAD = "PICKUPLOAD";
    public static final String ACT_PICK_DOWNLOAD = "PICKUPDOWNLOAD";

    public static final String TUTK_NAME_TAG     = "@tS#";

    private static Context mContext;
    private Tracker mTracker;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        initServerManager();
        initImageLoader();
        createDownloadsDirectory();
        createSharesDirectory();
        initChromeCastManager();

        FacebookSdk.sdkInitialize(getApplicationContext());
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
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

    private void createSharesDirectory() {
        String location = NASPref.getShareLocation(this);
        File directory = new File(location);
        directory.mkdirs();
        if (directory.exists()) return;
        File dir = new File(Environment.getExternalStorageDirectory(), getString(R.string.app_name));
        File shares = new File(dir, getString(R.string.shares_name));
        shares.mkdirs();
    }

    private void initChromeCastManager(){
        String applicationId = "A22AADD0";

        // Build a CastConfiguration object and initialize VideoCastManager
        CastConfiguration options = new CastConfiguration.Builder(applicationId)
                .enableAutoReconnect()
                .enableCaptionManagement()
                .enableDebug()
                .enableLockScreen()
                .enableNotification()
                .enableWifiReconnection()
                .setCastControllerImmersive(true)
                .setLaunchOptions(false, Locale.getDefault())
                .setNextPrevVisibilityPolicy(CastConfiguration.NEXT_PREV_VISIBILITY_POLICY_DISABLED)
                .addNotificationAction(CastConfiguration.NOTIFICATION_ACTION_REWIND, false)
                .addNotificationAction(CastConfiguration.NOTIFICATION_ACTION_PLAY_PAUSE, true)
                .addNotificationAction(CastConfiguration.NOTIFICATION_ACTION_DISCONNECT, true)
                .setForwardStep(10)
                .addNamespace("urn:x-cast:com.transcend.nas")
                .build();
        VideoCastManager.initialize(this, options);
    }

    /**
     * Gets the default {@link Tracker} for this {@link Application}.
     * @return tracker
     */
    synchronized public Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            mTracker = analytics.newTracker(R.xml.global_tracker);
        }
        return mTracker;
    }

    public static Context getContext() {
        return mContext;
    }

}
