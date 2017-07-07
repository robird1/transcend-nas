package com.transcend.nas.settings;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.utils.StorageUtils;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.DrawerMenuActivity;
import com.transcend.nas.DrawerMenuController;
import com.transcend.nas.LoaderID;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.NASUtils;
import com.transcend.nas.R;
import com.transcend.nas.management.FileActionLocateActivity;
import com.transcend.nas.management.firmware.FileFactory;

import java.io.File;


/**
 * Created by silverhsu on 16/3/2.
 */
public class SettingsActivity extends DrawerMenuActivity {
    public static final int REQUEST_CODE = SettingsActivity.class.hashCode() & 0xFFFF;
    public static final String TAG = SettingsActivity.class.getSimpleName();

    public SettingsFragment mFragment;
    public int mLoaderID = -1;

    @Override
    public int onLayoutID() {
        return R.layout.activity_drawer_settings;
    }

    @Override
    public int onToolbarID() {
        return R.id.settings_toolbar;
    }

    @Override
    public DrawerMenuController.DrawerMenu onActivityDrawer() {
        return DrawerMenuController.DrawerMenu.DRAWER_DEFAULT;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.toggleDrawerCheckedItem();

        mFragment = new SettingsFragment();
        getFragmentManager().beginTransaction().replace(R.id.settings_frame, mFragment).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //start firmware version loader
        if (mFragment.isAdmin()) {
            getLoaderManager().restartLoader(LoaderID.FIRMWARE_VERSION, null, this).forceLoad();
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

    /**
     * INITIALIZATION
     */
    @Override
    protected void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.settings_toolbar);
        toolbar.setTitle("");
        toolbar.setNavigationIcon(R.drawable.ic_navi_backaarow_white);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        switch (mLoaderID = id) {
            case LoaderID.FIRMWARE_VERSION:
                return new FirmwareVersionLoader(this);
            default:
                return super.onCreateLoader(id, args);
        }
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        if (loader instanceof FirmwareVersionLoader) {
            String version = ((FirmwareVersionLoader) loader).getVersion();
            String isUpgrade = ((FirmwareVersionLoader) loader).getIsUpgrade();
            if (!TextUtils.isEmpty(version)) {
                mFragment.refreshFirmwareVersion(version);
            }

            if ("no".equals(isUpgrade) || "".equals(isUpgrade)) {
                mFragment.removeFirmwareUpdate();
            }
        } else {
            super.onLoadFinished(loader, success);
        }
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {

    }


    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        public final String TAG = SettingsActivity.class.getSimpleName();
        private Toast mToast;

        public SettingsFragment() {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preference_settings);
            getPreferenceManager().setSharedPreferencesName(getString(R.string.pref_name));
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);
            refreshColumnDownloadLocation();
            refreshColumnCacheUseSize();
            if (!isAdmin()) {
                PreferenceCategory pref = (PreferenceCategory) findPreference(getString(R.string.pref_firmware));
                getPreferenceScreen().removePreference(pref);
            }
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
                if (resultCode == Activity.RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    if (bundle == null) return;
                    String mode = bundle.getString("mode");
                    String type = bundle.getString("type");
                    String path = bundle.getString("path");
                    if (NASApp.ACT_DIRECT.equals(type)) {
                        NASPref.setDownloadLocation(getActivity(), path);
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
                startBackupActivity();
            } else if (key.equals(getString(R.string.pref_disk_info))) {
                startDiskInfoActivity();
            } else if (key.equals(getString(R.string.pref_device_info))) {
                startDeviceInfoActivity();
            } else if (key.equals(getString(R.string.pref_cache_clean))) {
                showCleanCacheDialog();
            } else if (key.equals(getString(R.string.pref_about))) {
                startAboutActivity();
            } else if (key.equals(getString(R.string.pref_firmware_update))) {
                NASUtils.showFirmwareNotify(getActivity());
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
            new AlertDialog.Builder(getActivity()).setTitle(R.string.app_name).setMessage(
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
            Bundle args = new Bundle();
            args.putString("mode", NASApp.MODE_STG);
            args.putString("type", NASApp.ACT_DIRECT);
            args.putString("root", NASApp.ROOT_STG);
            args.putString("path", getDownloadLocation());
            Intent intent = new Intent();
            intent.setClass(getActivity(), FileActionLocateActivity.class);
            intent.putExtras(args);
            startActivityForResult(intent, FileActionLocateActivity.REQUEST_CODE);
        }

        private String getDownloadLocation() {
            String location = NASPref.getDownloadLocation(getActivity());
            File file = new File(location);
            if (!file.exists()) {
                location = NASApp.ROOT_STG;
            } else {                                 // Enter this block if SD card has been removed
                File[] files = file.listFiles();
                if (files == null) {
                    location = NASApp.ROOT_STG;
                }
            }
            return location;
        }

        private void startBackupActivity() {
            Intent intent = new Intent(getActivity(), SettingBackupActivity.class);
            startActivity(intent);
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
            String size = getString(R.string.used) + ": " + FileFactory.getInstance().getFileSize(StorageUtils.getCacheDirectory(getActivity()).getAbsolutePath());
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
            //mToast.setGravity(Gravity.CENTER, 0, 0);
            mToast.show();
        }

        private void startAboutActivity() {
            Intent intent = new Intent();
            intent.setClass(getActivity(), AboutActivity.class);
            startActivity(intent);
        }

        private void startDiskInfoActivity() {
            Intent intent = new Intent();
            intent.setClass(getActivity(), DiskInfoActivity.class);
            startActivity(intent);
        }

        private void startDeviceInfoActivity() {
            Intent intent = new Intent();
            intent.setClass(getActivity(), DeviceInfoActivity.class);
            startActivity(intent);
        }

        private void refreshFirmwareVersion(String version) {
            Preference pref = findPreference(getString(R.string.pref_firmware_version));
            pref.setSummary(version);
        }

        private void removeFirmwareUpdate() {
            PreferenceCategory prefCategory = (PreferenceCategory) findPreference(getString(R.string.pref_firmware));
            Preference pref = findPreference(getString(R.string.pref_firmware_update));
            if (prefCategory != null && pref != null) {
                prefCategory.removePreference(pref);
            }
        }

        private boolean isAdmin() {
            Server server = ServerManager.INSTANCE.getCurrentServer();
            return NASPref.defaultUserName.equals(server.getUsername());
        }

    }
}

