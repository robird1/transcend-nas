package com.transcend.nas.management;

import android.app.Fragment;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASApp;
import com.transcend.nas.R;
import com.transcend.nas.utils.FileFactory;

import java.io.File;
import java.text.DecimalFormat;

/**
 * Created by silverhsu on 16/3/9.
 */
public class FileInfoActivity extends AppCompatActivity {

    public static final String TAG = FileInfoActivity.class.getSimpleName();
    private ImageView ivImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_info);
        Bundle args = getIntent().getExtras();
        FileInfo fileInfo = (FileInfo) args.getSerializable("info");
        initToolbar();
        initImagePreview(fileInfo);
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
        toolbar.setNavigationIcon(R.drawable.ic_navigation_arrow_gray_24dp);
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
        if (fileInfo.type.equals(FileInfo.TYPE.PHOTO))
            ImageLoader.getInstance().displayImage(FileFactory.getInstance().getPhotoPath(true, fileInfo.path), ivImage);
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

            //calculator the file size
            String s = " MB";
            double sizeMB = (double) mFileInfo.size / 1024 / 1024;
            if(sizeMB < 1){
                sizeMB = (double) mFileInfo.size / 1024;
                s = " KB";
            }
            else if (sizeMB >= 1000) {
                sizeMB = (double) sizeMB / 1024;
                s = " GB";
            }

            //format the size
            DecimalFormat df=new DecimalFormat("#.##");
            String formatSize = df.format(sizeMB) + s;

            if(!FileInfo.TYPE.DIR.equals(mFileInfo.type))
                pref.setSummary(formatSize);
        }

    }

}
