package com.transcend.nas;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.transcend.nas.common.LoaderID;
import com.transcend.nas.connection.AutoLinkLoader;
import com.transcend.nas.connection.NASFinderActivity;
import com.transcend.nas.connection.NASListLoader;
import com.transcend.nas.connection.SignInActivity;
import com.transcend.nas.management.FileManageActivity;

import java.util.ArrayList;
import java.util.HashMap;

public class WelcomeActivity extends Activity implements LoaderManager.LoaderCallbacks<Boolean> {

    private static final String TAG = WelcomeActivity.class.getSimpleName();

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        mTextView = (TextView)findViewById(R.id.welcome_text);
        getLoaderManager().initLoader(LoaderID.AUTO_LINK, null, this).forceLoad();
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LoaderID.AUTO_LINK:
                mTextView.setText("嘗試自動連線");
                return new AutoLinkLoader(this);
            case LoaderID.NAS_LIST:
                mTextView.setText("正在尋找裝置");
                return new NASListLoader(this);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        if (loader instanceof AutoLinkLoader) {
            if (success)
                startFileManageActivity();
            else
                startNASListLoader();
            Log.w(TAG, "AutoLink " + success);
        }
        if (loader instanceof NASListLoader) {
            if (success)
                startNASFinderActivity(((NASListLoader)loader).getList());
            else
                startSignInActivity();
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }

    private void startFileManageActivity() {
        Intent intent = new Intent();
        intent.setClass(WelcomeActivity.this, FileManageActivity.class);
        startActivity(intent);
        finish();
    }

    private void startNASListLoader() {
        getLoaderManager().restartLoader(LoaderID.NAS_LIST, null, this).forceLoad();
    }

    private void startNASFinderActivity(ArrayList<HashMap<String, String>> list) {
        Intent intent = new Intent();
        intent.setClass(WelcomeActivity.this, NASFinderActivity.class);
        intent.putExtra("NASList", list);
        startActivity(intent);
        finish();
    }

    private void startSignInActivity() {
        Intent intent = new Intent();
        intent.setClass(WelcomeActivity.this, SignInActivity.class);
        startActivity(intent);
        finish();
    }

}
