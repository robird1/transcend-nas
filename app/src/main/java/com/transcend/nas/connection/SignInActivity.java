package com.transcend.nas.connection;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.transcend.nas.R;
import com.transcend.nas.common.LoaderID;

/**
 * Created by silverhsu on 16/2/2.
 */
public class SignInActivity extends Activity implements View.OnClickListener {

    private static final String TAG = SignInActivity.class.getSimpleName();

    private Button bnSignIn;
    private TextView tvFindDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        //mTextView = (TextView)findViewById(R.id.welcome_text);
        //getLoaderManager().initLoader(LoaderID.AUTO_LINK, null, this).forceLoad();
        initSignInButton();
        initFindDeviceTextView();
    }

    private void initSignInButton() {
        bnSignIn = (Button)findViewById(R.id.activity_sign_in_button);
        bnSignIn.setOnClickListener(this);
    }

    private void initFindDeviceTextView() {
        tvFindDevice = (TextView)findViewById(R.id.activity_sign_in_find_device);
        tvFindDevice.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.equals(bnSignIn)) {
            // sign in
        } else
        if (v.equals(tvFindDevice)) {
            startNASFinderActivity();
        }
    }

    private void startNASFinderActivity() {
        Intent intent = new Intent();
        intent.setClass(SignInActivity.this, NASFinderActivity.class);
        startActivity(intent);
        finish();
    }

}
