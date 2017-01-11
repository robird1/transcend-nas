package com.transcend.nas.management.externalstorage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by steve_su on 2017/1/10.
 */

public class SDCardReceiver extends BroadcastReceiver {
    private static final String TAG = SDCardReceiver.class.getSimpleName();
    private static List<SDCardObserver> mObserver = new ArrayList<>();

    public interface SDCardObserver {
        void notifyMounted();
        void notifyUnmounted();
    }

    public static void registerObserver(SDCardObserver observer) {
        mObserver.add(observer);
    }

    public static void unregisterObserver(SDCardObserver observer) {
        mObserver.remove(observer);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
//        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addDataScheme("file");

        switch (intent.getAction()) {
            case Intent.ACTION_MEDIA_MOUNTED:
                for (SDCardObserver o: mObserver) {
                    o.notifyMounted();
                }
                break;
            case Intent.ACTION_MEDIA_UNMOUNTED:
                for (SDCardObserver o: mObserver) {
                    o.notifyUnmounted();
                }
                break;
        }
    }

}
