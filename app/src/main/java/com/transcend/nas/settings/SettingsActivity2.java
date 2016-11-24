package com.transcend.nas.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.utils.StorageUtils;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.management.FileActionLocateActivity;
import com.transcend.nas.management.firmware.FileFactory;

/**
 * Created by steve_su on 2016/11/24.
 */

public class SettingsActivity2 extends AppCompatActivity {
    public static final int REQUEST_CODE = NewSettingsActivity.class.hashCode() & 0xFFFF;
    private static final String TAG = NewSettingsActivity.class.getSimpleName();
    private SettingsFragment mSettingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initToolbar();
        showSettingFragment();
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
        toolbar.setNavigationIcon(R.drawable.ic_navigation_arrow_gray_24dp);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
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
    private class SettingsFragment extends PreferenceFragment implements
            SharedPreferences.OnSharedPreferenceChangeListener {

        private Toast mToast;
        private boolean mIsDownloadLocation = false;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preference_settings_2);

            getPreferenceManager().setSharedPreferencesName(getString(R.string.pref_name));
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);
            refreshColumnDownloadLocation();
            refreshColumnCacheUseSize();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        // TODO check this method
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
                        if (NASApp.MODE_SMB.equals(mode)) {
                            NASPref.setBackupLocation(getActivity(), path);
                        }
                        else {
                            if(mIsDownloadLocation)
                                NASPref.setDownloadLocation(getActivity(), path);
                            else
                                NASPref.setBackupSource(getActivity(), path);
                        }
                    }
                }
            }
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            String key = preference.getKey();
            if (key.equals(getString(R.string.pref_download_location))) {
                startDownloadLocateActivity();
            } else if (key.equals(getString(R.string.pref_auto_backup))) {

                // TODO start an activity to configure backup setting

            } else if (key.equals(getString(R.string.pref_disk_info))) {
                startDiskInfoActivity();

            } else if (key.equals(getString(R.string.pref_device_info))) {

                // TODO start an activity to show device information

            } else if (key.equals(getString(R.string.pref_cache_clean))) {
                showCleanCacheDialog();

            } else if (key.equals(getString(R.string.pref_about))) {
                startAboutActivity();
            }

            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(getString(R.string.pref_download_location))) {
                refreshColumnDownloadLocation();
            } else if (key.equals(getString(R.string.pref_cache_size))) {
                refreshColumnCacheSize();
            }
        }

        private void showCleanCacheDialog() {
            new AlertDialog.Builder(SettingsActivity2.this).setTitle(R.string.app_name).setMessage(
                    R.string.dialog_clean_cache).setNegativeButton(R.string.dialog_button_no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            }).setPositiveButton(R.string.dialog_button_yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    cleanCache();
                }
            }).create().show();
        }

        private void cleanCache() {
            ImageLoader.getInstance().clearMemoryCache();
            ImageLoader.getInstance().clearDiskCache();
            toast(R.string.msg_cache_cleared);
            refreshColumnCacheUseSize();
        }

        private void startDownloadLocateActivity() {
            mIsDownloadLocation = true;
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

        private void refreshColumnDownloadLocation() {
            String location = NASPref.getDownloadLocation(getActivity());
            String key = getString(R.string.pref_download_location);
            Preference pref = findPreference(key);
            pref.setSummary(location);
        }

        private void refreshColumnCacheUseSize() {
            String key = getString(R.string.pref_cache_clean);
            Preference pref = findPreference(key);
            String size = getString(R.string.used) + ": " + FileFactory.getInstance().getFileSize(StorageUtils.getCacheDirectory(SettingsActivity2.this).getAbsolutePath());
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

        private void startAboutActivity() {
            Intent intent = new Intent();
            intent.setClass(SettingsActivity2.this, AboutActivity.class);
            startActivity(intent);
        }

        private void startDiskInfoActivity() {
            Intent intent = new Intent();
            intent.setClass(SettingsActivity2.this, DiskInfoActivity.class);
            startActivity(intent);
        }

    }

}
