package com.transcend.nas.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.transcend.nas.NASUtils;
import com.transcend.nas.R;

import java.util.Locale;

/**
 * Created by steve_su on 2016/11/28.
 */

public class HelpActivity extends AppCompatActivity {
    private static final String TAG = HelpActivity.class.getSimpleName();
    private WebView mWebView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        initToolbar();
        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.setWebViewClient(new MyWebViewClient());
        NASUtils.showProgressBar(this, true);
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

    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.settings_toolbar);
        toolbar.setTitle("");
        toolbar.setNavigationIcon(R.drawable.ic_navigation_arrow_white_24dp);
        setSupportActionBar(toolbar);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @NonNull
    private String getUrl() {
        Locale current = getResources().getConfiguration().locale;
        Log.d(TAG, "current locale: "+ current);

        String url = "http://help.storejetcloud.com/";
        if (current.equals(Locale.TRADITIONAL_CHINESE)) {
            url = url.concat("TW");
        } else {
            url = url.concat("EN");
        }
        url = url.concat("/start.html");
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
                NASUtils.showProgressBar(HelpActivity.this, false);
            }

        }
    }

}
