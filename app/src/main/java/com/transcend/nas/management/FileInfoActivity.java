package com.transcend.nas.management;

import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.transcend.nas.LoaderID;
import com.transcend.nas.R;
import com.transcend.nas.management.firmware.FileFactory;

import java.util.Map;

/**
 * Created by silverhsu on 16/3/9.
 */
public class FileInfoActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Boolean> {

    public static final int REQUEST_CODE = FileInfoActivity.class.hashCode() & 0xFFFF;
    public static final String TAG = FileInfoActivity.class.getSimpleName();
    private static final boolean enableMoreInfo = false;

    private ImageView ivImage;
    private InformationFragment mFragment;
    private int mLoaderID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_info);
        Bundle args = getIntent().getExtras();
        FileInfo fileInfo = (FileInfo) args.getSerializable("info");

        initToolbar();
        initImagePreview(fileInfo);
        initFragment();

        if(enableMoreInfo)
            checkFileInfo(fileInfo);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getLoaderManager().destroyLoader(mLoaderID);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        getLoaderManager().destroyLoader(mLoaderID);
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
        toolbar.setNavigationIcon(R.drawable.ic_navi_backaarow_white);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void initImagePreview(FileInfo fileInfo){
        ivImage = (ImageView) findViewById(R.id.info_image);
        if (fileInfo.type.equals(FileInfo.TYPE.DIR))
            ivImage.setImageResource(R.drawable.ic_folder_gray_big);
        else if (fileInfo.type.equals(FileInfo.TYPE.PHOTO))
            ivImage.setImageResource(R.drawable.ic_image_gray_big);
        else if (fileInfo.type.equals(FileInfo.TYPE.VIDEO))
            ivImage.setImageResource(R.drawable.ic_movies_gray_big);
        else if (fileInfo.type.equals(FileInfo.TYPE.MUSIC))
            ivImage.setImageResource(R.drawable.ic_audiotrack_gray_big);
        if (fileInfo.type.equals(FileInfo.TYPE.PHOTO) || fileInfo.type.equals(FileInfo.TYPE.VIDEO) || fileInfo.type.equals(FileInfo.TYPE.MUSIC))
            FileFactory.getInstance().displayPhoto(this, true, fileInfo.path, ivImage);
    }

    private void initFragment() {
        int id = R.id.info_frame;
        mFragment = new InformationFragment();
        getFragmentManager().beginTransaction().replace(id, mFragment).commit();
    }

    private void checkFileInfo(FileInfo fileInfo){
        Bundle args = new Bundle();
        args.putString("path", fileInfo.path);
        getLoaderManager().restartLoader(LoaderID.SMB_FILE_INFO, args, this).forceLoad();
    }

    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        switch (mLoaderID = id) {
            case LoaderID.SMB_FILE_INFO:
                String path = args.getString("path");
                return new FileInfoLoader(this, path);
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        if(loader instanceof FileInfoLoader) {
            if(success) {
                Map<String, String> result = ((FileInfoLoader) loader).getFileInfo();
                if(mFragment != null)
                    mFragment.addMoreInfo(result);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {

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
            refreshColumnSize();
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

        private void refreshColumnSize() {
            String key = getString(R.string.pref_info_size);
            Preference pref = findPreference(key);

            String formatSize = FileFactory.getInstance().getFileSize(mFileInfo.size);
            if(!FileInfo.TYPE.DIR.equals(mFileInfo.type))
                pref.setSummary(formatSize);
        }

        public void addMoreInfo(Map<String, String> info) {
            PreferenceScreen screen = getPreferenceScreen();
            if(info != null) {
                for (String key : info.keySet()) {
                    // Create the Preferences Manually - so that the key can be set programatically.
                    PreferenceCategory category = new PreferenceCategory(screen.getContext());
                    category.setLayoutResource(R.layout.preference_category_widget);
                    category.setTitle(key);
                    screen.addPreference(category);

                    Preference preference = new Preference(screen.getContext());
                    preference.setSummary(info.get(key));
                    preference.setSelectable(false);

                    category.addPreference(preference);
                }
            }

            //add footer
            PreferenceCategory category = new PreferenceCategory(screen.getContext());
            screen.addPreference(category);
        }

    }

}
