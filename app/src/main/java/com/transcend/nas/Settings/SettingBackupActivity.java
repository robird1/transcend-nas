package com.transcend.nas.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.realtek.nasfun.api.HttpClientManager;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerInfo;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.NASUtils;
import com.transcend.nas.R;
import com.transcend.nas.management.FileActionLocateActivity;
import com.transcend.nas.service.AutoBackupService;
import com.tutk.IOTC.P2PService;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static android.R.attr.version;
import static com.transcend.nas.R.string.location;


/**
 * Created by ikelee on 16/11/25.
 */
public class SettingBackupActivity extends AppCompatActivity {
    public static final int REQUEST_CODE = SettingBackupActivity.class.hashCode() & 0xFFFF;
    public static final String TAG = SettingBackupActivity.class.getSimpleName();

    public BackupFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initToolbar();
        mFragment = new BackupFragment();
        getFragmentManager().beginTransaction().replace(R.id.settings_frame, mFragment).commit();
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
        super.onBackPressed();
    }

    /**
     * INITIALIZATION
     */
    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.settings_toolbar);
        toolbar.setTitle("");
        toolbar.setNavigationIcon(R.drawable.ic_navigation_arrow_white_24dp);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        TextView title = (TextView) findViewById(R.id.settings_title);
        title.setText(getString(R.string.auto_backup));
    }

    public static class BackupFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        public final String TAG = SettingsActivity.class.getSimpleName();
        private static final String XML_TAG_HOST_NAME = "hostname";

        public BackupFragment() {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preference_backup);
            getPreferenceManager().setSharedPreferencesName(getString(R.string.pref_name));
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);
            refreshColumnBackupSetting();
            refreshColumnBackupVideo();
            refreshColumnBackupScenario();
            refreshColumnBackupLocation();
            refreshColumnBackupSource();
            refreshColumnBackupDevice();
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
                        if (NASApp.MODE_SMB.equals(mode)) {
                            NASPref.setBackupLocation(getActivity(), path);
                        } else {
                            NASPref.setBackupSource(getActivity(), path);
                        }
                    }
                }
            }
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            String key = preference.getKey();
            if (key.equals(getString(R.string.pref_backup_location))) {
                startBackupLocateActivity();
            } else if (key.equals(getString(R.string.pref_backup_source))) {
                startBackupSourceActivity();
            }
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(getString(R.string.pref_auto_backup))) {
                refreshColumnBackupSetting();
                restartService();
            } else if (key.equals(getString(R.string.pref_backup_video))) {
                refreshColumnBackupVideo();
                restartService();
            } else if (key.equals(getString(R.string.pref_backup_scenario))) {
                refreshColumnBackupScenario();
                restartService();
            } else if (key.equals(getString(R.string.pref_backup_location))) {
                refreshColumnBackupLocation();
                restartService();
            } else if (key.equals(getString(R.string.pref_backup_source))) {
                refreshColumnBackupSource();
                restartService();
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

        private void startBackupSourceActivity() {
            Bundle args = new Bundle();
            args.putString("mode", NASApp.MODE_STG);
            args.putString("type", NASApp.ACT_DIRECT);
            args.putString("root", NASApp.ROOT_STG);
            args.putString("path", NASPref.getBackupSource(getActivity()));
            Intent intent = new Intent();
            intent.setClass(getActivity(), FileActionLocateActivity.class);
            intent.putExtras(args);
            startActivityForResult(intent, FileActionLocateActivity.REQUEST_CODE);
        }

        private void refreshColumnBackupSetting() {
            String key = getString(R.string.pref_auto_backup);
            boolean checked = NASPref.getBackupSetting(getActivity());
            CheckBoxPreference pref = (CheckBoxPreference) findPreference(key);
            pref.setChecked(checked);

            CheckBoxPreference pref_backup_video = (CheckBoxPreference) findPreference(getString(R.string.pref_backup_video));
            pref_backup_video.setEnabled(checked);
            pref_backup_video.setSelectable(checked);
            CheckBoxPreference pref_backup_scenario = (CheckBoxPreference) findPreference(getString(R.string.pref_backup_scenario));
            pref_backup_scenario.setEnabled(checked);
            pref_backup_scenario.setSelectable(checked);
            Preference pref_backup_location = findPreference(getString(R.string.pref_backup_location));
            pref_backup_location.setEnabled(checked);
            pref_backup_location.setSelectable(checked);
            Preference pref_backup_source = findPreference(getString(R.string.pref_backup_source));
            pref_backup_source.setEnabled(checked);
            pref_backup_source.setSelectable(checked);
        }

        private void refreshColumnBackupVideo() {
            String key = getString(R.string.pref_backup_video);
            boolean checked = NASPref.getBackupVideo(getActivity());
            CheckBoxPreference pref = (CheckBoxPreference) findPreference(key);
            pref.setChecked(checked);
        }

        public void refreshColumnBackupScenario() {
            String key = getString(R.string.pref_backup_scenario);
            boolean checked = NASPref.getBackupScenario(getActivity());
            CheckBoxPreference pref = (CheckBoxPreference) findPreference(key);
            pref.setChecked(checked);
        }

        private void refreshColumnBackupLocation() {
            String location = NASPref.getBackupLocation(getActivity());
            String key = getString(R.string.pref_backup_location);
            Preference pref = findPreference(key);
            pref.setSummary(location);
        }

        private void refreshColumnBackupSource() {
            String location = NASPref.getBackupSource(getActivity());
            String key = getString(R.string.pref_backup_source);
            Preference pref = findPreference(key);
            pref.setSummary(location);
        }

        private void restartService() {
            Intent intent = new Intent(getActivity(), AutoBackupService.class);
            getActivity().stopService(intent);
            boolean enable = NASPref.getBackupSetting(getActivity());
            if (enable)
                getActivity().startService(intent);

            Log.d(TAG, "restartService : " + enable);
        }

        private void refreshColumnBackupDevice() {
            String device = NASPref.getDeviceName(getActivity());
            if (device != null && !"".equals(device)) {
                Preference pref = findPreference(getString(R.string.pref_backup_device));
                pref.setSummary(device);
            }
        }

    }
}

