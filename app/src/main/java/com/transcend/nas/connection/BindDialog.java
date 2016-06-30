package com.transcend.nas.connection;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.support.v7.widget.AppCompatEditText;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.common.NotificationDialog;

/**
 * Created by silverhsu on 16/1/4.
 */
public abstract class BindDialog implements View.OnClickListener {

    private static final String TAG = BindDialog.class.getSimpleName();

    public abstract void onConfirm(Bundle args);

    public abstract void onCancel();

    private AppCompatActivity mActivity;
    private AlertDialog mDialog;
    private Button mDlgBtnPos;
    private Button mDlgBtnNeg;
    private RelativeLayout mProgressView;
    private AppCompatEditText etPassword;
    private Bundle mArgs;

    public BindDialog(Context context, Bundle args) {
        mActivity = (AppCompatActivity) context;
        mArgs = args;
        initDialog();
        initFieldPassword();
        initProgressView();
    }

    private void initDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setMessage(mActivity.getString(R.string.msg_enteradminpw));
        builder.setView(R.layout.dialog_bind);
        builder.setCancelable(true);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.ok, null);
        mDialog = builder.show();
        mDlgBtnPos = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        mDlgBtnNeg = mDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        mDlgBtnPos.setOnClickListener(this);
        mDlgBtnNeg.setOnClickListener(this);
    }

    private void initFieldPassword() {
        etPassword = (AppCompatEditText) mDialog.findViewById(R.id.dialog_bind_password);
    }

    private void initProgressView() {
        mProgressView = (RelativeLayout) mDialog.findViewById(R.id.dialog_bind_progress_view);
    }

    @Override
    public void onClick(View v) {
        if (v.equals(mDlgBtnPos)) {
            String username = "admin";
            String password = etPassword.getText().toString();

            if(password.equals("")){
                Toast.makeText(mActivity, mActivity.getString(R.string.empty_password),Toast.LENGTH_SHORT).show();
                return;
            }

            showProgress();
            Server server = ServerManager.INSTANCE.getCurrentServer();
            Bundle args = mArgs;
            args.putString("hostname", server.getHostname());
            args.putString("username", username);
            args.putString("password", password);
            args.putString("nasName", server.getServerInfo().hostName);
            onConfirm(args);
        } else if (v.equals(mDlgBtnNeg)) {
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
