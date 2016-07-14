package com.transcend.nas;

import android.app.Activity;
import android.app.LoaderManager;
import android.app.Notification;
import android.content.Intent;
import android.content.Loader;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.transcend.nas.common.LoaderID;
import com.transcend.nas.connection.NASFinderActivity;
import com.transcend.nas.connection.NASListLoader;
import com.transcend.nas.connection.SignInActivity;
import com.transcend.nas.utils.StyleFactory;

import java.util.ArrayList;
import java.util.HashMap;

public class InitialActivity extends Activity implements LoaderManager.LoaderCallbacks<Boolean>, View.OnClickListener {

    private static final String TAG = InitialActivity.class.getSimpleName();
    private TextView mTitle;
    private Button mStart;
    private Button mRemoteAccess;
    private ImageView iBackground;
    private RelativeLayout mProgressView;
    private int mLoaderID = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initial);

        mTitle = (TextView) findViewById(R.id.initial_title);

        mStart = (Button) findViewById(R.id.initial_started_button);
        mStart.setOnClickListener(this);
        StyleFactory.set_white_button_touch_effect(this, mStart);

        mRemoteAccess = (Button) findViewById(R.id.initial_remote_access_button);
        mRemoteAccess.setOnClickListener(this);
        StyleFactory.set_white_button_touch_effect(this, mRemoteAccess);

        mProgressView = (RelativeLayout) findViewById(R.id.activity_init_progress_view);

        iBackground = (ImageView) findViewById(R.id.initial_bg);
        Point p = new Point();
        getWindowManager().getDefaultDisplay().getSize(p);
        iBackground.setImageBitmap(createBitmapFromResource(R.drawable.bg_wizard, p.x, p.y));

        Intent intent = getIntent();
        if(intent != null) {
            boolean retry = (boolean) intent.getBooleanExtra("Retry", false);
            if(retry)
                showRetryPage();
        }
    }

    public Bitmap createBitmapFromResource(int drawableId, int reqWidth,int reqHeight){
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), drawableId);
        int oldwidth = bmp.getWidth();
        int oldheight = bmp.getHeight();
        float scaleWidth = reqWidth / (float)oldwidth;
        float scaleHeight = reqHeight / (float)oldheight;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(bmp, 0, 0, oldwidth, oldheight, matrix, true);
        return resizedBitmap;
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        mProgressView.setVisibility(View.VISIBLE);
        switch (mLoaderID = id) {
            case LoaderID.NAS_LIST:
                int retry = 2;
                return new NASListLoader(this, retry);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        mProgressView.setVisibility(View.INVISIBLE);
        if (loader instanceof NASListLoader) {
            if (success)
                startNASFinderActivity(((NASListLoader) loader).getList());
            else
                showRetryPage();
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }

    private void showRetryPage() {
        mTitle.setText(getString(R.string.wizard_setup_found_error));
        mStart.setText(getString(R.string.wizard_try));
        mRemoteAccess.setVisibility(View.VISIBLE);
    }

    private void startNASFinderActivity(ArrayList<HashMap<String, String>> list) {
        Intent intent = new Intent();
        intent.setClass(InitialActivity.this, NASFinderActivity.class);
        intent.putExtra("NASList", list);
        intent.putExtra("RemoteAccess", false);
        intent.putExtra("Wizard", true);
        startActivity(intent);
        finish();
    }

    private void startSignInActivity() {
        Intent intent = new Intent();
        intent.setClass(InitialActivity.this, SignInActivity.class);
        intent.putExtra("Wizard", true);
        startActivity(intent);
        finish();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id){
            case R.id.initial_started_button:
                getLoaderManager().restartLoader(LoaderID.NAS_LIST, null, this).forceLoad();
                break;
            case R.id.initial_remote_access_button:
                startSignInActivity();
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if(mLoaderID >= 0){
            getLoaderManager().destroyLoader(mLoaderID);
        }

        if(mProgressView.isShown())
            mProgressView.setVisibility(View.INVISIBLE);
        else
            super.onBackPressed();
    }

}
