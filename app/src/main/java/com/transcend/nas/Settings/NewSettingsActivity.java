package com.transcend.nas.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by silverhsu on 16/3/2.
 */
public class NewSettingsActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Boolean> {

    public static final int REQUEST_CODE = NewSettingsActivity.class.hashCode() & 0xFFFF;
    public static final String TAG = NewSettingsActivity.class.getSimpleName();

    private RelativeLayout mProgressView;
    private SettingsFragment mSettingsFragment;

    private int mLoaderID;
    private int scenerioType = -1;
    private boolean isStartService = false;
    private boolean isRemoteAccessCheck = false;

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
        if (checked && path != null && !path.equals("")) {
            Bundle arg = new Bundle();
            arg.putString("path", path);
            getLoaderManager().restartLoader(LoaderID.SMB_NEW_FOLDER, arg, this).forceLoad();
        } else {
            doRemoteAccessCheck(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
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

    @Override
    public void onBackPressed() {
        if (mProgressView.isShown()) {
            getLoaderManager().destroyLoader(mLoaderID);
            mProgressView.setVisibility(View.INVISIBLE);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        mProgressView.setVisibility(View.VISIBLE);
        String server, email, pwd, token, nasName, nasUUID;

        switch (mLoaderID = id) {
            case LoaderID.SMB_NEW_FOLDER:
                String path = args.getString("path");
                return new SmbFolderCreateLoader(this, path);
            case LoaderID.TUTK_LOGIN:
                server = args.getString("server");
                email = args.getString("email");
                pwd = args.getString("password");
                return new TutkLoginLoader(this, server, email, pwd);
            case LoaderID.TUTK_NAS_GET:
                server = args.getString("server");
                token = args.getString("token");
                return new TutkGetNasLoader(this, server, token);
            case LoaderID.AUTO_BACKUP:
                return new AutoBackupLoader(this);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        if (loader instanceof SmbFolderCreateLoader) {
            if (isStartService) {
                Bundle arg = new Bundle();
                getLoaderManager().restartLoader(LoaderID.AUTO_BACKUP, arg, this).forceLoad();
                isStartService = false;
            } else {
                if(!doRemoteAccessCheck(false))
                    mProgressView.setVisibility(View.INVISIBLE);
            }
            return;
        }

        if (!success) {
            mProgressView.setVisibility(View.INVISIBLE);
            Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            return;
        }

        if (loader instanceof TutkLoginLoader) {
            checkLoginNASResult((TutkLoginLoader) loader);
        } else if (loader instanceof TutkGetNasLoader) {
            checkGetNASResult((TutkGetNasLoader) loader);
        } else if (loader instanceof AutoBackupLoader) {
            mProgressView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {

    }

    private void checkLoginNASResult(TutkLoginLoader loader) {
        String code = loader.getCode();
        String status = loader.getStatus();
        String token = loader.getAuthToke();
        String email = loader.getEmail();
        String pwd = loader.getPassword();

        if (!token.equals("")) {
            //token not null mean login success
            NASPref.setCloudUsername(mContext, email);
            NASPref.setCloudPassword(mContext, pwd);
            NASPref.setCloudAuthToken(mContext, loader.getAuthToke());
            Bundle arg = new Bundle();
            arg.putString("server", loader.getServer());
            arg.putString("token", loader.getAuthToke());
            getLoaderManager().restartLoader(LoaderID.TUTK_NAS_GET, arg, NewSettingsActivity.this).forceLoad();
        } else {
            mProgressView.setVisibility(View.INVISIBLE);
            if(isRemoteAccessCheck) {
                Toast.makeText(this, getString(R.string.remote_access_always_warning), Toast.LENGTH_SHORT).show();
            } else {
                if (code.equals(TutkCodeID.NOT_VERIFIED)) {
                    //account not verified
                    NASPref.setCloudAccountStatus(mContext, NASPref.Status.Padding.ordinal());
                    NASPref.setCloudUsername(mContext, email);
                    NASPref.setCloudPassword(mContext, pwd);
                } else {
                    if (!code.equals(""))
                        Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(this, getString(R.string.error_format), Toast.LENGTH_SHORT).show();
                }
            }
            setAutoBackupToWifi();
        }
    }

    private void checkGetNASResult(TutkGetNasLoader loader) {
        String status = loader.getStatus();
        String code = loader.getCode();
        NASPref.setCloudAccountStatus(mContext, NASPref.Status.Active.ordinal());

        boolean isBind = false;
        if (code.equals("")) {
            List<TutkGetNasLoader.TutkNasNode> naslist = loader.getNasList();
            //check nas uuid and record it
            Server mServer = ServerManager.INSTANCE.getCurrentServer();
            String uuid = mServer.getTutkUUID();
            if (uuid == null) {
                uuid = NASPref.getUUID(mContext);
            }

            for (TutkGetNasLoader.TutkNasNode nas : naslist) {
                if (nas.nasUUID.equals(uuid)) {
                    NASPref.setCloudUUID(mContext, uuid);
                    NASPref.setCloudAccountStatus(mContext, NASPref.Status.Bind.ordinal());
                    isBind = true;
                    break;
                }
            }

            if(isRemoteAccessCheck && !isBind) {
                setAutoBackupToWifi();
                Toast.makeText(this, getString(R.string.remote_access_always_warning), Toast.LENGTH_SHORT).show();
            }
        } else {
            setAutoBackupToWifi();
            Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
        }

        if(mSettingsFragment != null) {
            mSettingsFragment.refreshColumnBackupScenario(false, false);
            mSettingsFragment.refreshColumnRemoteAccessSetting();
        }
        mProgressView.setVisibility(View.INVISIBLE);
    }

    private void setAutoBackupToWifi() {
        String[] scenarios = mContext.getResources().getStringArray(R.array.backup_scenario_values);
        NASPref.setBackupScenario(mContext, scenarios[1]);
    }

    private boolean doRemoteAccessCheck(boolean check){
        isRemoteAccessCheck = check;
        String email = NASPref.getCloudUsername(mContext);
        String pwd = NASPref.getCloudPassword(mContext);
        if (!email.equals("") && !pwd.equals("")) {
            Bundle arg = new Bundle();
            arg.putString("server", NASPref.getCloudServer(mContext));
            arg.putString("email", email);
            arg.putString("password", pwd);
            getLoaderManager().restartLoader(LoaderID.TUTK_LOGIN, arg, NewSettingsActivity.this).forceLoad();
            return true;
        }
        else {
            if(isRemoteAccessCheck)
                Toast.makeText(this, getString(R.string.remote_access_always_warning), Toast.LENGTH_SHORT).show();
            setAutoBackupToWifi();
            return false;
        }
    }

    /**
     * INITIALIZATION
     */
    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.settings_toolbar);
        toolbar.setTitle("");
        toolbar.setNavigationIcon(R.drawable.ic_navigation_arrow_gray_24dp);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void initProgressView() {
        mProgressView = (RelativeLayout) findViewById(R.id.settings_progress_view);
    }

    private void showSettingFragment() {
        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (mSettingsFragment == null)
                    mSettingsFragment = new SettingsFragment();
                getFragmentManager().beginTransaction().replace(R.id.settings_frame, mSettingsFragment).commit();
                invalidateOptionsMenu();
            }
        };
        handler.sendEmptyMessage(0);
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
            } else if (requestCode == RemoteAccessActivity.REQUEST_CODE){
                if(mSettingsFragment != null)
                    mSettingsFragment.refreshColumnRemoteAccessSetting();
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
                startRemoteAccessActivity();
            } else if (key.equals(getString(R.string.pref_auto_backup))) {
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

        private void restartService(boolean createFolder) {
            boolean checked = NASPref.getBackupSetting(getActivity());
            Log.d(TAG, "check need to restart service : " + checked);
            if (checked) {
                //restart service
                Intent intent = new Intent(mContext, AutoBackupService.class);
                mContext.stopService(intent);

                Bundle arg = new Bundle();
                if (createFolder) {
                    isStartService = true;
                    arg.putString("path", NASPref.getBackupLocation(mContext));
                    getLoaderManager().restartLoader(LoaderID.SMB_NEW_FOLDER, arg, NewSettingsActivity.this).forceLoad();
                } else {
                    getLoaderManager().restartLoader(LoaderID.AUTO_BACKUP, arg, NewSettingsActivity.this).forceLoad();
                }
            }
        }

        private void refreshColumnRemoteAccessSetting() {
            String key = getString(R.string.pref_remote_access);
            Preference pref = findPreference(key);
            int status = NASPref.getCloudAccountStatus(mContext);
            if (status == NASPref.Status.Padding.ordinal())
                pref.setTitle(getString(R.string.remote_access_padding));
            else if (status == NASPref.Status.Active.ordinal())
                pref.setTitle(getString(R.string.remote_access_active) + ", " + getString(R.string.remote_access_unbind) + " " + getString(R.string.app_name));
            else if (status == NASPref.Status.Bind.ordinal())
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
                    getLoaderManager().restartLoader(LoaderID.SMB_NEW_FOLDER, arg, NewSettingsActivity.this).forceLoad();
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
                doRemoteAccessCheck(true);
                return;
            }

            String title = getActivity().getResources().getStringArray(R.array.backup_scenario_entries)[idx];
            String key = getString(R.string.pref_backup_scenario);
            ListPreference pref = (ListPreference) findPreference(key);
            pref.setValue(scenarios[idx]);
            pref.setTitle(title);
            pref.setEnabled(NASPref.getBackupSetting(getActivity()));
            if (!init && scenerioType != idx)
                restartService(false);

            scenerioType = idx;
        }

        private void refreshColumnBackupLocation(boolean init) {
            String location = NASPref.getBackupLocation(getActivity());
            String key = getString(R.string.pref_backup_location);
            Preference pref = findPreference(key);
            pref.setSummary(location);
            pref.setEnabled(NASPref.getBackupSetting(getActivity()));
            if (!init)
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

        private void startRemoteAccessActivity() {
            Intent intent = new Intent();
            intent.setClass(getActivity(), RemoteAccessActivity.class);
            startActivityForResult(intent, RemoteAccessActivity.REQUEST_CODE);
        }

        private void toast(int resId) {
            if (mToast != null)
                mToast.cancel();
            mToast = Toast.makeText(getActivity(), resId, Toast.LENGTH_SHORT);
            mToast.setGravity(Gravity.CENTER, 0, 0);
            mToast.show();
        }

    }

}
