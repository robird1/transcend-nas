package com.transcend.nas.settings;

import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;

import com.realtek.nasfun.api.ServerInfo;
import com.transcend.nas.LoaderID;
import com.transcend.nas.R;

/**
 * Created by steve_su on 2016/11/25.
 */

public class DeviceInfoActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Boolean> {
    public static final int REQUEST_CODE = DeviceInfoActivity.class.hashCode() & 0xFFFF;
    public static final String TAG = DeviceInfoActivity.class.getSimpleName();

    public int mLoaderID = -1;
    public DeviceInfoFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initToolbar();

        mFragment = new DeviceInfoFragment();
        getFragmentManager().beginTransaction().replace(R.id.settings_frame, mFragment).commit();

        //start firmware information loader
        getLoaderManager().restartLoader(LoaderID.FIRMWARE_INFORMATION, null, this).forceLoad();
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
        toolbar.setNavigationIcon(R.drawable.ic_navi_backaarow_white);
        TextView title = (TextView) toolbar.findViewById(R.id.settings_title);
        title.setText(getString(R.string.device_information));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        switch (mLoaderID = id) {
            case LoaderID.FIRMWARE_INFORMATION:
                return new FirmwareInfoLoader(this);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        if (loader instanceof FirmwareInfoLoader) {
            ServerInfo info = ((FirmwareInfoLoader) loader).getServerInfo();
            if (mFragment != null && info != null)
                mFragment.refreshDeviceInfo(info);
        }
        mLoaderID = -1;
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {

    }

    @Override
    public void onBackPressed() {
        if (mLoaderID >= 0) {
            getLoaderManager().destroyLoader(mLoaderID);
        }
        super.onBackPressed();
    }

    public static class DeviceInfoFragment extends PreferenceFragment {

        public DeviceInfoFragment() {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preference_device_info);
        }

        private void refreshDeviceInfo(ServerInfo info) {
            Preference prefDeviceName = findPreference(getString(R.string.pref_device_name));
            prefDeviceName.setSummary(info.hostName);
            Preference prefIPAddress = findPreference(getString(R.string.pref_device_ip));
            prefIPAddress.setSummary(info.ipAddress);
            Preference prefMACAddress = findPreference(getString(R.string.pref_device_mac));
            prefMACAddress.setSummary(info.mac);
        }

    }

}
