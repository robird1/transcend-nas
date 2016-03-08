package com.transcend.nas.settings;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.common.LoaderID;
import com.transcend.nas.management.FileActionLocateActivity;
import com.transcend.nas.management.SmbFolderCreateLoader;

import java.util.Arrays;

/**
 * Created by silverhsu on 16/3/2.
 */
public class SettingsActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Boolean> {

    public static final String TAG = SettingsActivity.class.getSimpleName();

    private RelativeLayout mProgressView;

    private int mLoaderID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initToolbar();
        initProgressView();
        initSettingsFragment();
        createBackupsFolder();
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
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        mProgressView.setVisibility(View.VISIBLE);
        String path = args.getString("path");
        switch (mLoaderID = id) {
            case LoaderID.SMB_NEW_FOLDER:
                return new SmbFolderCreateLoader(this, path);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        mProgressView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {

    }

    @Override
    public void onBackPressed() {
        if (mProgressView.isShown()) {
            getLoaderManager().destroyLoader(mLoaderID);
            mProgressView.setVisibility(View.INVISIBLE);
        }
    }

    /**
     *
     * INITIALIZATION
     *
     */
    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.settings_toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void initProgressView() {
        mProgressView = (RelativeLayout) findViewById(R.id.settings_progress_view);
    }


    private void initSettingsFragment() {
        int id = R.id.settings_frame;
        Fragment f = new SettingsFragment();
        getFragmentManager().beginTransaction().replace(id, f).commit();
    }

    private void createBackupsFolder() {
        Bundle args = new Bundle();
        args.putString("path", NASPref.getBackupLocation(this));
        getLoaderManager().restartLoader(LoaderID.SMB_NEW_FOLDER, args, this).forceLoad();
    }

    /**
     *
     * SETTINGS FRAGMENT
     *
     */
    public static class SettingsFragment extends PreferenceFragment implements
            SharedPreferences.OnSharedPreferenceChangeListener {

        private Toast mToast;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preference_settings);
            getPreferenceManager().setSharedPreferencesName(getString(R.string.pref_name));
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);
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
            }
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(getString(R.string.pref_auto_backup))) {
                boolean checked = sharedPreferences.getBoolean(key, true);
                // TODO: enable/disable auto backup

            } else
            if (key.equals(getString(R.string.pref_backup_scenario))) {
                refreshColumnBackupScenario();
                // TODO: stop backup and change backup scenario
            } else
            if (key.equals(getString(R.string.pref_backup_location))) {
                refreshColumnBackupLocation();
                // TODO: stop backup and change backup location

            } else
            if (key.equals(getString(R.string.pref_download_location))) {
                refreshColumnDownloadLocation();
            } else
            if (key.equals(getString(R.string.pref_cache_size))) {
                refreshColumnCacheSize();
                // TODO: reset cache size
            }
        }

        private void cleanCache() {
            ImageLoader.getInstance().clearMemoryCache();
            ImageLoader.getInstance().clearDiskCache();
            toast(R.string.msg_cache_cleared);
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

        private void refreshColumnBackupScenario() {
            String scenario = NASPref.getBackupScenario(getActivity());
            String[] scenarios =  getActivity().getResources().getStringArray(R.array.backup_scenario_values);
            int idx = Arrays.asList(scenarios).indexOf(scenario);
            String title = getActivity().getResources().getStringArray(R.array.backup_scenario_entries)[idx];
            String key = getString(R.string.pref_backup_scenario);
            ListPreference pref = (ListPreference) findPreference(key);
            pref.setValue(scenario);
            pref.setTitle(title);
        }

        private void refreshColumnBackupLocation() {
            String location = NASPref.getBackupLocation(getActivity());
            String key = getString(R.string.pref_backup_location);
            Preference pref = findPreference(key);
            pref.setSummary(location);
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

}
