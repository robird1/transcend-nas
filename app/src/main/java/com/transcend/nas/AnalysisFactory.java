package com.transcend.nas;

import android.app.Activity;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.management.FileInfo;
import com.tutk.IOTC.P2PService;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ike_lee on 2016/8/18.
 */
public class AnalysisFactory {
    private static final String TAG = AnalysisFactory.class.getSimpleName();
    private static AnalysisFactory mAnalysisFactory;
    private static final Object mMute = new Object();
    private static final boolean enableAnalysis = false;
    public static String KEYWORD_VIEW = "VIEW";
    public static String KEYWORD_EVENT = "EVENT";
    public static class CATEGORY_VIEW {
        public static String AUTOLINK = "AutoLink";
        public static String INITIAL = "Initial";
        public static String START = "Start";
        public static String NASLISTLOCAL = "NasListLocal";
        public static String NASLISTREMOTE = "NasListRemote";
        public static String BROWSERLOCAL = "BrowserLocal";
        public static String BROWSERLOCALDOWNLOAD = "BrowserLocalDownload";
        public static String BROWSERREMOTE = "BrowserRemote";
    }

    private Tracker mTracker;

    public AnalysisFactory(Activity activity) {
        mTracker = ((NASApp) activity.getApplication()).getDefaultTracker();
    }

    public static AnalysisFactory getInstance(Activity activity) {
        synchronized (mMute) {
            if (mAnalysisFactory == null)
                mAnalysisFactory = new AnalysisFactory(activity);
        }
        return mAnalysisFactory;
    }

    public void sendScreen(String screen){
        if(!enableAnalysis || mTracker == null)
            return;
        mTracker.setScreenName(KEYWORD_VIEW + "_" + screen);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    public void sendEvent(String category, String action){
        if(!enableAnalysis || mTracker == null)
            return;
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .build());
    }
}
