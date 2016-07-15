package com.transcend.nas.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.storage.StorageManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.cache.disc.DiskCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.utils.DiskCacheUtils;
import com.nostra13.universalimageloader.utils.StorageUtils;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.common.LoaderID;
import com.transcend.nas.common.NotificationDialog;
import com.transcend.nas.common.TutkCodeID;
import com.transcend.nas.connection.BindDialog;
import com.transcend.nas.connection.ForgetPwdDialog;
import com.transcend.nas.connection.LoginLoader;
import com.transcend.nas.management.AutoBackupLoader;
import com.transcend.nas.management.FileActionLocateActivity;
import com.transcend.nas.management.SmbFolderCreateLoader;
import com.transcend.nas.management.TutkCreateNasLoader;
import com.transcend.nas.management.TutkForgetPasswordLoader;
import com.transcend.nas.management.TutkGetNasLoader;
import com.transcend.nas.management.TutkLoginLoader;
import com.transcend.nas.management.TutkRegisterLoader;
import com.transcend.nas.management.TutkResendActivateLoader;
import com.transcend.nas.service.AutoBackupService;
import com.transcend.nas.utils.FileFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by silverhsu on 16/3/2.
 */
public class SettingsActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Boolean> {

    public static final String TAG = SettingsActivity.class.getSimpleName();

    private RelativeLayout mProgressView;

    private int mLoaderID;

    private boolean isInitRemoteAccessCheck = true;
    private boolean isSubFragment = false;
    private boolean isInitFragment = true;
    private boolean isRemoteAccessRegister = false;
    private boolean isRemoteAccessActive = false;
    private boolean isRemoteAccessCheck = false;
    private int scenerioType = -1;
    private boolean isStartService = false;
    private TextView mTitle = null;
    private List<TutkGetNasLoader.TutkNasNode> naslist;
    private SettingsFragment mSettingsFragment;
    private BindDialog mBindDialog;
    private ForgetPwdDialog mForgetDialog;

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_settings);
        initToolbar();
        showSettingFragment();
        initProgressView();
        boolean checked = NASPref.getBackupSetting(mContext);
        String path = NASPref.getBackupLocation(mContext);
        if(checked && path != null && !path.equals("")){
            Bundle arg = new Bundle();
            arg.putString("path", path);
            getLoaderManager().restartLoader(LoaderID.SMB_NEW_FOLDER, arg, this).forceLoad();
        }
        else{
            if(isInitRemoteAccessCheck){
                String email = NASPref.getCloudUsername(mContext);
                String pwd = NASPref.getCloudPassword(mContext);
                if (!email.equals("") && !pwd.equals("")) {
                    Bundle arg = new Bundle();
                    arg.putString("server", NASPref.getCloudServer(mContext));
                    arg.putString("email", email);
                    arg.putString("password", pwd);
                    getLoaderManager().restartLoader(LoaderID.TUTK_LOGIN, arg, SettingsActivity.this).forceLoad();
                }
                else
                    isInitRemoteAccessCheck = false;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isSubFragment)
            getMenuInflater().inflate(R.menu.nas_finder, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (isSubFragment)
                    if(isRemoteAccessRegister)
                        showSettingFragment();
                    else{
                        if(!isInitFragment)
                            showRemoteAccessFragment(getString(R.string.remote_access));
                        else
                            showSettingFragment();
                    }
                else
                    finish();
                break;
            case R.id.action_refresh_nas_finder:
                String email = NASPref.getCloudUsername(mContext);
                String pwd = NASPref.getCloudPassword(mContext);
                if (isRemoteAccessRegister && !email.equals("") && !pwd.equals("")) {
                    isRemoteAccessRegister = true;
                    isRemoteAccessActive = false;
                    Bundle arg = new Bundle();
                    arg.putString("server", NASPref.getCloudServer(mContext));
                    arg.putString("email", email);
                    arg.putString("password", pwd);
                    getLoaderManager().restartLoader(LoaderID.TUTK_LOGIN, arg, SettingsActivity.this).forceLoad();
                } else {
                    Toast.makeText(this, getString(R.string.remote_access_no_login), Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mForgetDialog != null) {
            getLoaderManager().destroyLoader(LoaderID.TUTK_FORGET_PASSWORD);
            mForgetDialog.dismiss();
            mForgetDialog = null;
        }

        if (mBindDialog != null) {
            getLoaderManager().destroyLoader(mLoaderID);
            mBindDialog.dismiss();
            mBindDialog = null;
        }

        if (mProgressView.isShown()) {
            getLoaderManager().destroyLoader(mLoaderID);
            mProgressView.setVisibility(View.INVISIBLE);
        } else {
            if (isSubFragment)
                if(isRemoteAccessRegister)
                    showSettingFragment();
                else{
                    if(!isInitFragment)
                        showRemoteAccessFragment(getString(R.string.remote_access));
                    else
                        showSettingFragment();
                }
            else
                finish();
        }
    }

    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        if(id != LoaderID.LOGIN)
            mProgressView.setVisibility(View.VISIBLE);

        String server, email, pwd, token;

        switch (mLoaderID = id) {
            case LoaderID.SMB_NEW_FOLDER:
                String path = args.getString("path");
                return new SmbFolderCreateLoader(this, path);
            case LoaderID.TUTK_REGISTER:
                server = args.getString("server");
                email = args.getString("email");
                pwd = args.getString("password");
                return new TutkRegisterLoader(this, server, email, pwd);
            case LoaderID.TUTK_LOGIN:
                server = args.getString("server");
                email = args.getString("email");
                pwd = args.getString("password");
                return new TutkLoginLoader(this, server, email, pwd);
            case LoaderID.TUTK_NAS_CREATE:
                server = args.getString("server");
                token = args.getString("token");
                String nasName = args.getString("nasName");
                String nasUUID = args.getString("nasUUID");
                return new TutkCreateNasLoader(this, server, token, nasName, nasUUID);
            case LoaderID.TUTK_NAS_GET:
                server = args.getString("server");
                token = args.getString("token");
                return new TutkGetNasLoader(this, server, token);
            case LoaderID.TUTK_NAS_RESEND_ACTIVATE:
                server = args.getString("server");
                email = args.getString("email");
                return new TutkResendActivateLoader(this, server, email);
            case LoaderID.TUTK_FORGET_PASSWORD:
                server = NASPref.getCloudServer(this);
                email = args.getString("email");
                return new TutkForgetPasswordLoader(this, server, email);
            case LoaderID.AUTO_BACKUP:
                return new AutoBackupLoader(this);
            case LoaderID.LOGIN:
                return new LoginLoader(this, args, false);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        if(loader instanceof SmbFolderCreateLoader){
            if(isStartService){
                Bundle arg = new Bundle();
                getLoaderManager().restartLoader(LoaderID.AUTO_BACKUP, arg, this).forceLoad();
                isStartService = false;
                return;
            }
            else {
                if(isInitRemoteAccessCheck){
                    String email = NASPref.getCloudUsername(mContext);
                    String pwd = NASPref.getCloudPassword(mContext);
                    if (!email.equals("") && !pwd.equals("")) {
                        Bundle arg = new Bundle();
                        arg.putString("server", NASPref.getCloudServer(mContext));
                        arg.putString("email", email);
                        arg.putString("password", pwd);
                        getLoaderManager().restartLoader(LoaderID.TUTK_LOGIN, arg, SettingsActivity.this).forceLoad();
                        return;
                    }
                }

                mProgressView.setVisibility(View.INVISIBLE);
                return;
            }
        }

        if (!success) {
            if(!isInitRemoteAccessCheck) {
                if (loader instanceof LoginLoader) {
                    if (mBindDialog != null)
                        mBindDialog.hideProgress();
                    Toast.makeText(this, ((LoginLoader) loader).getLoginError(), Toast.LENGTH_SHORT).show();
                } else {
                    mProgressView.setVisibility(View.INVISIBLE);
                    Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                }
            }
            mProgressView.setVisibility(View.INVISIBLE);
            isInitRemoteAccessCheck = false;
            return;
        }

        if (loader instanceof TutkForgetPasswordLoader) {
            checkForgetPasswordResult((TutkForgetPasswordLoader) loader);
        } else if (loader instanceof TutkRegisterLoader) {
            checkRegisterNASResult((TutkRegisterLoader) loader);
        } else if (loader instanceof TutkLoginLoader) {
            checkLoginNASResult((TutkLoginLoader) loader);
        } else if (loader instanceof TutkCreateNasLoader) {
            checkCreateNASResult((TutkCreateNasLoader) loader);
        } else if (loader instanceof TutkGetNasLoader) {
            checkGetNASResult((TutkGetNasLoader) loader);
        } else if (loader instanceof TutkResendActivateLoader) {
            checkResendActivateResult((TutkResendActivateLoader) loader);
        } else if (loader instanceof AutoBackupLoader) {
            mProgressView.setVisibility(View.INVISIBLE);
        } else if (loader instanceof LoginLoader) {
            checkLoginResult((LoginLoader) loader);
        }
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {

    }

    private void checkForgetPasswordResult(TutkForgetPasswordLoader loader) {
        mProgressView.setVisibility(View.INVISIBLE);
        String code = loader.getCode();
        String status = loader.getStatus();

        if (mForgetDialog != null)
            mForgetDialog.hideProgress();

        if (code.equals(TutkCodeID.SUCCESS)) {
            if (mForgetDialog != null) {
                mForgetDialog.dismiss();
                mForgetDialog = null;
            }

            NASPref.setCloudAccountStatus(this, NASPref.Status.Inactive.ordinal());
            NASPref.setCloudPassword(this, "");
            NASPref.setCloudAuthToken(this, "");
            NASPref.setCloudUUID(this, "");
            String[] scenarios = getResources().getStringArray(R.array.backup_scenario_values);
            NASPref.setBackupScenario(this, scenarios[1]);
            Toast.makeText(this, getString(R.string.forget_password_send), Toast.LENGTH_SHORT).show();
        } else {
            if (!code.equals(""))
                Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, getString(R.string.error_format), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkRegisterNASResult(TutkRegisterLoader loader) {
        String status = loader.getStatus();
        String code = loader.getCode();

        if ((code.equals(TutkCodeID.SUCCESS))) {
            Bundle arg = new Bundle();
            arg.putString("server", NASPref.getCloudServer(mContext));
            arg.putString("email", loader.getEmail());
            arg.putString("password", loader.getPassword());
            getLoaderManager().restartLoader(LoaderID.TUTK_LOGIN, arg, this).forceLoad();
        } else {
            mProgressView.setVisibility(View.INVISIBLE);
            if (!code.equals(""))
                Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, getString(R.string.error_format), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkLoginNASResult(TutkLoginLoader loader) {
        String code = loader.getCode();
        String status = loader.getStatus();
        String token = loader.getAuthToke();
        String email = loader.getEmail();
        String pwd = loader.getPassword();

        if (!token.equals("")) {
            //token not null mean login success
            isRemoteAccessRegister = true;
            isRemoteAccessActive = true;
            NASPref.setCloudAccountStatus(mContext, NASPref.Status.Active.ordinal());
            NASPref.setCloudUsername(mContext, email);
            NASPref.setCloudPassword(mContext, pwd);
            NASPref.setCloudAuthToken(mContext, loader.getAuthToke());
            Bundle arg = new Bundle();
            arg.putString("server", loader.getServer());
            arg.putString("token", loader.getAuthToke());
            getLoaderManager().restartLoader(LoaderID.TUTK_NAS_GET, arg, SettingsActivity.this).forceLoad();
        } else {
            setAutoBackupToWifi();
            mProgressView.setVisibility(View.INVISIBLE);
            if (code.equals(TutkCodeID.NOT_VERIFIED)) {
                //account not verified
                isRemoteAccessRegister = true;
                isRemoteAccessActive = false;
                NASPref.setCloudAccountStatus(mContext, NASPref.Status.Padding.ordinal());
                NASPref.setCloudUsername(mContext, email);
                NASPref.setCloudPassword(mContext, pwd);
            } else {
                isRemoteAccessRegister = false;
                isRemoteAccessActive = false;
                if (!code.equals(""))
                    Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, getString(R.string.error_format), Toast.LENGTH_SHORT).show();

                if (isSubFragment)
                    return;
            }

            if(!isInitRemoteAccessCheck)
                showRemoteAccessFragment(getString(R.string.remote_access));
            isInitRemoteAccessCheck = false;
        }
    }

    private void checkCreateNASResult(TutkCreateNasLoader loader) {
        String status = loader.getStatus();
        String code = loader.getCode();
        if (mBindDialog != null)
            mBindDialog.hideProgress();

        if (code.equals("") || code.equals(TutkCodeID.SUCCESS) || code.equals(TutkCodeID.UID_ALREADY_TAKEN)) {
            if (mBindDialog != null) {
                mBindDialog.dismiss();
                mBindDialog = null;
            }
            Bundle arg = new Bundle();
            arg.putString("server", loader.getServer());
            arg.putString("token", loader.getAuthToken());
            getLoaderManager().restartLoader(LoaderID.TUTK_NAS_GET, arg, SettingsActivity.this).forceLoad();
        } else {
            setAutoBackupToWifi();
            Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
        }
    }

    private void checkGetNASResult(TutkGetNasLoader loader) {
        String status = loader.getStatus();
        String code = loader.getCode();

        if (code.equals("")) {
            naslist = loader.getNasList();
            //check nas uuid and record it
            Server mServer = ServerManager.INSTANCE.getCurrentServer();
            String uuid = mServer.getTutkUUID();
            if(uuid == null){
                uuid = NASPref.getUUID(mContext);
            }

            boolean isBind = false;
            for(TutkGetNasLoader.TutkNasNode nas : naslist){
                if(nas.nasUUID.equals(uuid)){
                    NASPref.setCloudUUID(mContext, uuid);
                    NASPref.setCloudAccountStatus(mContext, NASPref.Status.Bind.ordinal());
                    isBind = true;
                    break;
                }
            }

            if(!isBind){
                NASPref.setCloudAccountStatus(mContext, NASPref.Status.Active.ordinal());
            }

            if(!isInitRemoteAccessCheck) {
                if (isRemoteAccessCheck) {
                    if (isBind) {
                        if (mSettingsFragment == null)
                            showSettingFragment();
                        else
                            mSettingsFragment.refreshColumnBackupScenario(false, false);
                    } else {
                        showRemoteAccessFragment(getString(R.string.remote_access));
                        setAutoBackupToWifi();
                        Toast.makeText(this, getString(R.string.remote_access_always_warning), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    showRemoteAccessFragment(getString(R.string.remote_access));
                }
            }
        } else {
            if(!isInitRemoteAccessCheck) {
                setAutoBackupToWifi();
                Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
            }
        }

        if(isInitRemoteAccessCheck){
            if (mSettingsFragment == null)
                showSettingFragment();
            else
                mSettingsFragment.refreshColumnRemoteAccessSetting();
        }

        isInitRemoteAccessCheck = false;
        mProgressView.setVisibility(View.INVISIBLE);
    }

    private void checkResendActivateResult(TutkResendActivateLoader loader) {
        String status = loader.getStatus();
        String code = loader.getCode();
        if (code.equals(TutkCodeID.SUCCESS))
            Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
        else if (code.equals(TutkCodeID.VERIFICATIONEXPIRED)) {
            isRemoteAccessRegister = false;
            isRemoteAccessActive = false;
            showRemoteAccessFragment(getString(R.string.remote_access));
        } else
            Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
        mProgressView.setVisibility(View.INVISIBLE);
    }

    private void checkLoginResult(LoginLoader loader){
        Bundle args = loader.getBundleArgs();
        getLoaderManager().restartLoader(LoaderID.TUTK_NAS_CREATE, args, SettingsActivity.this).forceLoad();
    }

    private void setAutoBackupToWifi() {
        String[] scenarios = mContext.getResources().getStringArray(R.array.backup_scenario_values);
        NASPref.setBackupScenario(mContext, scenarios[1]);
    }

    private void showNotificationDialog(String title, final int loaderID, final Bundle args) {
        Bundle value = new Bundle();
        value.putString(NotificationDialog.DIALOG_MESSAGE, title);
        NotificationDialog mNotificationDialog = new NotificationDialog(this, value) {
            @Override
            public void onConfirm() {
                if (loaderID == LoaderID.TUTK_LOGOUT) {
                    NASPref.setCloudAccountStatus(mContext, NASPref.Status.Inactive.ordinal());
                    NASPref.setCloudPassword(mContext, "");
                    NASPref.setCloudAuthToken(mContext, "");
                    NASPref.setCloudUUID(mContext, "");
                    String[] scenarios = mContext.getResources().getStringArray(R.array.backup_scenario_values);
                    NASPref.setBackupScenario(mContext, scenarios[1]);
                    isRemoteAccessRegister = false;
                    isRemoteAccessActive = false;
                    //TODO: check show setting or remote access
                    showRemoteAccessFragment(getString(R.string.remote_access));
                } else
                    getLoaderManager().restartLoader(loaderID, args, SettingsActivity.this).forceLoad();
            }

            @Override
            public void onCancel() {

            }
        };
    }


    /**
     * INITIALIZATION
     */
    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.settings_toolbar);
        toolbar.setTitle("");
        toolbar.setNavigationIcon(R.drawable.ic_navigation_arrow_gray_24dp);
        mTitle = (TextView) toolbar.findViewById(R.id.settings_title);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void showSettingFragment() {
        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                mTitle.setText(getString(R.string.settings));
                isSubFragment = false;
                if(mSettingsFragment == null)
                    mSettingsFragment = new SettingsFragment();
                getFragmentManager().beginTransaction().replace(R.id.settings_frame, mSettingsFragment).commit();
                invalidateOptionsMenu();
            }
        };
        handler.sendEmptyMessage(0);
    }

    private void showRemoteAccessFragment(final String title) {
        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                mTitle.setText(title);
                Fragment f = new RemoteAccessFragment();
                getFragmentManager().beginTransaction().replace(R.id.settings_frame, f).commit();
                isSubFragment = true;
                invalidateOptionsMenu();
            }
        };
        handler.sendEmptyMessage(0);
    }

    private void initProgressView() {
        mProgressView = (RelativeLayout) findViewById(R.id.settings_progress_view);
    }

    /**
     * SETTINGS FRAGMENT
     */
    @SuppressLint("ValidFragment")
    public class SettingsFragment extends PreferenceFragment implements
            SharedPreferences.OnSharedPreferenceChangeListener {

        private Toast mToast;

        public SettingsFragment() {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            String scenario = NASPref.getBackupScenario(getActivity());
            String[] scenarios = getActivity().getResources().getStringArray(R.array.backup_scenario_values);
            scenerioType = Arrays.asList(scenarios).indexOf(scenario);

            addPreferencesFromResource(R.xml.preference_settings);
            getPreferenceManager().setSharedPreferencesName(getString(R.string.pref_name));
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);
            refreshColumnRemoteAccessSetting();
            refreshColumnBackupSetting(false);
            refreshColumnBackupScenario(true, false);
            refreshColumnBackupLocation(true);
            refreshColumnDownloadLocation();
            refreshColumnCacheUseSize();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            Log.w(TAG, "onActivityResult");
            if (requestCode == FileActionLocateActivity.REQUEST_CODE) {
                if (resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    if (bundle == null) return;
                    String mode = bundle.getString("mode");
                    String type = bundle.getString("type");
                    String path = bundle.getString("path");
                    if (NASApp.ACT_DIRECT.equals(type)) {
                        if (NASApp.MODE_SMB.equals(mode))
                            NASPref.setBackupLocation(getActivity(), path);
                        else
                            NASPref.setDownloadLocation(getActivity(), path);
                    }
                }
            }
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            String key = preference.getKey();
            if (key.equals(getString(R.string.pref_backup_location))) {
                startBackupLocateActivity();
            } else if (key.equals(getString(R.string.pref_download_location))) {
                startDownloadLocateActivity();
            } else if (key.equals(getString(R.string.pref_cache_clean))) {
                cleanCache();
            } else if (key.equals(getString(R.string.pref_remote_access))) {
                startRemoteAccessFragment();
            } else if(key.equals(getString(R.string.pref_auto_backup))){
                String scenario = NASPref.getBackupScenario(getActivity());
                String[] scenarios = getActivity().getResources().getStringArray(R.array.backup_scenario_values);
                scenerioType = Arrays.asList(scenarios).indexOf(scenario);
            }
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(getString(R.string.pref_auto_backup))) {
                refreshColumnBackupSetting(true);
            } else if (key.equals(getString(R.string.pref_backup_scenario))) {
                refreshColumnBackupScenario(false, true);
            } else if (key.equals(getString(R.string.pref_backup_location))) {
                refreshColumnBackupLocation(false);
            } else if (key.equals(getString(R.string.pref_download_location))) {
                refreshColumnDownloadLocation();
            } else if (key.equals(getString(R.string.pref_cache_size))) {
                refreshColumnCacheSize();
            }
        }

        private void cleanCache() {
            ImageLoader.getInstance().clearMemoryCache();
            ImageLoader.getInstance().clearDiskCache();
            toast(R.string.msg_cache_cleared);
            refreshColumnCacheUseSize();
        }

        private void startRemoteAccessFragment() {
            String email = NASPref.getCloudUsername(mContext);
            String pwd = NASPref.getCloudPassword(mContext);
            if (!email.equals("") && !pwd.equals("")) {
                isRemoteAccessRegister = true;
                isRemoteAccessActive = false;
                Bundle arg = new Bundle();
                arg.putString("server", NASPref.getCloudServer(mContext));
                arg.putString("email", email);
                arg.putString("password", pwd);
                getLoaderManager().restartLoader(LoaderID.TUTK_LOGIN, arg, SettingsActivity.this).forceLoad();
            } else {
                isRemoteAccessRegister = false;
                isRemoteAccessActive = false;
                showRemoteAccessFragment(getString(R.string.login));
                if (isRemoteAccessCheck) {
                    setAutoBackupToWifi();
                    Toast.makeText(mContext, getString(R.string.remote_access_always_warning), Toast.LENGTH_LONG).show();
                }
            }
        }

        private void startBackupLocateActivity() {
            Bundle args = new Bundle();
            args.putString("mode", NASApp.MODE_SMB);
            args.putString("type", NASApp.ACT_DIRECT);
            args.putString("root", NASApp.ROOT_SMB);
            args.putString("path", NASPref.getBackupLocation(getActivity()));
            Intent intent = new Intent();
            intent.setClass(getActivity(), FileActionLocateActivity.class);
            intent.putExtras(args);
            startActivityForResult(intent, FileActionLocateActivity.REQUEST_CODE);
        }

        private void startDownloadLocateActivity() {
            Bundle args = new Bundle();
            args.putString("mode", NASApp.MODE_STG);
            args.putString("type", NASApp.ACT_DIRECT);
            args.putString("root", NASApp.ROOT_STG);
            args.putString("path", NASPref.getDownloadLocation(getActivity()));
            Intent intent = new Intent();
            intent.setClass(getActivity(), FileActionLocateActivity.class);
            intent.putExtras(args);
            startActivityForResult(intent, FileActionLocateActivity.REQUEST_CODE);
        }

        private void restartService(boolean createFolder){
            boolean checked = NASPref.getBackupSetting(getActivity());
            Log.d(TAG, "check need to restart service : " + checked);
            if(checked) {
                //restart service
                Intent intent = new Intent(mContext, AutoBackupService.class);
                mContext.stopService(intent);

                Bundle arg = new Bundle();
                if(createFolder){
                    isStartService = true;
                    arg.putString("path",NASPref.getBackupLocation(mContext));
                    getLoaderManager().restartLoader(LoaderID.SMB_NEW_FOLDER, arg, SettingsActivity.this).forceLoad();
                }
                else {
                    getLoaderManager().restartLoader(LoaderID.AUTO_BACKUP, arg, SettingsActivity.this).forceLoad();
                }
            }
        }

        private void refreshColumnRemoteAccessSetting() {
            String key = getString(R.string.pref_remote_access);
            Preference pref = findPreference(key);
            int status = NASPref.getCloudAccountStatus(mContext);
            if(status == NASPref.Status.Padding.ordinal())
                pref.setTitle(getString(R.string.remote_access_padding));
            else if(status == NASPref.Status.Active.ordinal())
                pref.setTitle(getString(R.string.remote_access_active) + ", " + getString(R.string.remote_access_unbind) + " " + getString(R.string.app_name));
            else if(status == NASPref.Status.Bind.ordinal())
                pref.setTitle(getString(R.string.remote_access_bind) + " " + getString(R.string.app_name));
            else
                pref.setTitle(getString(R.string.remote_access_inactive));
        }

        private void refreshColumnBackupSetting(boolean changeService) {
            String key = getString(R.string.pref_auto_backup);
            boolean checked = NASPref.getBackupSetting(getActivity());
            CheckBoxPreference pref = (CheckBoxPreference) findPreference(key);
            pref.setChecked(checked);

            if (changeService) {
                Intent intent = new Intent(mContext, AutoBackupService.class);
                if (checked) {
                    isStartService = true;
                    Bundle arg = new Bundle();
                    arg.putString("path", NASPref.getBackupLocation(mContext));
                    getLoaderManager().restartLoader(LoaderID.SMB_NEW_FOLDER, arg, SettingsActivity.this).forceLoad();
                } else
                    mContext.stopService(intent);
            }

            ListPreference pref_backup_scenario = (ListPreference) findPreference(getString(R.string.pref_backup_scenario));
            pref_backup_scenario.setEnabled(checked);
            pref_backup_scenario.setSelectable(checked);
            Preference pref_backup_location = findPreference(getString(R.string.pref_backup_location));
            pref_backup_location.setEnabled(checked);
            pref_backup_location.setSelectable(checked);
        }

        public void refreshColumnBackupScenario(boolean init, boolean check) {
            String scenario = NASPref.getBackupScenario(getActivity());
            String[] scenarios = getActivity().getResources().getStringArray(R.array.backup_scenario_values);
            int idx = Arrays.asList(scenarios).indexOf(scenario);
            if (check && idx == 0) {
                //"Always" Auto Backup, we need to check remote access set or not
                isRemoteAccessCheck = true;
                startRemoteAccessFragment();
                return;
            }

            String title = getActivity().getResources().getStringArray(R.array.backup_scenario_entries)[idx];
            String key = getString(R.string.pref_backup_scenario);
            ListPreference pref = (ListPreference) findPreference(key);
            pref.setValue(scenarios[idx]);
            pref.setTitle(title);
            pref.setEnabled(NASPref.getBackupSetting(getActivity()));
            if(!init && scenerioType != idx)
                restartService(false);

            scenerioType = idx;
            isRemoteAccessCheck = false;
        }

        private void refreshColumnBackupLocation(boolean init) {
            String location = NASPref.getBackupLocation(getActivity());
            String key = getString(R.string.pref_backup_location);
            Preference pref = findPreference(key);
            pref.setSummary(location);
            pref.setEnabled(NASPref.getBackupSetting(getActivity()));
            if(!init)
                restartService(false);
        }

        private void refreshColumnDownloadLocation() {
            String location = NASPref.getDownloadLocation(getActivity());
            String key = getString(R.string.pref_download_location);
            Preference pref = findPreference(key);
            pref.setSummary(location);
        }

        private void refreshColumnCacheUseSize() {
            String key = getString(R.string.pref_cache_clean);
            Preference pref = findPreference(key);
            String size = getString(R.string.used) + ": " + FileFactory.getInstance().getFileSize(StorageUtils.getCacheDirectory(mContext).getAbsolutePath());
            pref.setSummary(size);
        }

        private void refreshColumnCacheSize() {
            String size = NASPref.getCacheSize(getActivity());
            String key = getString(R.string.pref_cache_size);
            ListPreference pref = (ListPreference) findPreference(key);
            pref.setValue(size);
            pref.setSummary(size);
        }

        private void toast(int resId) {
            if (mToast != null)
                mToast.cancel();
            mToast = Toast.makeText(getActivity(), resId, Toast.LENGTH_SHORT);
            mToast.setGravity(Gravity.CENTER, 0, 0);
            mToast.show();
        }

    }

    /**
     * RemoteAccess FRAGMENT
     */
    @SuppressLint("ValidFragment")
    public class RemoteAccessFragment extends Fragment implements View.OnClickListener {
        LinearLayout initLayout;
        LinearLayout loginLayout;
        LinearLayout registerLayout;

        TextInputLayout tlEmail;
        TextInputLayout tlPwd;
        TextInputLayout tlPwdConfirm;

        public RemoteAccessFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v;
            if (isRemoteAccessRegister) {
                isInitFragment = false;
                v = inflater.inflate(R.layout.fragment_remote_access_normal, container, false);
                TextView tvEmail = (TextView) v.findViewById(R.id.remote_access_email);
                TextView tvStatus = (TextView) v.findViewById(R.id.remote_access_status);
                Button btResend = (Button) v.findViewById(R.id.remote_access_resend_button);
                Button btBind = (Button) v.findViewById(R.id.remote_access_bind_button);
                Button btDelete = (Button) v.findViewById(R.id.remote_access_delete_button);
                TextView tvListTitle = (TextView) v.findViewById(R.id.remote_access_list_title);
                ListView lvList = (ListView) v.findViewById(R.id.remote_access_list);
                tvEmail.setText(NASPref.getCloudUsername(mContext));

                if (isRemoteAccessActive) {
                    btResend.setVisibility(View.GONE);

                    boolean isBind = false;
                    if (naslist != null) {
                        Server mServer = ServerManager.INSTANCE.getCurrentServer();
                        String uuid = mServer.getTutkUUID();
                        if(uuid == null)
                            uuid = NASPref.getUUID(getActivity());
                        String serialNum = NASPref.getSerialNum(getActivity());
                        Log.d(TAG, "Current user: " + mServer.getUsername());
                        Log.d(TAG, "Current UUID: " + uuid);
                        Log.d(TAG, "Current SerialNum: " + serialNum);
                        String ID_TITLE = "TITLE", ID_SUBTITLE = "SUBTITLE";
                        ArrayList<HashMap<String, String>> myListData = new ArrayList<HashMap<String, String>>();

                        for (TutkGetNasLoader.TutkNasNode node : naslist) {
                            HashMap<String, String> item = new HashMap<String, String>();
                            item.put(ID_TITLE, node.nasName);
                            item.put(ID_SUBTITLE, node.nasUUID);
                            if (node.nasUUID.equals(uuid)) {
                                myListData.add(0, item);
                                isBind = true;
                            }
                            else
                                myListData.add(item);
                        }

                        lvList.setAdapter(new SimpleAdapter(mContext,
                                        myListData,
                                        android.R.layout.simple_list_item_2,
                                        new String[]{ID_TITLE, ID_SUBTITLE},
                                        new int[]{android.R.id.text1, android.R.id.text2})
                        );
                    }

                    if(isBind) {
                        NASPref.setCloudAccountStatus(mContext, NASPref.Status.Bind.ordinal());
                        tvStatus.setText(getString(R.string.remote_access_success_info));
                        btBind.setVisibility(View.GONE);
                    }
                    else {
                        NASPref.setCloudAccountStatus(mContext, NASPref.Status.Active.ordinal());
                        tvStatus.setText(getString(R.string.remote_access_bind_info));
                        btBind.setVisibility(View.VISIBLE);
                    }
                } else {
                    tvStatus.setText(getString(R.string.remote_access_send_activate_info));
                    btResend.setVisibility(View.VISIBLE);
                    btBind.setVisibility(View.GONE);
                }

                btResend.setOnClickListener(this);
                btBind.setOnClickListener(this);
                btDelete.setOnClickListener(this);
            } else {
                isInitFragment = true;
                v = inflater.inflate(R.layout.fragment_remote_access_register, container, false);
                initLayout = (LinearLayout) v.findViewById(R.id.remote_access_init_layout);
                registerLayout = (LinearLayout) v.findViewById(R.id.remote_access_register_layout);
                loginLayout = (LinearLayout) v.findViewById(R.id.remote_access_login_layout);
                mTitle.setText(getString(R.string.remote_access));
                Button btLogin = (Button) v.findViewById(R.id.remote_access_init_login);
                btLogin.setOnClickListener(this);
                Button btRegister = (Button) v.findViewById(R.id.remote_access_init_register);
                btRegister.setOnClickListener(this);
            }

            return v;
        }

        @Override
        public void onClick(View v) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            String email, pwd, pwdConfirm;
            Bundle arg;

            switch (v.getId()) {
                case R.id.remote_access_init_login:
                    mTitle.setText(getString(R.string.login));
                    initLayout.setVisibility(View.GONE);
                    loginLayout.setVisibility(View.VISIBLE);
                    registerLayout.setVisibility(View.GONE);
                    tlEmail = (TextInputLayout) findViewById(R.id.remote_access_login_email);
                    tlEmail.getEditText().setText(NASPref.getCloudUsername(mContext));
                    tlPwd = (TextInputLayout) findViewById(R.id.remote_access_login_password);
                    tlPwd.getEditText().setText("");
                    Button btLogin = (Button) findViewById(R.id.remote_access_login_login);
                    btLogin.setOnClickListener(this);
                    TextView tvForget = (TextView) findViewById(R.id.remote_access_login_forget);
                    tvForget.setOnClickListener(this);
                    isInitFragment = false;
                    break;
                case R.id.remote_access_init_register:
                    mTitle.setText(getString(R.string.register));
                    initLayout.setVisibility(View.GONE);
                    loginLayout.setVisibility(View.GONE);
                    registerLayout.setVisibility(View.VISIBLE);
                    tlEmail = (TextInputLayout) findViewById(R.id.remote_access_register_email);
                    tlEmail.getEditText().setText(NASPref.getCloudUsername(mContext));
                    tlPwd = (TextInputLayout) findViewById(R.id.remote_access_register_password);
                    tlPwd.getEditText().setText("");
                    tlPwdConfirm = (TextInputLayout) findViewById(R.id.remote_access_register_password_confirm);
                    tlPwdConfirm.getEditText().setText("");
                    Button btSubmit = (Button) findViewById(R.id.remote_access_register_submit);
                    btSubmit.setOnClickListener(this);
                    isInitFragment = false;
                    break;
                case R.id.remote_access_resend_button:
                    arg = new Bundle();
                    arg.putString("server", NASPref.getCloudServer(mContext));
                    arg.putString("email", NASPref.getCloudUsername(mContext));
                    showNotificationDialog(getString(R.string.remote_access_send_activate_warning), LoaderID.TUTK_NAS_RESEND_ACTIVATE, arg);
                    break;
                case R.id.remote_access_bind_button:
                    arg = new Bundle();
                    arg.putString("server", NASPref.getCloudServer(mContext));
                    arg.putString("token", NASPref.getCloudAuthToken(mContext));
                    Server server = ServerManager.INSTANCE.getCurrentServer();
                    String nasName = server.getServerInfo().hostName;
                    String serialNum = NASPref.getSerialNum(getActivity());
                    if(serialNum != null && !serialNum.equals(""))
                        nasName = nasName + NASApp.TUTK_NAME_TAG + serialNum;
                    arg.putString("nasName", nasName);
                    String uuid = server.getTutkUUID();
                    if(uuid == null)
                        uuid = NASPref.getUUID(getActivity());
                    if(uuid != null && !uuid.equals("")){
                        arg.putString("nasUUID", uuid);
                        getLoaderManager().restartLoader(LoaderID.TUTK_NAS_CREATE, arg, SettingsActivity.this).forceLoad();
                    }
                    else{
                        mBindDialog = new BindDialog(mContext, arg) {
                            @Override
                            public void onConfirm(Bundle args) {
                                getLoaderManager().restartLoader(LoaderID.LOGIN, args, SettingsActivity.this).forceLoad();
                            }

                            @Override
                            public void onCancel() {
                                //getLoaderManager().destroyLoader(LoaderID.TUTK_NAS_CREATE);
                                mBindDialog.dismiss();
                                mBindDialog = null;
                            }
                        };
                    }
                    break;
                case R.id.remote_access_delete_button:
                    showNotificationDialog(getString(R.string.remote_access_logout), LoaderID.TUTK_LOGOUT, null);
                    break;
                case R.id.remote_access_login_login:
                    email = tlEmail.getEditText().getText().toString();
                    pwd = tlPwd.getEditText().getText().toString();

                    if (email.equals("")) {
                        Toast.makeText(mContext, getString(R.string.empty_email), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (pwd.equals("")) {
                        Toast.makeText(mContext, getString(R.string.empty_password), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    arg = new Bundle();
                    arg.putString("server", NASPref.getCloudServer(mContext));
                    arg.putString("email", email);
                    arg.putString("password", pwd);
                    getLoaderManager().restartLoader(LoaderID.TUTK_LOGIN, arg, SettingsActivity.this).forceLoad();
                    break;
                case R.id.remote_access_register_submit:
                    email = tlEmail.getEditText().getText().toString();
                    pwd = tlPwd.getEditText().getText().toString();
                    pwdConfirm = tlPwdConfirm.getEditText().getText().toString();

                    if (email.equals("")) {
                        Toast.makeText(mContext, getString(R.string.empty_email), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (pwd.equals("")) {
                        Toast.makeText(mContext, getString(R.string.empty_password), Toast.LENGTH_SHORT).show();
                        return;
                    } else if(pwd.length() < 6  || pwd.length() > 20){
                        Toast.makeText(mContext, getString(R.string.password_size) + "", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    //check password and confirm password
                    if (!pwd.equals(pwdConfirm)) {
                        Toast.makeText(mContext, getString(R.string.confirm_password_error), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    arg = new Bundle();
                    arg.putString("server", NASPref.getCloudServer(mContext));
                    arg.putString("email", email);
                    arg.putString("password", pwd);
                    getLoaderManager().restartLoader(LoaderID.TUTK_REGISTER, arg, SettingsActivity.this).forceLoad();
                    break;
                case R.id.remote_access_login_forget:
                    Bundle args = new Bundle();
                    args.putString("title", getString(R.string.forget_password_title));
                    args.putString("email", NASPref.getCloudUsername(mContext));
                    mForgetDialog = new ForgetPwdDialog(mContext, args) {
                        @Override
                        public void onConfirm(Bundle args) {
                            getLoaderManager().restartLoader(LoaderID.TUTK_FORGET_PASSWORD, args, SettingsActivity.this).forceLoad();
                        }

                        @Override
                        public void onCancel() {
                            getLoaderManager().destroyLoader(LoaderID.TUTK_FORGET_PASSWORD);
                            mForgetDialog.dismiss();
                            mForgetDialog = null;
                        }
                    };
                default:
                    break;
            }
        }
    }

}
