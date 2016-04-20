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
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerInfo;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.common.LoaderID;
import com.transcend.nas.common.NotificationDialog;
import com.transcend.nas.common.TutkCodeID;
import com.transcend.nas.management.FileActionLocateActivity;
import com.transcend.nas.management.SmbFolderCreateLoader;
import com.transcend.nas.management.TutkCreateNasLoader;
import com.transcend.nas.management.TutkGetNasLoader;
import com.transcend.nas.management.TutkLoginLoader;
import com.transcend.nas.management.TutkRegisterLoader;
import com.transcend.nas.management.TutkResendActivateLoader;
import com.transcend.nas.service.AutoBackupService;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by silverhsu on 16/3/2.
 */
public class SettingsActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Boolean> {

    public static final String TAG = SettingsActivity.class.getSimpleName();

    private RelativeLayout mProgressView;

    private int mLoaderID;

    private boolean isSubFragment = false;
    private boolean isRemoteAccessRegister = false;
    private boolean isRemoteAccessActive = false;
    private TextView mTitle = null;
    private List<TutkGetNasLoader.TutkNasNode> naslist;

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_settings);
        initToolbar();
        showSettingFragment();
        initProgressView();
        createBackupsFolder();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(isSubFragment)
            getMenuInflater().inflate(R.menu.nas_finder, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (isSubFragment)
                    showSettingFragment();
                else
                    finish();
                break;
            case R.id.action_refresh_nas_finder:
                String email = NASPref.getCloudUsername(mContext);
                String pwd = NASPref.getCloudPassword(mContext);
                if (isRemoteAccessRegister && !email.equals("")) {
                    isRemoteAccessRegister = true;
                    isRemoteAccessActive = false;
                    Bundle arg = new Bundle();
                    arg.putString("server", NASPref.getCloudServer(mContext));
                    arg.putString("email", email);
                    arg.putString("password", pwd);
                    getLoaderManager().restartLoader(LoaderID.TUTK_LOGIN, arg, SettingsActivity.this).forceLoad();
                }
                else{
                    Toast.makeText(this,getString(R.string.remote_access_no_login),Toast.LENGTH_SHORT).show();;
                }
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
            if (isSubFragment)
                showSettingFragment();
            else
                finish();
        }
    }

    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
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
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        if (!success) {
            mProgressView.setVisibility(View.INVISIBLE);
            Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            return;
        }

        if (loader instanceof TutkRegisterLoader) {
            checkRegisterNASResult((TutkRegisterLoader) loader);
        } else if (loader instanceof TutkLoginLoader) {
            checkLoginNASResult((TutkLoginLoader) loader);
        } else if (loader instanceof TutkCreateNasLoader) {
            checkCreateNASResult((TutkCreateNasLoader) loader);
        } else if (loader instanceof TutkGetNasLoader) {
            checkGetNASResult((TutkGetNasLoader) loader);
        } else if (loader instanceof TutkResendActivateLoader) {
            checkResendActivateResult((TutkResendActivateLoader) loader);
        }
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {

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
            NASPref.setCloudUsername(mContext, email);
            NASPref.setCloudPassword(mContext, pwd);
            NASPref.setCloudAuthToken(mContext, loader.getAuthToke());
            Bundle arg = new Bundle();
            arg.putString("server", loader.getServer());
            arg.putString("token", loader.getAuthToke());
            //TODO : get current nas uuid
            ServerInfo info = ServerManager.INSTANCE.getCurrentServer().getServerInfo();
            arg.putString("nasName", info.hostName);
            arg.putString("nasUUID", "CHKABX6WVL7C9HPGUHZJ");
            getLoaderManager().restartLoader(LoaderID.TUTK_NAS_CREATE, arg, SettingsActivity.this).forceLoad();
        } else {
            mProgressView.setVisibility(View.INVISIBLE);
            if (code.equals(TutkCodeID.NOT_VERIFIED)) {
                //account not verified
                isRemoteAccessRegister = true;
                isRemoteAccessActive = false;
                NASPref.setCloudUsername(mContext, email);
                NASPref.setCloudPassword(mContext, pwd);
                Toast.makeText(this, getString(R.string.remote_access_send_activate_info), Toast.LENGTH_SHORT).show();
            } else {
                isRemoteAccessRegister = false;
                isRemoteAccessActive = false;
                if (!code.equals(""))
                    Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, getString(R.string.error_format), Toast.LENGTH_SHORT).show();

                if(isSubFragment)
                    return;
            }

            showRemoteAccessFragment(getString(R.string.remote_access));
        }
    }

    private void checkCreateNASResult(TutkCreateNasLoader loader) {
        String status = loader.getStatus();
        String code = loader.getCode();

        if (code.equals("") || code.equals(TutkCodeID.SUCCESS) || code.equals(TutkCodeID.UID_ALREADY_TAKEN)) {
            Bundle arg = new Bundle();
            arg.putString("server", loader.getServer());
            arg.putString("token", loader.getAuthToken());
            getLoaderManager().restartLoader(LoaderID.TUTK_NAS_GET, arg, SettingsActivity.this).forceLoad();
        } else {
            mProgressView.setVisibility(View.INVISIBLE);
            Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
        }
    }

    private void checkGetNASResult(TutkGetNasLoader loader) {
        String status = loader.getStatus();
        String code = loader.getCode();

        if (code.equals("")) {
            naslist = loader.getNasList();
            showRemoteAccessFragment(getString(R.string.remote_access));
        } else {
            Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
        }
        mProgressView.setVisibility(View.INVISIBLE);
    }

    private void checkResendActivateResult(TutkResendActivateLoader loader) {
        String status = loader.getStatus();
        String code = loader.getCode();
        if (code.equals(TutkCodeID.SUCCESS))
            Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
        else if(code.equals(TutkCodeID.VERIFICATIONEXPIRED)){
            isRemoteAccessRegister = false;
            isRemoteAccessActive = false;
            showRemoteAccessFragment(getString(R.string.remote_access));
        }
        else
            Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
        mProgressView.setVisibility(View.INVISIBLE);
    }

    private void showNotificationDialog(String title, final int loaderID, final Bundle args) {
        Bundle value = new Bundle();
        value.putString("title", title);
        NotificationDialog mNotificationDialog = new NotificationDialog(this, value) {
            @Override
            public void onConfirm() {
                if (loaderID == LoaderID.TUTK_LOGOUT) {
                    NASPref.setCloudUsername(mContext, "");
                    NASPref.setCloudPassword(mContext, "");
                    NASPref.setCloudAuthToken(mContext, "");
                    showSettingFragment();
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
        mTitle = (TextView) toolbar.findViewById(R.id.settings_title);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void showSettingFragment() {
        mTitle.setText(getString(R.string.settings));
        isSubFragment = false;
        Fragment f = new SettingsFragment();
        getFragmentManager().beginTransaction().replace(R.id.settings_frame, f).commit();
        invalidateOptionsMenu();
    }

    private void showRemoteAccessFragment(final String title){
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

    private void createBackupsFolder() {
        boolean checked = NASPref.getBackupSetting(this);
        if (checked) {
            //create the auto backup basic folder : /home/username/
            Bundle args = new Bundle();
            args.putString("path", NASPref.getBackupLocation(this));
            getLoaderManager().restartLoader(LoaderID.SMB_NEW_FOLDER, args, this).forceLoad();
        }
    }


    /**
     * SETTINGS FRAGMENT
     */
    public class SettingsFragment extends PreferenceFragment implements
            SharedPreferences.OnSharedPreferenceChangeListener {

        private Toast mToast;

        public SettingsFragment(){

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preference_settings);
            getPreferenceManager().setSharedPreferencesName(getString(R.string.pref_name));
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);
            refreshColumnRemoteAccessSetting();
            refreshColumnBackupSetting(false);
            refreshColumnBackupScenario();
            refreshColumnBackupLocation();
            refreshColumnDownloadLocation();
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
            if (preference.getKey().equals(getString(R.string.pref_backup_location))) {
                startBackupLocateActivity();
            } else if (preference.getKey().equals(getString(R.string.pref_download_location))) {
                startDownloadLocateActivity();
            } else if (preference.getKey().equals(getString(R.string.pref_cache_clean))) {
                cleanCache();
            } else if (preference.getKey().equals(getString(R.string.pref_remote_access))) {
                startRemoteAccessFragment();
            }
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(getString(R.string.pref_auto_backup))) {
                refreshColumnBackupSetting(true);
            } else if (key.equals(getString(R.string.pref_backup_scenario))) {
                refreshColumnBackupScenario();
                // TODO: stop backup and change backup scenario
            } else if (key.equals(getString(R.string.pref_backup_location))) {
                refreshColumnBackupLocation();
                // TODO: stop backup and change backup location

            } else if (key.equals(getString(R.string.pref_download_location))) {
                refreshColumnDownloadLocation();
            } else if (key.equals(getString(R.string.pref_cache_size))) {
                refreshColumnCacheSize();
                // TODO: reset cache size
            }
        }

        private void cleanCache() {
            ImageLoader.getInstance().clearMemoryCache();
            ImageLoader.getInstance().clearDiskCache();
            toast(R.string.msg_cache_cleared);
        }

        private void startRemoteAccessFragment() {
            String email = NASPref.getCloudUsername(mContext);
            String pwd = NASPref.getCloudPassword(mContext);
            if (email.equals("")) {
                isRemoteAccessRegister = false;
                isRemoteAccessActive = false;
                showRemoteAccessFragment(getString(R.string.register));
            } else {
                isRemoteAccessRegister = true;
                isRemoteAccessActive = false;
                Bundle arg = new Bundle();
                arg.putString("server", NASPref.getCloudServer(mContext));
                arg.putString("email", email);
                arg.putString("password", pwd);
                getLoaderManager().restartLoader(LoaderID.TUTK_LOGIN, arg, SettingsActivity.this).forceLoad();
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

        private void refreshColumnRemoteAccessSetting() {
            String key = getString(R.string.pref_remote_access);
            String email = NASPref.getCloudUsername(mContext);
            Preference pref = findPreference(key);
            pref.setSummary(email.equals("") ? getString(R.string.remote_access_inactive) : email);
        }

        private void refreshColumnBackupSetting(boolean changeService) {
            String key = getString(R.string.pref_auto_backup);
            boolean checked = NASPref.getBackupSetting(getActivity());
            CheckBoxPreference pref = (CheckBoxPreference) findPreference(key);
            pref.setChecked(checked);

            if (changeService) {
                Intent intent = new Intent(mContext, AutoBackupService.class);
                if (checked) {
                    mContext.startService(intent);

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

        private void refreshColumnBackupScenario() {
            String scenario = NASPref.getBackupScenario(getActivity());
            String[] scenarios = getActivity().getResources().getStringArray(R.array.backup_scenario_values);
            int idx = Arrays.asList(scenarios).indexOf(scenario);
            String title = getActivity().getResources().getStringArray(R.array.backup_scenario_entries)[idx];
            String key = getString(R.string.pref_backup_scenario);
            ListPreference pref = (ListPreference) findPreference(key);
            pref.setValue(scenario);
            pref.setTitle(title);
            pref.setEnabled(NASPref.getBackupSetting(getActivity()));
        }

        private void refreshColumnBackupLocation() {
            String location = NASPref.getBackupLocation(getActivity());
            String key = getString(R.string.pref_backup_location);
            Preference pref = findPreference(key);
            pref.setSummary(location);
            pref.setEnabled(NASPref.getBackupSetting(getActivity()));
        }

        private void refreshColumnDownloadLocation() {
            String location = NASPref.getDownloadLocation(getActivity());
            String key = getString(R.string.pref_download_location);
            Preference pref = findPreference(key);
            pref.setSummary(location);
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
    public class RemoteAccessFragment extends Fragment implements View.OnClickListener {
        TextInputLayout tlEmail;
        TextInputLayout tlPwd;
        TextInputLayout tlPwdConfirm;
        Button btLogin;
        Button btSubmit;
        TextView tvRegister;
        TextView tvInfo;
        boolean isLogin = true;

        public RemoteAccessFragment(){
            isLogin = true;
        }

        public RemoteAccessFragment(boolean login){
            isLogin = login;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v;
            if (isRemoteAccessRegister) {
                v = inflater.inflate(R.layout.fragment_remote_access_normal, container, false);
                TextView tvEmail = (TextView) v.findViewById(R.id.remote_access_email);
                TextView tvStatus = (TextView) v.findViewById(R.id.remote_access_status);
                Button btResend = (Button) v.findViewById(R.id.remote_access_resend_button);
                Button btDelete = (Button) v.findViewById(R.id.remote_access_delete_button);
                TextView tvListTitle = (TextView) v.findViewById(R.id.remote_access_list_title);
                ListView lvList = (ListView) v.findViewById(R.id.remote_access_list);
                tvEmail.setText(NASPref.getCloudUsername(mContext));

                if (isRemoteAccessActive) {
                    tvStatus.setText(getString(R.string.remote_access_active));
                    btResend.setVisibility(View.GONE);
                    tvListTitle.setVisibility(View.VISIBLE);

                    if (naslist != null) {
                        //TODO: get cuurrent nas uuid
                        //Server mServer = ServerManager.INSTANCE.getCurrentServer();
                        //String uuid = mServer.getTutkUUID();
                        String uuid = "";

                        String ID_TITLE = "TITLE", ID_SUBTITLE = "SUBTITLE";
                        ArrayList<HashMap<String, String>> myListData = new ArrayList<HashMap<String, String>>();

                        for (TutkGetNasLoader.TutkNasNode node : naslist) {
                            HashMap<String, String> item = new HashMap<String, String>();
                            item.put(ID_TITLE, node.nasName + (node.nasUUID.equals(uuid) ? " <- " + getString(R.string.current_device) : ""));
                            item.put(ID_SUBTITLE, "UUID: " + node.nasUUID);
                            myListData.add(item);
                        }

                        lvList.setAdapter(new SimpleAdapter(mContext,
                                        myListData,
                                        android.R.layout.simple_list_item_2,
                                        new String[]{ID_TITLE, ID_SUBTITLE},
                                        new int[]{android.R.id.text1, android.R.id.text2})
                        );
                    }
                } else {
                    tvStatus.setText(getString(R.string.remote_access_inactive));
                    btResend.setVisibility(View.VISIBLE);
                    tvListTitle.setVisibility(View.INVISIBLE);
                }

                btResend.setOnClickListener(this);
                btDelete.setOnClickListener(this);
            } else {
                v = inflater.inflate(R.layout.fragment_remote_access_register, container, false);
                tlEmail = (TextInputLayout) v.findViewById(R.id.register_email);
                tlEmail.getEditText().setText(NASPref.getCloudUsername(mContext));
                tlPwd = (TextInputLayout) v.findViewById(R.id.register_password);
                tlPwdConfirm = (TextInputLayout) v.findViewById(R.id.register_password_confirm);
                btLogin = (Button) v.findViewById(R.id.register_login);
                btLogin.setOnClickListener(this);
                btSubmit = (Button) v.findViewById(R.id.register_submit);
                btSubmit.setOnClickListener(this);
                tvRegister = (TextView) v.findViewById(R.id.register_button);
                tvRegister.setOnClickListener(this);
                tvInfo = (TextView) v.findViewById(R.id.register_info);
                initRegisterContent();
                if(!isLogin) {
                    tvInfo.setText(getString(R.string.remote_access_verification_expired));
                }
            }

            return v;
        }

        private void initRegisterContent(){
            tlPwd.getEditText().setText(null);
            tlPwdConfirm.getEditText().setText(null);
            if (isLogin) {
                tvInfo.setText(getString(R.string.remote_access_welcome));
                tvRegister.setText(getString(R.string.register));
                btLogin.setVisibility(View.VISIBLE);
                btSubmit.setVisibility(View.GONE);
                tlPwdConfirm.setVisibility(View.GONE);
            } else {
                tvInfo.setText(getString(R.string.remote_access_register));
                tvRegister.setText(getString(R.string.login));
                btLogin.setVisibility(View.GONE);
                btSubmit.setVisibility(View.VISIBLE);
                tlPwdConfirm.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onClick(View v) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

            String email, pwd, pwdConfirm;
            Bundle arg;

            switch (v.getId()) {
                case R.id.remote_access_resend_button:
                    arg = new Bundle();
                    arg.putString("server", NASPref.getCloudServer(mContext));
                    arg.putString("email", NASPref.getCloudUsername(mContext));
                    showNotificationDialog(getString(R.string.remote_access_send_activate_warning), LoaderID.TUTK_NAS_RESEND_ACTIVATE, arg);
                    break;
                case R.id.remote_access_delete_button:
                    showNotificationDialog(getString(R.string.remote_access_logout), LoaderID.TUTK_LOGOUT, null);
                    break;
                case R.id.register_login:
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
                case R.id.register_button:
                    isLogin = !isLogin;
                    initRegisterContent();
                    break;
                case R.id.register_submit:
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
                default:
                    break;
            }
        }
    }

}
