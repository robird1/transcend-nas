package com.transcend.nas;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerInfo;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.common.AnimFactory;
import com.transcend.nas.common.GoogleAnalysisFactory;
import com.transcend.nas.connection.LoginActivity;
import com.transcend.nas.connection.LoginHelper;
import com.transcend.nas.connection.LoginListActivity;
import com.transcend.nas.connection.NASListLoader;
import com.transcend.nas.introduce.FirstUseActivity;
import com.transcend.nas.introduce.IntroduceActivity;
import com.transcend.nas.management.FileManageActivity;
import com.transcend.nas.tutk.TutkGetNasLoader;
import com.transcend.nas.tutk.TutkLinkNasLoader;
import com.tutk.IOTC.P2PService;

import java.util.ArrayList;
import java.util.HashMap;

import static com.transcend.nas.NASPref.useBrowserMinFirmwareVersion;

public class AutoLinkActivity extends Activity implements LoaderManager.LoaderCallbacks<Boolean> {

    private static final String TAG = AutoLinkActivity.class.getSimpleName();

    private TextView mTextView;
    private int mLoaderID = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        boolean isFirstUse = NASPref.getIsFirstUse(this);
        if (isFirstUse) {
            startFirstUseActivity();
            return;
        }

        boolean isIntroduce = NASPref.getIntroduce(this);
        if (!isIntroduce) {
            startIntroduceActivity();
            return;
        }

        mTextView = (TextView) findViewById(R.id.welcome_text);
        mTextView.setText(getString(R.string.try_search_device));

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

        startLoginActivity();
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
                        (isRemote ? GoogleAnalysisFactory.LABEL.AutoLinkRemote : GoogleAnalysisFactory.LABEL.AutoLinkLan) + "_" + GoogleAnalysisFactory.SUCCESS);
                startFileManageActivity();
            } else {
                GoogleAnalysisFactory.getInstance(this).sendEvent(GoogleAnalysisFactory.VIEW.AUTO_LINK, GoogleAnalysisFactory.ACTION.LoginNas,
                        (isRemote ? GoogleAnalysisFactory.LABEL.AutoLinkRemote : GoogleAnalysisFactory.LABEL.AutoLinkLan) + "_" + GoogleAnalysisFactory.FAIL);
                if (isRemote) {
                    startRemoteAccessListLoader();
                } else {
                    startTutkNasLinkLoader();
                }
            }
        } else if (loader instanceof TutkLinkNasLoader) {
            if (success) {
                Bundle args = getAccountInfo(true);
                if (args != null)
                    getLoaderManager().restartLoader(LoaderID.AUTO_LINK, args, this).forceLoad();
                else
                    startLoginActivity();
            } else {
                startRemoteAccessListLoader();
            }
        } else if (loader instanceof NASListLoader) {
            if (success)
                startLoginListActivity(((NASListLoader) loader).getList(), false);
            else
                startRemoteAccessListLoader();
        } else if (loader instanceof TutkGetNasLoader) {
            TutkGetNasLoader listLoader = (TutkGetNasLoader) loader;
            String code = listLoader.getCode();
            String status = listLoader.getStatus();
            if (success && code.equals(""))
                startLoginListActivity(listLoader.getNasArrayList(), true);
            else
                startLoginActivity();
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }

    private Bundle getAccountInfo(boolean isRemote) {
        String hostname = P2PService.getInstance().getP2PIP() + ":" + P2PService.getInstance().getP2PPort(P2PService.P2PProtocalType.HTTP);
        LoginHelper loginHelper = new LoginHelper(this);
        LoginHelper.LoginInfo account = new LoginHelper.LoginInfo();
        account.email = NASPref.getCloudUsername(this);
        account.macAddress = NASPref.getMacAddress(this);
        boolean exist = loginHelper.getAccount(account);
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
            startLoginActivity();
        }
    }

    private void startTutkNasLinkLoader() {
        Bundle args = new Bundle();
        String uuid = NASPref.getUUID(this);
        if (uuid == null || "".equals(uuid)) {
            uuid = NASPref.getCloudUUID(this);
            if (uuid == null || "".equals(uuid)) {
                startRemoteAccessListLoader();
                return;
            }
        }
        args.putString("hostname", uuid);
        getLoaderManager().restartLoader(LoaderID.TUTK_NAS_LINK, args, this).forceLoad();
    }

    private void startFileManageActivity() {
        mTextView.clearAnimation();
        Intent intent = new Intent();

        Class navigationClass = FileManageActivity.class;
        Server server = ServerManager.INSTANCE.getCurrentServer();
        ServerInfo info = server.getServerInfo();
        NASPref.setFirmwareVersion(this, info.firmwareVer);
        int firmwareVer = Integer.valueOf(info.firmwareVer);
        if (firmwareVer >= useBrowserMinFirmwareVersion) {
//            navigationClass = BrowserActivity.class;
        }

        intent.setClass(AutoLinkActivity.this, navigationClass);
        startActivity(intent);
        finish();
    }

    private void startFirstUseActivity() {
        Intent intent = new Intent();
        intent.setClass(AutoLinkActivity.this, FirstUseActivity.class);
        startActivity(intent);
        finish();
    }

    private void startIntroduceActivity() {
        Intent intent = new Intent();
        intent.setClass(AutoLinkActivity.this, IntroduceActivity.class);
        startActivity(intent);
        finish();
    }

    private void startLoginActivity() {
        mTextView.clearAnimation();
        Intent intent = new Intent();
        intent.setClass(AutoLinkActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

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
        startLoginActivity();
    }

}
