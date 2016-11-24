package com.transcend.nas.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.utils.StorageUtils;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.management.FileActionLocateActivity;
import com.transcend.nas.management.firmware.FileFactory;


/**
 * Created by ikelee on 16/11/23.
 */

public class SettingsFragment extends BasicFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String TAG = SettingsActivity.class.getSimpleName();
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
            if (resultCode == getActivity().RESULT_OK) {
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
            startBackupFragment();
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
        args.putString("path", NASPref.getDownloadLocation(getActivity()));
        Intent intent = new Intent();
        intent.setClass(getActivity(), FileActionLocateActivity.class);
        intent.putExtras(args);
        startActivityForResult(intent, FileActionLocateActivity.REQUEST_CODE);
    }

    private void startBackupFragment() {
        BackupFragment mBackupFragment = new BackupFragment();
        mBackupFragment.setListener(mBackupFragment, getFragmentListener());
        mBackupFragment.setPreviousFragment(this);
        showNextFragement(mBackupFragment);
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
        mToast.setGravity(Gravity.CENTER, 0, 0);
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
}
