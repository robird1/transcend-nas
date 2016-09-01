package com.transcend.nas;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.transcend.nas.connection.AutoLinkLoader;
import com.transcend.nas.connection.GuideActivity;
import com.transcend.nas.connection.NASListActivity;
import com.transcend.nas.connection.NASListLoader;
import com.transcend.nas.connection.StartActivity;
import com.transcend.nas.connection_new.IntroduceActivity;
import com.transcend.nas.connection_new.LoginActivity;
import com.transcend.nas.common.AnalysisFactory;
import com.transcend.nas.common.AnimFactory;
import com.transcend.nas.common.LoaderID;
import com.transcend.nas.connection_new.LoginByEmailActivity;
import com.transcend.nas.connection_new.LoginListActivity;
import com.transcend.nas.management.FileManageActivity;
import com.transcend.nas.management.TutkGetNasLoader;

import java.util.ArrayList;
import java.util.HashMap;

public class AutoLinkActivity extends Activity implements LoaderManager.LoaderCallbacks<Boolean> {

    private static final String TAG = AutoLinkActivity.class.getSimpleName();

    private TextView mTextView;
    private int mLoaderID = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        ImageView logo = (ImageView) findViewById(R.id.welcome_image);
        mTextView = (TextView) findViewById(R.id.welcome_text);
        mTextView.startAnimation(AnimFactory.getInstance().getBlinkAnimation());

        if (NASPref.useNewLoginFlow) {
            boolean isIntroduce = NASPref.getIntroduce(this);
            if (!isIntroduce) {
                startIntroduceActivity();
            } else {
                boolean isInit = NASPref.getInitial(this);
                if (isInit) {
                    getLoaderManager().initLoader(LoaderID.AUTO_LINK, null, this).forceLoad();
                } else {
                    startStartActivity();
                }
            }
        } else {
            boolean isInit = NASPref.getInitial(this);
            if (isInit) {
                AnalysisFactory.getInstance(this).sendScreen(AnalysisFactory.VIEW.AUTOLINK);
                getLoaderManager().initLoader(LoaderID.AUTO_LINK, null, this).forceLoad();
            } else {
                startGuideActivity();
            }
        }
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        AnalysisFactory.getInstance(this).recordStartTime();
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
        AnalysisFactory.getInstance(this).recordEndTime();
        if (loader instanceof AutoLinkLoader) {
            AnalysisFactory.getInstance(this).sendConnectEvent(AnalysisFactory.ACTION.AUTOLINK, success);
            AnalysisFactory.getInstance(this).sendTimeEvent(AnalysisFactory.EVENT.CONNECT, AnalysisFactory.ACTION.AUTOLINK, success);
            if (success) {
                boolean isWizard = ((AutoLinkLoader) loader).isWizard();
                if (isWizard)
                    startFileManageActivity();
                else
                    startNASListLoader();
            } else {
                startNASListLoader();
            }
            Log.w(TAG, "AutoLink " + success);
        }
        if (loader instanceof NASListLoader) {
            AnalysisFactory.getInstance(this).sendTimeEvent(AnalysisFactory.EVENT.CONNECT, AnalysisFactory.ACTION.FINDLOCAL, success);
            if (success)
                startNASFinderActivity(((NASListLoader) loader).getList(), false);
            else
                startRemoteAccessListLoader();
        }
        if (loader instanceof TutkGetNasLoader) {
            AnalysisFactory.getInstance(this).sendTimeEvent(AnalysisFactory.EVENT.CONNECT, AnalysisFactory.ACTION.FINDREMOTE, success);
            TutkGetNasLoader listLoader = (TutkGetNasLoader) loader;
            String code = listLoader.getCode();
            String status = listLoader.getStatus();
            if (success && code.equals(""))
                startNASFinderActivity(listLoader.getNasArrayList(), true);
            else {
                startStartActivity();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }

    private void startFileManageActivity() {
        Intent intent = new Intent();
        intent.setClass(AutoLinkActivity.this, FileManageActivity.class);
        startActivity(intent);
        finish();
    }

    private void startRemoteAccessListLoader() {
        String server = NASPref.getCloudServer(this);
        String token = NASPref.getCloudAuthToken(this);
        if (!token.equals("")) {
            Bundle args = new Bundle();
            args.putString("server", server);
            args.putString("token", token);
            getLoaderManager().restartLoader(LoaderID.TUTK_NAS_GET, args, this).forceLoad();
        } else {
            startStartActivity();
        }
    }

    private void startNASListLoader() {
        if (NASPref.useNewLoginFlow) {
            startRemoteAccessListLoader();
        } else {
            getLoaderManager().restartLoader(LoaderID.NAS_LIST, null, this).forceLoad();
        }
    }

    private void startGuideActivity() {
        Intent intent = new Intent();
        intent.setClass(AutoLinkActivity.this, GuideActivity.class);
        startActivity(intent);
        finish();
    }

    private void startStartActivity() {
        if (NASPref.useNewLoginFlow) {
            startLoginActivity();
        } else {
            Intent intent = new Intent();
            intent.setClass(AutoLinkActivity.this, StartActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void startNASFinderActivity(ArrayList<HashMap<String, String>> list, boolean isRemoteAccess) {
        if (NASPref.useNewLoginFlow) {
            startLoginListActivity(list, isRemoteAccess);
        } else {
            Intent intent = new Intent();
            intent.setClass(AutoLinkActivity.this, NASListActivity.class);
            intent.putExtra("NASList", list);
            intent.putExtra("RemoteAccess", isRemoteAccess);
            startActivity(intent);
            finish();
        }
    }

    //new page
    private void startIntroduceActivity() {
        Intent intent = new Intent();
        intent.setClass(AutoLinkActivity.this, IntroduceActivity.class);
        startActivity(intent);
        finish();
    }

    //new page
    private void startLoginActivity() {
        Intent intent = new Intent();
        intent.setClass(AutoLinkActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    //new page
    private void startLoginListActivity(ArrayList<HashMap<String, String>> list, boolean isRemoteAccess) {
        Intent intent = new Intent();
        intent.setClass(AutoLinkActivity.this, LoginListActivity.class);
        intent.putExtra("NASList", list);
        intent.putExtra("RemoteAccess", isRemoteAccess);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (mLoaderID >= 0) {
            getLoaderManager().destroyLoader(mLoaderID);
        }

        startStartActivity();
    }

}
