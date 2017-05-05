package com.transcend.nas.settings;

import android.app.LoaderManager;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
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
import com.transcend.nas.connection.InviteAccountActivity;
import com.transcend.nas.connection.InviteShortLinkLoader;
import com.transcend.nas.management.FileActionLocateActivity;
import com.transcend.nas.management.firmware.FileFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
            if (mFragment != null && version != null && !"".equals(version))
                mFragment.refreshFirmwareVersion(version);
        } else {
            super.onLoadFinished(loader, success);
        }
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {

    }

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener, LoaderManager.LoaderCallbacks<Boolean> {
        public final String TAG = SettingsActivity.class.getSimpleName();
        private Toast mToast;
        private RelativeLayout mProgressView;

        public SettingsFragment() {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            initProgressView();
            addPreferencesFromResource(R.xml.preference_settings);
            getPreferenceManager().setSharedPreferencesName(getString(R.string.pref_name));
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);
            refreshColumnDownloadLocation();
            refreshColumnCacheUseSize();
            if (!isAdmin()) {
                PreferenceCategory pref = (PreferenceCategory) findPreference(getString(R.string.pref_firmware));
                getPreferenceScreen().removePreference(pref);
            }

            if (!isInviteItemVisible()) {
                PreferenceCategory category = (PreferenceCategory) findPreference(getString(R.string.pref_other));
                category.removePreference(findPreference(getString(R.string.pref_fb_invite)));
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
                startBackupActivity();
            } else if (key.equals(getString(R.string.pref_disk_info))) {
                startDiskInfoActivity();
            } else if (key.equals(getString(R.string.pref_device_info))) {
                startDeviceInfoActivity();
            } else if (key.equals(getString(R.string.pref_cache_clean))) {
                showCleanCacheDialog();
            } else if (key.equals(getString(R.string.pref_about))) {
                startAboutActivity();
            } else if (key.equals(getString(R.string.pref_fb_invite))) {
                startFBInviteActivity();
            }
//            else if (key.equals(getString(R.string.pref_invite))) {
//                startInviteActivity();
//            }

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

        @Override
        public Loader<Boolean> onCreateLoader(int id, Bundle args) {
            switch (id) {
                case LoaderID.INVITE_SHORT_LINK:
                    Log.d(TAG, "[Enter] new InviteShortLinkLoader");
                    mProgressView.setVisibility(View.VISIBLE);
                    return new InviteShortLinkLoader(getActivity(), args.getString("url"));
                default:
                    return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<Boolean> loader, Boolean aBoolean) {
            if (loader instanceof InviteShortLinkLoader) {
                Log.d(TAG, "[Enter] loader instanceof InviteShortLinkLoader");
                mProgressView.setVisibility(View.INVISIBLE);
                String link = ((InviteShortLinkLoader) loader).getLink();
                String msg = "Enjoy my StoreJet Cloud!\n\n";
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_TEXT, msg + link);
                shareIntent.setType("text/*");
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.invite_friends)));
            }
        }

        @Override
        public void onLoaderReset(Loader<Boolean> loader) {

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

        private void startFBInviteActivity() {
            Intent intent = new Intent();
            intent.setClass(getActivity(), InviteAccountActivity.class);

            startActivity(intent);
        }

//        // TODO check .split("@tS#")
//        private void startInviteActivity() {
//            Log.d(TAG, "[Enter] startInviteActivity");
//
//            String uuid = NASPref.getCloudUUID(getActivity());
//            String nasId = NASPref.getCloudNasID(getActivity());
////            String nickName = NASPref.getCloudNickName(getActivity()).split("%40tS#")[0];
//            String nickName = NASPref.getCloudNickName(getActivity()).split("@tS#")[0];
//            String userName = NASPref.getUsername(getActivity());
//            String password = NASPref.getCloudPassword(getActivity());
//            String password2 = NASPref.getPassword(getActivity());
//
//            Log.d(TAG, "nasName: "+ NASPref.getCloudNickName(getActivity()));
//            Log.d(TAG, "nickName: "+ nickName);
//            Log.d(TAG, "userName: "+ userName);
//            Log.d(TAG, "password: "+ password);
//            Log.d(TAG, "password2: "+ password2);
//
//            //TODO add IOS info
//            String url = "https://z69nd.app.goo.gl/?link=http://www.storejetcloud.com?uuid%3D"+
//                    uuid+ "%26nasId%3D"+ nasId+ "%26nickName%3D"+ nickName+ "%26username%3D"+ userName+ "%26password%3D"+ password2+"&apn=com.transcend.nas";
//
//            Bundle arg = new Bundle();
//            arg.putString("url", url);
//            getLoaderManager().restartLoader(LoaderID.INVITE_SHORT_LINK, arg, this).forceLoad();
//        }

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

        private boolean isAdmin() {
            Server server = ServerManager.INSTANCE.getCurrentServer();
            return NASPref.defaultUserName.equals(server.getUsername());
        }

        private void initProgressView() {
            mProgressView = (RelativeLayout) getActivity().findViewById(R.id.settings_progress_view);
        }

        private boolean isInviteItemVisible() {
            boolean isVisible = true;
            String currentUUID = NASPref.getCloudUUID(this.getActivity());
            String account = NASPref.getUsername(this.getActivity());
            Log.d(TAG, "currentUUID: "+ currentUUID);
            Log.d(TAG, "account: "+ account);

            try {
                String jsonString = NASUtils.getInvitedNASList(this.getActivity());
                JSONArray jsonArray = new JSONArray(jsonString);
                for (int i=0; i< jsonArray.length(); i++) {
                    JSONObject temp = (JSONObject) jsonArray.get(i);
                    Log.d(TAG, "temp.optString(\"uuid\"): " + temp.optString("uuid"));
                    Log.d(TAG, "temp.optString(\"account\"): "+ temp.optString("account"));

                    boolean isInvitedMode = currentUUID.equals(temp.optString("uuid")) && account.equals(temp.optString("account"));
                    Log.d(TAG, "isInvitedMode: "+ isInvitedMode);

                    if (isInvitedMode) {
                        isVisible = false;
                        break;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return isVisible;
        }

    }

}

