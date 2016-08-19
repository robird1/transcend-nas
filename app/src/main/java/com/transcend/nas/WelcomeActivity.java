package com.transcend.nas;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.transcend.nas.common.LoaderID;
import com.transcend.nas.connection.AutoLinkLoader;
import com.transcend.nas.connection.NASFinderActivity;
import com.transcend.nas.connection.NASListLoader;
import com.transcend.nas.connection.SignInActivity;
import com.transcend.nas.management.FileManageActivity;
import com.transcend.nas.management.TutkGetNasLoader;

import java.util.ArrayList;
import java.util.HashMap;

public class WelcomeActivity extends Activity implements LoaderManager.LoaderCallbacks<Boolean> {

    private static final String TAG = WelcomeActivity.class.getSimpleName();

    private TextView mTextView;
    private int mLoaderID = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        mTextView = (TextView) findViewById(R.id.welcome_text);
        boolean isInit = NASPref.getInitial(this);
        if(isInit) {
            getLoaderManager().initLoader(LoaderID.AUTO_LINK, null, this).forceLoad();
        }
        else{
            startInitialActivity();
        }
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch (mLoaderID = id) {
            case LoaderID.AUTO_LINK:
                mTextView.setText(getString(R.string.try_auto_connect));
                return new AutoLinkLoader(this);
            case LoaderID.NAS_LIST:
                mTextView.setText(getString(R.string.try_search_device));
                return new NASListLoader(this);
            case LoaderID.TUTK_NAS_GET:
                mTextView.setText(getString(R.string.try_remote_access));
                String server = args.getString("server");
                String token = args.getString("token");
                return new TutkGetNasLoader(this, server, token);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        if (loader instanceof AutoLinkLoader) {
            if (success) {
                boolean isWizard = ((AutoLinkLoader) loader).isWizard();
                if(isWizard)
                    startFileManageActivity();
                else {
                    startNASListLoader();
                    //AutoLinkLoader.LinkType type = ((AutoLinkLoader) loader).getLinkType();
                    //startWizardActivity(type == AutoLinkLoader.LinkType.INTERNET);
                }
            }
            else
                startNASListLoader();
            Log.w(TAG, "AutoLink " + success);
        }
        if (loader instanceof NASListLoader) {
            if (success)
                startNASFinderActivity(((NASListLoader) loader).getList(), false);
            else
                startRemoteAccessListLoader();
        }
        if (loader instanceof TutkGetNasLoader) {
            TutkGetNasLoader listLoader = (TutkGetNasLoader) loader;
            String code = listLoader.getCode();
            String status = listLoader.getStatus();

            if (success && code.equals(""))
                startNASFinderActivity(listLoader.getNasArrayList(), true);
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

    /*private void startWizardActivity(boolean isRemoteAccess) {
        Intent intent = new Intent();
        intent.setClass(WelcomeActivity.this, WizardActivity.class);
        String hostname = isRemoteAccess ? NASPref.getCloudUUID(this) : NASPref.getHostname(this);
        if(hostname != null && !hostname.equals("")) {
            intent.putExtra("Hostname", hostname);
            intent.putExtra("RemoteAccess", isRemoteAccess);
            startActivity(intent);
            finish();
        }
        else{
            startNASListLoader();
        }
    }*/

    private void startRemoteAccessListLoader() {
        String server = NASPref.getCloudServer(this);
        String token = NASPref.getCloudAuthToken(this);
        if (!token.equals("")) {
            Bundle args = new Bundle();
            args.putString("server", server);
            args.putString("token", token);
            getLoaderManager().restartLoader(LoaderID.TUTK_NAS_GET, args, this).forceLoad();
        }
        else
            startSignInActivity();
    }

    private void startNASListLoader() {
        getLoaderManager().restartLoader(LoaderID.NAS_LIST, null, this).forceLoad();
    }

    private void startNASFinderActivity(ArrayList<HashMap<String, String>> list, boolean isRemoteAccess) {
        Intent intent = new Intent();
        intent.setClass(WelcomeActivity.this, NASFinderActivity.class);
        intent.putExtra("NASList", list);
        intent.putExtra("RemoteAccess", isRemoteAccess);
        startActivity(intent);
        finish();
    }

    private void startSignInActivity() {
        Intent intent = new Intent();
        intent.setClass(WelcomeActivity.this, SignInActivity.class);
        startActivity(intent);
        finish();
    }

    private void startInitialActivity() {
        Intent intent = new Intent();
        intent.setClass(WelcomeActivity.this, InitialActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if(mLoaderID >= 0){
            getLoaderManager().destroyLoader(mLoaderID);
        }
        startSignInActivity();
    }

}
