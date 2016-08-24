package com.transcend.nas.connection;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.support.v7.widget.AppCompatEditText;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.view.NotificationDialog;

/**
 * Created by silverhsu on 16/1/4.
 */
public abstract class LoginDialog implements View.OnClickListener {

    private static final String TAG = LoginDialog.class.getSimpleName();

    public abstract void onConfirm(Bundle args);

    public abstract void onCancel();

    public abstract void onDelete(Bundle args);

    private AppCompatActivity mActivity;
    private AlertDialog mDialog;
    private Button mDlgBtnPos;
    private Button mDlgBtnNeg;
    private Button mDlgBtnNeu;
    private RelativeLayout mProgressView;
    private AppCompatEditText etHostname;
    private AppCompatAutoCompleteTextView tvUsername;
    private AppCompatEditText etPassword;

    private String mNickname;
    private String mHostname;
    private String mUsername;
    private String mPassword;
    private String mNasId;
    private boolean isRemoeteAccess = false;
    private boolean isDelete = false;

    public LoginDialog(Context context, Bundle args, boolean isRemoteAccess, boolean isDelete) {
        mActivity = (AppCompatActivity) context;
        this.isRemoeteAccess = isRemoteAccess;
        this.isDelete = isDelete;
        mNasId = args.getString("nasId");
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
        if(!isDelete)
            builder.setPositiveButton(R.string.login, null);
        if (isRemoeteAccess)
            builder.setNeutralButton(R.string.delete, null);
        builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_BACK){
                    onCancel();
                    return true;
                }
                return false;
            }
        });
        mDialog = builder.show();
        mDlgBtnPos = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        mDlgBtnNeg = mDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        mDlgBtnNeu = mDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        mDlgBtnPos.setOnClickListener(this);
        mDlgBtnNeg.setOnClickListener(this);
        mDlgBtnNeu.setOnClickListener(this);
        mDialog.setCanceledOnTouchOutside(false);
    }

    private void initFieldIP() {
        etHostname = (AppCompatEditText) mDialog.findViewById(R.id.dialog_login_ip);
        etHostname.setText(mHostname);
    }

    private void initFieldAccount() {
        tvUsername = (AppCompatAutoCompleteTextView) mDialog.findViewById(R.id.dialog_login_account);
        tvUsername.setText(mUsername);
    }

    private void initFieldPassword() {
        etPassword = (AppCompatEditText) mDialog.findViewById(R.id.dialog_login_password);
        etPassword.setText(mPassword);
    }

    private void initProgressView() {
        mProgressView = (RelativeLayout) mDialog.findViewById(R.id.dialog_login_progress_view);
    }

    @Override
    public void onClick(View v) {
        if (v.equals(mDlgBtnPos)) {
            String hostname = etHostname.getText().toString();
            String username = tvUsername.getText().toString();
            String password = etPassword.getText().toString();
            if(username.equals("")){
                Toast.makeText(mActivity, mActivity.getString(R.string.empty_account),Toast.LENGTH_SHORT).show();
                return;
            }

            if(password.equals("")){
                Toast.makeText(mActivity, mActivity.getString(R.string.empty_password),Toast.LENGTH_SHORT).show();
                return;
            }

            showProgress();
            Bundle args = new Bundle();
            args.putString("hostname", hostname);
            args.putString("username", username);
            args.putString("password", password);
            Log.w(TAG, "hostname: " + args.get("hostname"));
            Log.w(TAG, "username: " + args.get("username"));
            Log.w(TAG, "password: " + args.get("password"));
            onConfirm(args);
        } else if (v.equals(mDlgBtnNeg)) {
            hideProgress();
            mDialog.dismiss();
            onCancel();
        } else if (v.equals(mDlgBtnNeu)) {
            showNotificationDialog();
        }
    }

    private void showNotificationDialog() {
        Bundle value = new Bundle();
        value.putString(NotificationDialog.DIALOG_MESSAGE, mActivity.getString(R.string.remote_access_delete_warning));
        NotificationDialog mNotificationDialog = new NotificationDialog(mActivity, value) {
            @Override
            public void onConfirm() {
                showProgress();
                doDelete();
            }

            @Override
            public void onCancel() {

            }
        };
    }

    private void doDelete() {
        showProgress();
        Bundle args = new Bundle();
        args.putString("server", NASPref.getCloudServer(mActivity));
        args.putString("token", NASPref.getCloudAuthToken(mActivity));
        args.putString("nasId", mNasId);
        onDelete(args);
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
