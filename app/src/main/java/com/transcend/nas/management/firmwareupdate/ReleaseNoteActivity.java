package com.transcend.nas.management.firmwareupdate;

import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.transcend.nas.LoaderID;
import com.transcend.nas.R;
import com.transcend.nas.common.StyleFactory;

/**
 * Created by steve_su on 2017/10/16.
 */

public class ReleaseNoteActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Boolean>, View.OnClickListener {
    private static final String TAG = ReleaseNoteActivity.class.getSimpleName();
    private TextView mReleaseNoteTitle;
    private TextView mVersionView;
    private TextView mNoteView;
    private Button mConfirmButton;
    private RelativeLayout mProgressView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_release_note);
        initToolbar();
        mReleaseNoteTitle = (TextView) findViewById(R.id.release_note_title);
        mVersionView = (TextView) findViewById(R.id.firmware_version);
        mNoteView = (TextView) findViewById(R.id.release_note_content);
        mProgressView = (RelativeLayout) findViewById(R.id.main_progress_view);
        mConfirmButton = (Button) findViewById(R.id.confirm_button);
        StyleFactory.set_grey_button_touch_effect(this, mConfirmButton);
        mConfirmButton.setOnClickListener(this);
        mConfirmButton.setEnabled(false);

        requestFirmwareInfo();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.release_note, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                requestFirmwareInfo();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LoaderID.FIRMWARE_RELEASE_NOTE:
                return new ReleaseNoteLoader(this);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader loader, Boolean isSuccess) {
        mProgressView.setVisibility(View.INVISIBLE);
        if (isSuccess) {
            if (loader instanceof ReleaseNoteLoader) {
                if (mVersionView == null || mNoteView == null || ((ReleaseNoteLoader) loader).getData() == null)
                    return;
                String version = ((ReleaseNoteLoader) loader).getData().getString("remote_version");
                String releaseNote = ((ReleaseNoteLoader) loader).getData().getString("note");

                // remote version might be -1 if there is any connection issue
                if ("-1".equals(version)) {
                    Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                    return;
                }

                mConfirmButton.setEnabled(true);
                mConfirmButton.setAlpha(1.0f);
                mReleaseNoteTitle.setText(this.getString(R.string.release_note_title));
                mVersionView.setText("v".concat(version));
                Spanned tmp = Html.fromHtml(Html.fromHtml(releaseNote).toString());
                mNoteView.setText(tmp);
            }
        } else {
            Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.confirm_button) {
            startService(new Intent(this, FirmwareUpdateService.class));
        }
    }

    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
    }

    private void requestFirmwareInfo() {
//        mConfirmButton.setAlpha(0.5f);
        mProgressView.setVisibility(View.VISIBLE);
        getLoaderManager().restartLoader(LoaderID.FIRMWARE_RELEASE_NOTE, null, this).forceLoad();
    }

}
