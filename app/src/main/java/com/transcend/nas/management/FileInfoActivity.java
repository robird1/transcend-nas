package com.transcend.nas.management;

import android.app.Fragment;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.transcend.nas.R;

/**
 * Created by silverhsu on 16/3/9.
 */
public class FileInfoActivity extends AppCompatActivity {

    public static final String TAG = FileInfoActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_info);
        initToolbar();
        initFragment();
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
        finish();
    }


    /**
     *
     * INITIALIZATION
     *
     */
    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.info_toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void initFragment() {
        int id = R.id.info_frame;
        Fragment f = new InformationFragment();
        getFragmentManager().beginTransaction().replace(id, f).commit();
    }


    /**
     *
     * INFORMATION FRAGMENT
     *
     */
    public static class InformationFragment extends PreferenceFragment {

        private FileInfo mFileInfo;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preference_information);
            initData();
            refreshColumnLocation();
            refreshColumnName();
            refreshColumnType();
            refreshColumnTime();
        }

        private void initData() {
            Bundle args = getActivity().getIntent().getExtras();
            mFileInfo = (FileInfo) args.getSerializable("info");
        }

        private void refreshColumnLocation() {
            String key = getString(R.string.pref_info_path);
            Preference pref = findPreference(key);
            pref.setSummary(mFileInfo.path);
        }

        private void refreshColumnName() {
            String key = getString(R.string.pref_info_name);
            Preference pref = findPreference(key);
            pref.setSummary(mFileInfo.name);
        }

        private void refreshColumnType() {
            int resId = FileInfo.TYPE.DIR.equals(mFileInfo.type) ? R.string.folder
                    : FileInfo.TYPE.PHOTO.equals(mFileInfo.type) ? R.string.photo
                    : FileInfo.TYPE.VIDEO.equals(mFileInfo.type) ? R.string.video
                    : FileInfo.TYPE.MUSIC.equals(mFileInfo.type) ? R.string.music
                    : R.string.unknown_format;
            String key = getString(R.string.pref_info_type);
            Preference pref = findPreference(key);
            pref.setSummary(resId);
        }

        private void refreshColumnTime() {
            String key = getString(R.string.pref_info_time);
            Preference pref = findPreference(key);
            pref.setSummary(mFileInfo.time);
        }

    }

}
