package com.transcend.nas.connection;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.support.v7.widget.AppCompatEditText;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.transcend.nas.R;
import com.transcend.nas.common.LoaderID;
import com.transcend.nas.common.NotificationDialog;

/**
 * Created by ikelee on 16/4/1.
 */
public abstract class ForgetPwdDialog implements View.OnClickListener {

    private static final String TAG = ForgetPwdDialog.class.getSimpleName();

    public abstract void onConfirm(Bundle args);
    public abstract void onCancel();

    private AppCompatActivity mActivity;
    private AlertDialog mDialog;
    private Button mDlgBtnPos;
    private Button mDlgBtnNeg;
    private RelativeLayout mProgressView;
    private AppCompatEditText etEmail;

    private String mTitle;
    private String mEmail;

    public ForgetPwdDialog(Context context, Bundle args) {
        mActivity = (AppCompatActivity)context;
        mTitle = args.getString("title");
        mEmail = args.getString("email");
        initDialog();
        initFieldEmail();
        initProgressView();
    }

    private void initDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(mTitle);
        builder.setView(R.layout.dialog_login_forget);
        builder.setCancelable(true);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.send, null);
        mDialog = builder.show();
        mDlgBtnPos = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        mDlgBtnNeg = mDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        mDlgBtnPos.setOnClickListener(this);
        mDlgBtnNeg.setOnClickListener(this);
    }

    private void initFieldEmail() {
        etEmail = (AppCompatEditText)mDialog.findViewById(R.id.dialog_login_forget_email);
        etEmail.setText(mEmail);
    }

    private void initProgressView() {
        mProgressView = (RelativeLayout)mDialog.findViewById(R.id.dialog_login_forget_progress_view);
    }

    @Override
    public void onClick(View v) {
        if (v.equals(mDlgBtnPos)) {
            String email = etEmail.getText().toString();
            if(!email.equals("")) {
                InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                showNotificationDialog();
            }
            else {
                Toast.makeText(mActivity, mActivity.getString(R.string.empty_email) ,Toast.LENGTH_SHORT).show();
            }
        }
        if (v.equals(mDlgBtnNeg)) {
            hideProgress();
            mDialog.dismiss();
            onCancel();
        }
    }

    private void showNotificationDialog() {
        Bundle value = new Bundle();
        value.putString(NotificationDialog.DIALOG_MESSAGE, mActivity.getString(R.string.forget_password_warning));
        NotificationDialog mNotificationDialog = new NotificationDialog(mActivity, value) {
            @Override
            public void onConfirm() {
                showProgress();
                doConfirm();
            }

            @Override
            public void onCancel() {

            }
        };
    }

    private void doConfirm(){
        Bundle args = new Bundle();
        args.putString("email", etEmail.getText().toString());
        onConfirm(args);
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
