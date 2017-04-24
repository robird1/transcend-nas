package com.transcend.nas.connection;

import android.net.Uri;
import android.util.Log;

import com.facebook.applinks.AppLinkData;

import bolts.AppLinks;

/**
 * Created by steve_su on 2017/3/15.
 */

public class DeepLinkFBInviteActivity extends AbstractInviteActivity {
    private static final String TAG = DeepLinkFBInviteActivity.class.getSimpleName();

    @Override
    protected void receiveInviteDeepLink() {
        Uri targetUrl = AppLinks.getTargetUrlFromInboundIntent(this, getIntent());
        if (targetUrl != null) {
            Log.d(TAG, "[Enter] targetUrl != null");
            Log.d(TAG, "App Link Target URL : " + targetUrl.toString());
            Log.d(TAG, " ");
            extractInviteData(targetUrl.toString());
        } else {
            Log.d(TAG, "[Enter] targetUrl == null");
            AppLinkData.fetchDeferredAppLinkData(this,
                    new AppLinkData.CompletionHandler() {
                        @Override
                        public void onDeferredAppLinkDataFetched(AppLinkData appLinkData) {
                            Log.d(TAG, "appLinkData.getTargetUri(): "+ appLinkData.getTargetUri().toString());
                            Log.d(TAG, " ");
                            extractInviteData(appLinkData.getTargetUri().toString());

                        }
                    });
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
//        finishAffinity();
    }
}
