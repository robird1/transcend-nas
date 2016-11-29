package com.transcend.nas.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
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
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static com.transcend.nas.R.string.pref_firmware_version;


/**
 * Created by silverhsu on 16/3/2.
 */
public class SettingsActivity extends AppCompatActivity {
    public static final int REQUEST_CODE = SettingsActivity.class.hashCode() & 0xFFFF;
    public static final String TAG = SettingsActivity.class.getSimpleName();

    public SettingsFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initToolbar();

        mFragment = new SettingsFragment();
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
        toolbar.setNavigationIcon(R.drawable.ic_navigation_arrow_gray_24dp);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        public final String TAG = SettingsActivity.class.getSimpleName();
        private static final String XML_TAG_FIRMWARE_VERSION = "remote_ver";
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
            refreshFirmwareVersion();
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

        private void startDeviceInfoActivity() {
            Intent intent = new Intent();
            intent.setClass(getActivity(), DeviceInfoActivity.class);
            startActivity(intent);
        }

        private void refreshFirmwareVersion() {
            if (isAdmin()) {
                if (getActivity() != null) {
                    NASPref.showProgressBar(getActivity(), true);
                }
                update();
            } else {
                String categoryKey = getString(R.string.pref_setting_category);
                PreferenceCategory category = (PreferenceCategory) findPreference(categoryKey);
                String preferenceKey = getString(pref_firmware_version);
                Preference firmwarePref = findPreference(preferenceKey);
                category.removePreference(firmwarePref);
            }
        }

        private boolean isAdmin() {
            Server server = ServerManager.INSTANCE.getCurrentServer();
            return NASPref.defaultUserName.equals(server.getUsername());
        }

        private void update() {
            final Handler handler = new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    if (getActivity() != null) {
                        Preference pref = findPreference(getString(pref_firmware_version));
                        pref.setSummary((String) msg.obj);

                        NASPref.showProgressBar(getActivity(), false);
                    }
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
            HttpEntity entity = sendPostRequest();
            InputStream inputStream = null;
            String inputEncoding = null;

            if (entity != null) {
                try {
                    inputStream = entity.getContent();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                inputEncoding = EntityUtils.getContentCharSet(entity);
            }

            if (inputEncoding == null) {
                inputEncoding = HTTP.DEFAULT_CONTENT_CHARSET;
            }

            if (inputStream != null) {
                firmwareVersion = doParse(inputStream, inputEncoding);
//            getPostResultString(entity, inputStream);
            }

            return firmwareVersion;
        }

        private HttpEntity sendPostRequest() {
            HttpEntity entity = null;
            Server server = ServerManager.INSTANCE.getCurrentServer();
            String hostname = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
            String commandURL = "http://" + hostname + "/nas/firmware/getversion";

            HttpResponse response;
            try {
                HttpPost httpPost = new HttpPost(commandURL);
                List<NameValuePair> nameValuePairs = new ArrayList<>();
                nameValuePairs.add(new BasicNameValuePair("hash", server.getHash()));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                response = HttpClientManager.getClient().execute(httpPost);

                if (response != null) {
                    entity = response.getEntity();
                }

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return entity;
        }

        private String doParse(InputStream inputStream, String inputEncoding)
        {
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
//                    Log.d(TAG, "tagName: " + tagName);

                    if (eventType == XmlPullParser.START_TAG) {
                        if (tagName.equals(XML_TAG_FIRMWARE_VERSION)) {
                            parser.next();
//                            Log.d(TAG, "parser.getText(): " + parser.getText());

                            firmwareVersion = parser.getText();
                            break;
                        }
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

//    private void getPostResultString(HttpEntity entity, InputStream inputStream) {
//        Log.d(TAG, "inputStream: "+ inputStream);
//        Log.d(TAG, "contentType: " + entity.getContentType().toString());
//
//        try {
//            ByteArrayOutputStream result = new ByteArrayOutputStream();
//            byte[] buffer = new byte[1024];
//            int length;
//
//            while ((length = inputStream.read(buffer)) != -1) {
//                result.write(buffer, 0, length);
//            }
//
//            Log.d(TAG, "result: "+ result.toString("UTF-8"));
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
    }
}

