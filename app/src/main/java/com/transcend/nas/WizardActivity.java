package com.transcend.nas;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.transcend.nas.common.LoaderID;
import com.transcend.nas.connection.AutoLinkLoader;
import com.transcend.nas.connection.NASFinderActivity;
import com.transcend.nas.connection.NASListLoader;
import com.transcend.nas.connection.SignInActivity;
import com.transcend.nas.management.FileManageActivity;
import com.transcend.nas.management.TutkGetNasLoader;

import java.util.ArrayList;
import java.util.HashMap;

public class WizardActivity extends Activity implements LoaderManager.LoaderCallbacks<Boolean> {

    private static final String TAG = WizardActivity.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wizard);
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {

    }

    @Override
    public void onLoaderReset(Loader loader) {

    }
}
