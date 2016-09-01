package com.transcend.nas.connection;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.transcend.nas.R;
import com.transcend.nas.common.AnalysisFactory;
import com.transcend.nas.common.LoaderID;
import com.transcend.nas.common.StyleFactory;

import java.util.ArrayList;
import java.util.HashMap;

public class GuideActivity extends Activity implements LoaderManager.LoaderCallbacks<Boolean>, View.OnClickListener {

    private static final String TAG = GuideActivity.class.getSimpleName();
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
        AnalysisFactory.getInstance(this).sendScreen(AnalysisFactory.VIEW.GUIDE);
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
        AnalysisFactory.getInstance(this).recordStartTime();
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
        AnalysisFactory.getInstance(this).recordEndTime();
        mProgressView.setVisibility(View.INVISIBLE);
        if (loader instanceof NASListLoader) {
            AnalysisFactory.getInstance(this).sendTimeEvent(AnalysisFactory.EVENT.CONNECT ,AnalysisFactory.ACTION.FINDLOCAL, success);
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
        intent.setClass(GuideActivity.this, NASListActivity.class);
        intent.putExtra("NASList", list);
        intent.putExtra("RemoteAccess", false);
        intent.putExtra("Wizard", true);
        startActivity(intent);
        finish();
    }

    private void startSignInActivity() {
        Intent intent = new Intent();
        intent.setClass(GuideActivity.this, StartActivity.class);
        intent.putExtra("Initial", true);
        startActivity(intent);
        finish();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id){
            case R.id.initial_started_button:
                AnalysisFactory.getInstance(this).sendClickEvent(AnalysisFactory.VIEW.GUIDE, AnalysisFactory.ACTION.FINDLOCAL);
                getLoaderManager().restartLoader(LoaderID.NAS_LIST, null, this).forceLoad();
                break;
            case R.id.initial_remote_access_button:
                AnalysisFactory.getInstance(this).sendClickEvent(AnalysisFactory.VIEW.GUIDE, AnalysisFactory.ACTION.STARTREMOTE);
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
