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
import com.realtek.nasfun.api.HttpClientManager;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.management.FileActionLocateActivity;
import com.transcend.nas.management.firmware.FileFactory;
import com.tutk.IOTC.P2PService;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
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

/**
 * Created by steve_su on 2016/11/24.
 */

public class SettingsActivity2 extends AppCompatActivity {
    public static final int REQUEST_CODE = SettingsActivity2.class.hashCode() & 0xFFFF;
    private static final String TAG = SettingsActivity2.class.getSimpleName();
    private static final String XML_TAG_FIRMWARE_VERSION = "software";
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
            refreshFirmwareVersion();
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

        private void refreshFirmwareVersion() {
            final Handler handler = new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    Preference pref = findPreference(getString(R.string.pref_firmware_version));
                    pref.setSummary((String) msg.obj);
                }
            };

            new Thread(new Runnable() {
                @Override
                public void run() {
                    String version = getFirmwareVersion();
                    if (version != null) {
                        Message msg = Message.obtain();
                        msg.obj = version;
                        handler.sendMessage(msg);
                    }
                }
            }).start();

        }

        private String getFirmwareVersion() {
            String firmwareVersion = null;
            Server server = ServerManager.INSTANCE.getCurrentServer();
            String hostname = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
            String hash = server.getHash();
            DefaultHttpClient httpClient = HttpClientManager.getClient();
            String commandURL = "http://" + hostname + "/nas/get/info";
            Log.d(TAG, commandURL);

            HttpResponse response;
            InputStream inputStream = null;
            try {
                HttpPost httpPost = new HttpPost(commandURL);
                List<NameValuePair> nameValuePairs = new ArrayList<>();
                nameValuePairs.add(new BasicNameValuePair("hash", hash));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                response = httpClient.execute(httpPost);
                HttpEntity entity = null;
                if (response != null) {
                    entity = response.getEntity();
                }

                if (entity != null) {
                    inputStream = entity.getContent();
                }
                String inputEncoding = EntityUtils.getContentCharSet(entity);
                if (inputEncoding == null) {
                    inputEncoding = HTTP.DEFAULT_CONTENT_CHARSET;
                }

                if (inputStream != null) {
                    firmwareVersion = doParse(inputStream, inputEncoding);
                }

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return firmwareVersion;
        }

        private String doParse(InputStream inputStream, String inputEncoding)
        {
            Log.d(TAG, "[Enter] doParse()");

            String firmwareVersion = null;
            XmlPullParserFactory factory;

            try {
                factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(inputStream, inputEncoding);
                int eventType = parser.getEventType();

                do {
                    String tagName = parser.getName();
                    Log.d(TAG, "tagName: " + tagName);

                    if (eventType == XmlPullParser.START_TAG) {
                        if (tagName.equals(XML_TAG_FIRMWARE_VERSION)) {
                            parser.next();
                            Log.d(TAG, "parser.getText(): " + parser.getText());

                            firmwareVersion = parser.getText();
                            break;
                        }

                    } else if (eventType == XmlPullParser.TEXT) {
                        Log.d(TAG, "parser.getText(): " + parser.getText());
                    }

                    eventType = parser.next();

                } while (eventType != XmlPullParser.END_DOCUMENT);

            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return firmwareVersion;

        }

    }

}
