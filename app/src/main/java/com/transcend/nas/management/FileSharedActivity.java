package com.transcend.nas.management;

import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.transcend.nas.AutoLinkActivity;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.LoaderID;
import com.transcend.nas.management.upload.UriFileUploadLoader;

import java.util.ArrayList;

public class FileSharedActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Boolean> {

    private static final String TAG = FileSharedActivity.class.getSimpleName();
    private ArrayList<Uri> mImageUris;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_shared);
        String password = NASPref.getPassword(this);
        if (password != null && !password.equals("")) {
            Intent intent = getIntent();
            onReceiveIntent(intent);
        } else {
            Toast.makeText(this, getString(R.string.login_not), Toast.LENGTH_SHORT).show();
            startAutoLinkActivity();
            finish();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        onReceiveIntent(intent);
    }

    private void onReceiveIntent(Intent intent) {
        //TODO : login flow
        if (intent == null) {
            Log.d(TAG, "onReceiveIntent Empty");
            finish();
            return;
        }

        String action = intent.getAction();
        String type = intent.getType();
        Log.d(TAG, "onReceiveIntent " + action + ", " + type);

        ArrayList<Uri> imageUris = null;
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/") || type.startsWith("video/")) {
                Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri != null) {
                    imageUris = new ArrayList<>();
                    imageUris.add(imageUri);
                }
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith("image/") || type.startsWith("video/")) {
                imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            }
        }

        if (imageUris != null && imageUris.size() > 0) {
            if (mImageUris == null)
                mImageUris = new ArrayList<>();
            mImageUris.clear();

            for (Uri uri : imageUris) {
                mImageUris.add(uri);
            }

            if (mImageUris.size() > 0) {
                startFileActionLocateActivity();
                return;
            }
        }

        Toast.makeText(this, getString(R.string.unknown_format), Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FileActionLocateActivity.REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Bundle bundle = data.getExtras();
                if (bundle == null)
                    return;

                String type = bundle.getString("type");
                String path = bundle.getString("path");
                if (NASApp.ACT_UPLOAD.equals(type))
                    doUpload(path);
            }
        }

        finish();
    }

    /**
     * LOADER CONTROL
     */
    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        ArrayList<Uri> uris = args.getParcelableArrayList("uris");
        String path = args.getString("path");
        switch (id) {
            case LoaderID.LOCAL_FILE_UPLOAD:
                return new UriFileUploadLoader(this, uris, path);

        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        Log.w(TAG, "onLoaderFinished: " + loader.getClass().getSimpleName() + " " + success);
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {
        Log.w(TAG, "onLoaderReset: " + loader.getClass().getSimpleName());
    }

    private void doUpload(String dest) {
        Bundle args = new Bundle();

        if (mImageUris != null)
            args.putParcelableArrayList("uris", mImageUris);

        if (dest != null)
            args.putString("path", dest);

        int id = LoaderID.LOCAL_FILE_UPLOAD;
        getLoaderManager().restartLoader(id, args, FileSharedActivity.this).forceLoad();
        Log.w(TAG, "doUpload: " + mImageUris.size() + " item(s) to " + dest);
    }

    private void startFileActionLocateActivity() {
        String mode = NASApp.MODE_SMB;
        String root = NASApp.ROOT_SMB;
        String path = NASApp.ROOT_SMB;
        String type = NASApp.ACT_UPLOAD;

        Bundle args = new Bundle();
        args.putString("mode", mode);
        args.putString("type", type);
        args.putString("root", root);
        args.putString("path", path);
        Intent intent = new Intent();
        intent.setClass(FileSharedActivity.this, FileActionLocateActivity.class);
        intent.putExtras(args);
        startActivityForResult(intent, FileActionLocateActivity.REQUEST_CODE);
    }

    private void startAutoLinkActivity() {
        Intent intent = new Intent();
        intent.setClass(FileSharedActivity.this, AutoLinkActivity.class);
        startActivity(intent);
    }
}
