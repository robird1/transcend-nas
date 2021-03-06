package com.transcend.nas.settings;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.DialogInterface;
import android.content.Loader;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerInfo;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.DrawerMenuController;
import com.transcend.nas.LoaderID;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.tutk.TutkCreateNasLoader;


/**
 * Created by steve_su on 2016/11/25.
 */

public class DeviceInfoActivity extends AppCompatActivity{
    public static final int REQUEST_CODE = DeviceInfoActivity.class.hashCode() & 0xFFFF;
    public static final String TAG = DeviceInfoActivity.class.getSimpleName();
    private static final String REGULAR_EXPRESSION = "^[a-zA-Z0-9-]{1,32}$";
    private static final boolean enableReviseDeviceName = false;

    private static boolean mIsBackButtonEnable = true;
    public static int mLoaderID = -1;
    public DeviceInfoFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initToolbar();

        mFragment = new DeviceInfoFragment();
        getFragmentManager().beginTransaction().replace(R.id.settings_frame, mFragment).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mIsBackButtonEnable)
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
    public void onBackPressed() {
        if (mIsBackButtonEnable) {
            if (mLoaderID >= 0) {
                mFragment.getLoaderManager().destroyLoader(mLoaderID);
            }
            super.onBackPressed();
        }
    }

    public static class DeviceInfoFragment extends PreferenceFragment implements LoaderManager.LoaderCallbacks<Boolean> {
        private Activity mContext;
        private Preference mPrefDeviceName;
        private RelativeLayout mProgressView;
        private String mUserInputName;
        private boolean mIsUpdateHostName = false;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mContext = DeviceInfoFragment.this.getActivity();
            addPreferencesFromResource(R.xml.preference_device_info);
            mPrefDeviceName = findPreference(getString(R.string.pref_device_name));
            initProgressView();
            getLoaderManager().restartLoader(LoaderID.FIRMWARE_INFORMATION, null, this).forceLoad();
        }

        @Override
        public Loader<Boolean> onCreateLoader(int id, Bundle args) {
            switch (mLoaderID = id) {
                case LoaderID.FIRMWARE_INFORMATION:
                    return new FirmwareInfoLoader(mContext);
                case LoaderID.FIRMWARE_DEVICE_NAME:
                    return new FirmwareHostNameLoader(mContext, args.getString("hostname"));
                case LoaderID.TUTK_NAS_CREATE:
                    return new TutkCreateNasLoader(mContext, args);
            }
            return null;
        }

        @Override
        public void onLoadFinished(Loader loader, Boolean success) {
            if (success) {
                if (loader instanceof FirmwareInfoLoader) {
                    ServerInfo info = ((FirmwareInfoLoader) loader).getServerInfo();
                    if (info != null) {
                        if (!mIsUpdateHostName) {
                            refreshDeviceInfo(info);
                        } else {
                            updateDeviceName(info);
                            mIsUpdateHostName = false;
                            mIsBackButtonEnable = true;
                        }
                    }
                    mLoaderID = -1;
                } else if (loader instanceof FirmwareHostNameLoader) {
                    mIsUpdateHostName = true;
                    getLoaderManager().restartLoader(LoaderID.FIRMWARE_INFORMATION, null, this).forceLoad();
                } else if (loader instanceof TutkCreateNasLoader) {
                    updateRemoteSever();
                }
            } else {
                Toast.makeText(mContext, getString(R.string.error), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onLoaderReset(Loader loader) {

        }

        private void refreshDeviceInfo(final ServerInfo info) {
            mPrefDeviceName.setSummary(info.hostName);
            if (enableReviseDeviceName && isAdmin()) {
                mPrefDeviceName.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        initDialog(info.hostName);
                        return false;
                    }
                });
            }
            Preference prefIPAddress = findPreference(getString(R.string.pref_device_ip));
            prefIPAddress.setSummary(info.ipAddress);
            Preference prefMACAddress = findPreference(getString(R.string.pref_device_mac));
            prefMACAddress.setSummary(info.mac);
        }

        private void updateDeviceName(ServerInfo info) {
            mProgressView.setVisibility(View.INVISIBLE);
            if (info != null && mUserInputName.equals(info.hostName)) {
                NASPref.setDeviceName(mContext, info.hostName);
                notifyUI(info.hostName);
            } else {
                Toast.makeText(mContext, mContext.getString(R.string.error), Toast.LENGTH_LONG).show();
            }
        }

        private void notifyUI(String name) {
            mPrefDeviceName.setSummary(name);
            for (DrawerMenuController.DeviceNameObserver o : DrawerMenuController.getObserver()) {
                o.onChangeDeviceName();
            }
        }

        private void initDialog(String hostName) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle(R.string.settings_device_name_title);
            builder.setView(R.layout.dialog_device_name);
            builder.setNegativeButton(R.string.cancel, null);
            builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    EditText userInput = (EditText) ((AlertDialog) dialog).findViewById(R.id.device_name);
                    mUserInputName = userInput.getText().toString();
                    boolean isValid = mUserInputName.matches(REGULAR_EXPRESSION);
                    if (isValid) {
                        updateTutkInfo();
                    } else {
                        Toast.makeText(mContext, mContext.getString(R.string.invalid_name), Toast.LENGTH_LONG).show();
                    }
                }
            });
            AlertDialog dialog = builder.show();
            EditText hostNameField = (EditText) dialog.findViewById(R.id.device_name);
            hostNameField.setText(hostName);
        }

        private void updateRemoteSever() {
            if (isAdmin()) {
                Bundle args = new Bundle();
                args.putString("hostname", mUserInputName);
                mProgressView.setVisibility(View.VISIBLE);
                mIsBackButtonEnable = false;
                getLoaderManager().restartLoader(LoaderID.FIRMWARE_DEVICE_NAME, args, this).forceLoad();
            }
        }

        private void updateTutkInfo() {
            String uuid = ServerManager.INSTANCE.getCurrentServer().getTutkUUID();
            if (uuid != null && !uuid.equals("")) {
                Bundle args = new Bundle();
                args.putString("server", NASPref.getCloudServer(mContext));
                args.putString("token", NASPref.getCloudAuthToken(mContext));
                args.putString("nasName", mUserInputName);
                args.putString("nasUUID", uuid);

                // use the create API for updating nas name
                getLoaderManager().restartLoader(LoaderID.TUTK_NAS_CREATE, args, this).forceLoad();
            } else {
                Toast.makeText(mContext, getString(R.string.error), Toast.LENGTH_SHORT).show();
            }
        }

        private boolean isAdmin() {
            Server server = ServerManager.INSTANCE.getCurrentServer();
            return NASPref.defaultUserName.equals(server.getUsername());
        }

        private void initProgressView() {
            mProgressView = (RelativeLayout) mContext.findViewById(R.id.settings_progress_view);
        }

    }

}
