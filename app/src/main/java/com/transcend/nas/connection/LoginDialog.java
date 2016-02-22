package com.transcend.nas.connection;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.support.v7.widget.AppCompatEditText;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.transcend.nas.R;
import com.transcend.nas.common.LoaderID;
import com.transcend.nas.management.FileManageActivity;
import com.tutk.IOTC.P2PService;

/**
 * Created by silverhsu on 16/1/4.
 */
public abstract class LoginDialog implements View.OnClickListener {

    private static final String TAG = LoginDialog.class.getSimpleName();

    public abstract void onConfirm(Bundle args);
    public abstract void onCancel();

    private AppCompatActivity mActivity;
    private AlertDialog mDialog;
    private Button mDlgBtnPos;
    private Button mDlgBtnNeg;
    private RelativeLayout mProgressView;
    private AppCompatEditText etHostname;
    private AppCompatAutoCompleteTextView tvUsername;
    private AppCompatEditText etPassword;

    private String mNickname;
    private String mHostname;
    private String mUsername;
    private String mPassword;

    public LoginDialog(Context context, Bundle args) {
        mActivity = (AppCompatActivity)context;
        mNickname = args.getString("nickname");
        mHostname = args.getString("hostname");
        mUsername = args.getString("username");
        mPassword = args.getString("password");
        initDialog();
        initFieldIP();
        initFieldAccount();
        initFieldPassword();
        initProgressView();
    }

    private void initDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(mNickname);
        builder.setView(R.layout.dialog_login);
        builder.setCancelable(true);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.login, null);
        mDialog = builder.show();
        mDlgBtnPos = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        mDlgBtnNeg = mDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        mDlgBtnPos.setOnClickListener(this);
        mDlgBtnNeg.setOnClickListener(this);
    }

    private void initFieldIP() {
        etHostname = (AppCompatEditText)mDialog.findViewById(R.id.dialog_login_ip);
        etHostname.setText(mHostname);
        //etIP.setKeyListener(null);
    }

    private void initFieldAccount() {
        tvUsername = (AppCompatAutoCompleteTextView)mDialog.findViewById(R.id.dialog_login_account);
        tvUsername.setText(mUsername);
    }

    private void initFieldPassword() {
        etPassword = (AppCompatEditText)mDialog.findViewById(R.id.dialog_login_password);
        etPassword.setText(mPassword);
    }

    private void initProgressView() {
        mProgressView = (RelativeLayout)mDialog.findViewById(R.id.dialog_login_progress_view);
    }

    @Override
    public void onClick(View v) {
        if (v.equals(mDlgBtnPos)) {
            showProgress();
            Bundle args = new Bundle();
            args.putString("hostname", etHostname.getText().toString());
            args.putString("username", tvUsername.getText().toString());
            args.putString("password", etPassword.getText().toString());
            Log.w(TAG, "hostname: " + args.get("hostname"));
            Log.w(TAG, "username: " + args.get("username"));
            Log.w(TAG, "password: " + args.get("password"));
            onConfirm(args);
        }
        if (v.equals(mDlgBtnNeg)) {
            hideProgress();
            mDialog.dismiss();
            onCancel();
        }
    }

    public void showProgress() {
        mProgressView.setVisibility(View.VISIBLE);
    }

    public void hideProgress() {
        mProgressView.setVisibility(View.INVISIBLE);
    }

    public void dismiss() {
        mDialog.dismiss();
    }

}
