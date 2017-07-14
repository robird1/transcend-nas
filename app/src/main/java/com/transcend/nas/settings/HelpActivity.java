package com.transcend.nas.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

import com.transcend.nas.DrawerMenuActivity;
import com.transcend.nas.DrawerMenuController;
import com.transcend.nas.R;

import java.util.Locale;

/**
 * Created by steve_su on 2016/11/28.
 */

public class HelpActivity extends DrawerMenuActivity {
    private static final String TAG = HelpActivity.class.getSimpleName();
    private WebView mWebView;
    private RelativeLayout mProgress;

    @Override
    public int onLayoutID() {
        return R.layout.activity_drawer_help;
    }

    @Override
    public int onToolbarID() {
        return R.id.settings_toolbar;
    }

    @Override
    public DrawerMenuController.DrawerMenu onActivityDrawer() {
        return DrawerMenuController.DrawerMenu.DRAWER_DEFAULT;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.toggleDrawerCheckedItem();

        mProgress = (RelativeLayout) findViewById(R.id.settings_progress_view);
        mProgress.setVisibility(View.VISIBLE);
        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.setWebViewClient(new MyWebViewClient());
        mWebView.loadUrl(getUrl());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @NonNull
    private String getUrl() {
        Locale current = getResources().getConfiguration().locale;
        Log.d(TAG, "current locale: "+ current);

        String url = "http://help.storejetcloud.com/";
        if (current.equals(Locale.TRADITIONAL_CHINESE)) {
            url = url.concat("TW/");
        } else if (current.equals(Locale.JAPANESE)) {
            url = url.concat("JP/");
        } else if (current.equals(Locale.KOREAN)) {
            url = url.concat("KR/");
        } else {
            url = url.concat("EN/");
        }
        Log.d(TAG, "url: " + url);
        return url;
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
//            Log.d(TAG,"[Enter] onPageFinished() mWebView.getProgress(): " + mWebView.getProgress());
            if(mWebView.getProgress() == 100) {
                mProgress.setVisibility(View.INVISIBLE);
            }

        }
    }

}
