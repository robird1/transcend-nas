package com.transcend.nas;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.transcend.nas.common.GoogleAnalysisFactory;
import com.transcend.nas.connection.old.GuideActivity;
import com.transcend.nas.connection.old.NASListActivity;
import com.transcend.nas.connection.NASListLoader;
import com.transcend.nas.connection.old.StartActivity;
import com.transcend.nas.introduce.FirstUseActivity;
import com.transcend.nas.introduce.IntroduceActivity;
import com.transcend.nas.connection.LoginActivity;
import com.transcend.nas.common.AnimFactory;
import com.transcend.nas.connection.LoginHelper;
import com.transcend.nas.connection.LoginListActivity;
import com.transcend.nas.management.FileManageActivity;
import com.transcend.nas.tutk.TutkGetNasLoader;
import com.transcend.nas.tutk.TutkLinkNasLoader;
import com.tutk.IOTC.P2PService;

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
        mTextView.setText(getString(R.string.try_search_device));

        boolean isLicenseAgreed = NASPref.getIsLicenseAgreed(this);
        if(!isLicenseAgreed) {
            startActivity(new Intent(this, FirstUseActivity.class));
            finish();
            return;
        }

        if (NASPref.useNewLoginFlow) {
            boolean isIntroduce = NASPref.getIntroduce(this);
            if (!isIntroduce) {
                startIntroduceActivity();
            } else {
                String email = NASPref.getCloudUsername(this);
                String pwd = NASPref.getCloudPassword(this);
                String token = NASPref.getCloudAuthToken(this);
                if (!email.equals("") && !pwd.equals("") && !token.equals("")) {
                    Bundle args = getAccountInfo(false);
                    if (args != null) {
                        GoogleAnalysisFactory.getInstance(this).sendScreen(GoogleAnalysisFactory.VIEW.AUTO_LINK);
                        mTextView.startAnimation(AnimFactory.getInstance().getBlinkAnimation());
                        getLoaderManager().initLoader(LoaderID.AUTO_LINK, args, this).forceLoad();
                        return;
                    }
                }

                startStartActivity();
            }
        } else {
            boolean isInit = NASPref.getInitial(this);
            if (isInit) {
                Bundle args = getAccountInfo(false);
                getLoaderManager().initLoader(LoaderID.AUTO_LINK, args, this).forceLoad();
            } else {
                startGuideActivity();
            }
        }
    }

    @Override
    protected void onResume() {
        //TODO : check any loader running or not
        super.onResume();
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch (mLoaderID = id) {
            case LoaderID.AUTO_LINK:
                boolean isRemote = args.getBoolean("RemoteAccess");
                mTextView.setText(isRemote ? getString(R.string.try_auto_connect) : getString(R.string.try_search_device));
                return new AutoLinkLoader(this, args);
            case LoaderID.TUTK_NAS_LINK:
                mTextView.setText(getString(R.string.try_remote_access));
                return new TutkLinkNasLoader(this, args);
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
        Log.d("IKE", "onLoadFinished " + loader.getClass().toString());
        if (loader instanceof AutoLinkLoader) {
            boolean isWizard = ((AutoLinkLoader) loader).isWizard();
            boolean isRemote = ((AutoLinkLoader) loader).isRemote();
            if (success && isWizard) {
                GoogleAnalysisFactory.getInstance(this).sendEvent(GoogleAnalysisFactory.VIEW.AUTO_LINK, GoogleAnalysisFactory.ACTION.LoginNas,
                        (isRemote ? GoogleAnalysisFactory.LABEL.AutoLinkRemote : GoogleAnalysisFactory.LABEL.AutoLinkLan ) + "_" + GoogleAnalysisFactory.SUCCESS);
                startFileManageActivity();
            } else {
                GoogleAnalysisFactory.getInstance(this).sendEvent(GoogleAnalysisFactory.VIEW.AUTO_LINK, GoogleAnalysisFactory.ACTION.LoginNas,
                        (isRemote ? GoogleAnalysisFactory.LABEL.AutoLinkRemote : GoogleAnalysisFactory.LABEL.AutoLinkLan) + "_" + GoogleAnalysisFactory.FAIL);
                if (isRemote) {
                    if (NASPref.useNewLoginFlow)
                        startRemoteAccessListLoader();
                    else
                        startNASListLoader();
                } else {
                    startTutkNasLinkLoader();
                }
            }
        } else if (loader instanceof TutkLinkNasLoader) {
            if (success) {
                Bundle args = getAccountInfo(true);
                if(args != null)
                    getLoaderManager().restartLoader(LoaderID.AUTO_LINK, args, this).forceLoad();
                else
                    startStartActivity();
            } else {
                if (NASPref.useNewLoginFlow)
                    startRemoteAccessListLoader();
                else
                    startNASListLoader();
            }
        } else if (loader instanceof NASListLoader) {
            if (success)
                startNASFinderActivity(((NASListLoader) loader).getList(), false);
            else
                startRemoteAccessListLoader();
        } else if (loader instanceof TutkGetNasLoader) {
            TutkGetNasLoader listLoader = (TutkGetNasLoader) loader;
            String code = listLoader.getCode();
            String status = listLoader.getStatus();
            if (success && code.equals(""))
                startNASFinderActivity(listLoader.getNasArrayList(), true);
            else
                startStartActivity();
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }

    private Bundle getAccountInfo(boolean isRemote) {
        String hostname = P2PService.getInstance().getP2PIP() + ":" + P2PService.getInstance().getP2PPort(P2PService.P2PProtocalType.HTTP);
        if(NASPref.useNewLoginFlow) {
            LoginHelper loginHelper = new LoginHelper(this);
            LoginHelper.LoginInfo account = new LoginHelper.LoginInfo();
            account.email = NASPref.getCloudUsername(this);
            account.macAddress = NASPref.getMacAddress(this);
            boolean exist = loginHelper.getAccount(account);
            loginHelper.onDestroy();
            if (exist) {
                Bundle args = new Bundle();
                args.putString("hostname", isRemote ? hostname : account.ip);
                args.putString("username", account.username);
                args.putString("password", account.password);
                args.putBoolean("RemoteAccess", isRemote);
                return args;
            } else {
                return null;
            }
        } else {
            Bundle args = new Bundle();
            args.putString("hostname", isRemote ? hostname : NASPref.getLocalHostname(this));
            args.putString("username", NASPref.getUsername(this));
            args.putString("password", NASPref.getPassword(this));
            args.putBoolean("RemoteAccess", isRemote);
            return args;
        }
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

    private void startTutkNasLinkLoader() {
        Bundle args = new Bundle();
        String uuid = NASPref.getUUID(this);
        if (uuid == null || "".equals(uuid)) {
            uuid = NASPref.getCloudUUID(this);
            if (uuid == null || "".equals(uuid)) {
                if (NASPref.useNewLoginFlow) {
                    startRemoteAccessListLoader();
                } else {
                    startNASListLoader();
                }
                return;
            }
        }
        args.putString("hostname", uuid);
        getLoaderManager().restartLoader(LoaderID.TUTK_NAS_LINK, args, this).forceLoad();
    }

    private void startNASListLoader() {
        getLoaderManager().restartLoader(LoaderID.NAS_LIST, null, this).forceLoad();
    }

    private void startFileManageActivity() {
        mTextView.clearAnimation();
        Intent intent = new Intent();
        intent.setClass(AutoLinkActivity.this, FileManageActivity.class);
        startActivity(intent);
        finish();
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
        mTextView.clearAnimation();
        Intent intent = new Intent();
        intent.setClass(AutoLinkActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    //new page
    private void startLoginListActivity(ArrayList<HashMap<String, String>> list, boolean isRemoteAccess) {
        mTextView.clearAnimation();
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
