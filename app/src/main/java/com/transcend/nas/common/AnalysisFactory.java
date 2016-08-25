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
    private static String KEYWORD_VIEW = "VIEW"; // mean new activity
    private static String KEYWORD_BUTTON = "BUTTON"; // mean click event
    private static String KEYWORD_EVENT = "EVENT"; // mean load event

    public static class VIEW {
        public static String AUTOLINK = "AutoLink";
        public static String START = "Start";
        public static String GUIDE = "Guide";
        public static String NASLISTLOCAL = "NasListLocal";
        public static String NASLISTREMOTE = "NasListRemote";
        public static String BROWSERLOCAL = "BrowserLocal";
        public static String BROWSERLOCALDOWNLOAD = "BrowserLocalDownload";
        public static String BROWSERREMOTE = "BrowserRemote";
    }

    public static class EVENT {
        public static String LOADDATA = "LoadData";
        public static String CONNECT = "Connect";
    }

    public static class ACTION {
        public static String CLICKBUTTON = "ClickButton";
        public static String AUTOLINK = "AutoLink";
        public static String AUTOLINKLOCAL = "AutoLinkLocal";
        public static String AUTOLINKREMOTE = "AutoLinkRemote";
        public static String STARTLOCAL = "StartLocal";
        public static String FINDLOCAL = "FindLocal";
        public static String LOGINLOCAL = "LoginLocal";
        public static String STARTREMOTE = "StartRemote";
        public static String FINDREMOTE = "FindRemote";
        public static String CHECKREMOTE = "CheckRemote";
        public static String LINKREMOTE = "LinkRemote";
        public static String LOGINREMOTE = "LoginRemote";
    }

    public static class LABEL {
        public static String SUCCESS = "Success";
        public static String FAIL = "Fail";
        public static String EMPTY = "EmptyInfo";
        public static String NOWIZARD = "NoWizard";
    }

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
        mTracker.setScreenName(KEYWORD_VIEW + "_" + screen);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    public void sendLoadDataEvent(String action, boolean success) {
        sendEvent(KEYWORD_EVENT + "_" + EVENT.LOADDATA, action, success ? LABEL.SUCCESS : LABEL.FAIL);
    }

    public void sendLoadDataEvent(String action, String label) {
        sendEvent(KEYWORD_EVENT + "_" + EVENT.LOADDATA, action, label);
    }

    public void sendConnectEvent(String action, boolean success) {
        sendEvent(KEYWORD_EVENT + "_" + EVENT.CONNECT, action, success ? LABEL.SUCCESS : LABEL.FAIL);
    }

    public void sendConnectEvent(String action, String label) {
        sendEvent(KEYWORD_EVENT + "_" + EVENT.CONNECT, action, label);
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

    public void sendClickEvent(String view, String label) {
        if (!enableAnalysis || mTracker == null)
            return;
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory(KEYWORD_BUTTON + "_" + view)
                .setAction(ACTION.CLICKBUTTON)
                .setLabel(label)
                .build());
    }

    public void sendTimeEvent(String event, String label, boolean success) {
        sendTimeEvent(event, label, getTimePeriod(), success);
    }

    public void sendTimeEvent(String event, String label, long value, boolean success) {
        if (!enableAnalysis || mTracker == null)
            return;

        mTracker.send(new HitBuilders.TimingBuilder()
                .setCategory(event)
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

    public long getTimePeriod(){
        long time = endTime - startTime;
        startTime = 0;
        endTime = 0;
        return time > 0 ? time : 0;
    }
}
