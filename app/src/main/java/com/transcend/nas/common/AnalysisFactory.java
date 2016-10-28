package com.transcend.nas.common;

import android.app.Activity;
import android.content.Context;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.transcend.nas.NASApp;

/**
 * Created by ike_lee on 2016/8/18.
 */
public class AnalysisFactory {
    private static final String TAG = AnalysisFactory.class.getSimpleName();
    private static AnalysisFactory mAnalysisFactory;
    private static final Object mMute = new Object();
    private static final boolean enableAnalysis = true;

    public static class VIEW {
        public static String AUTO_LINK = "AutoLinkActivity";
        public static String INTRODUCE = "IntroduceActivity";
        public static String START = "LoginActivity";
        public static String START_EMAIL_LOGIN = "LoginByEmailActivity";
        public static String START_NAS_LIST = "LoginListActivity";
        public static String BROWSER_LOCAL = "LocalFileManageActivity";
        public static String BROWSER_LOCAL_DOWNLOAD = "LocalDownloadFileManageActivity";
        public static String BROWSER_REMOTE = "NasFileManageActivity";
    }

    public static class ACTION {
        public static String Click = "Click";
        public static String LoginTutk = "LoginTutk";
        public static String LoginNas = "LoginNas";
    }

    public static class LABEL {
        public static String LoginByStart = "LoginByStart";
        public static String LoginByFacebook = "LoginByFacebook";
        public static String LoginByEmail = "LoginByEmail";
        public static String RegisterEmail = "RegisterEmail";
        public static String ResendEmail = "ResendEmail";
        public static String ForgetPassword = "ForgetPassword";
        public static String Logout = "Logout";
        public static String AutoLinkLan = "AutoLinkLan";
        public static String AutoLinkRemote = "AutoLinkRemote";
        public static String LoginByAdmin = "LoginByAdmin";
        public static String LoginByNonAdmin = "LoginByNonAdmin";
    }

    public static String SUCCESS = "Success";
    public static String FAIL = "Fail";

    private long startTime = 0;
    private long endTime = 0;
    private Tracker mTracker;

    public AnalysisFactory(Activity activity) {
        mTracker = ((NASApp) activity.getApplication()).getDefaultTracker();
    }

    public static AnalysisFactory getInstance(Context context) {
        synchronized (mMute) {
            if (mAnalysisFactory == null)
                mAnalysisFactory = new AnalysisFactory((Activity) context);
        }
        return mAnalysisFactory;
    }

    public static AnalysisFactory getInstance(Activity activity) {
        synchronized (mMute) {
            if (mAnalysisFactory == null)
                mAnalysisFactory = new AnalysisFactory(activity);
        }
        return mAnalysisFactory;
    }

    public void sendScreen(String screen) {
        if (!enableAnalysis || mTracker == null)
            return;
        mTracker.setScreenName(screen);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    public void sendEvent(String category, String action, String label) {
        if (!enableAnalysis || mTracker == null)
            return;
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label)
                .build());
    }

    public void sendTimeEvent(String category, String label, long value, boolean success) {
        if (!enableAnalysis || mTracker == null)
            return;
        mTracker.send(new HitBuilders.TimingBuilder()
                .setCategory(category)
                .setLabel(label + "_" + (success ? "PASS" : "FAIL"))
                .setValue(value)
                .build());
    }

    public void recordStartTime(){
        startTime = System.currentTimeMillis();
    }

    public void recordEndTime(){
        endTime = System.currentTimeMillis();
    }

    private long getTimePeriod(){
        long time = endTime - startTime;
        startTime = 0;
        endTime = 0;
        return time > 0 ? time : 0;
    }
}
