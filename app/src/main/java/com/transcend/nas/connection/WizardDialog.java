package com.transcend.nas.connection;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.transcend.nas.NASPref;
import com.transcend.nas.R;

/**
 * Created by ikelee on 16/9/1.
 */
public abstract class WizardDialog implements View.OnClickListener {

    private static final String TAG = WizardDialog.class.getSimpleName();

    public abstract void onConfirm(Bundle args);

    public abstract void onCancel();

    private AppCompatActivity mActivity;
    private AlertDialog mDialog;
    private Button mDlgBtnPos;
    private Button mDlgBtnNeg;
    private RelativeLayout mProgressView;
    private TextView tvUsername;
    private AppCompatEditText etPassword;
    private AppCompatEditText etConfirmPassword;
    private LinearLayout mWizardLayout;
    private LinearLayout mFinishLayout;
    private boolean isFinish = false;

    private Bundle mArgs;
    private String mAccount;

    public WizardDialog(Context context, Bundle args) {
        mActivity = (AppCompatActivity) context;
        mArgs = args;
        mAccount = args.getString("username", NASPref.defaultUserName);
        initDialog();
        initFieldAccount();
        initFieldPassword();
        initFieldConfirmPassword();
        initProgressView();
        initView();
    }

    private void initDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(R.string.wizard_setup_start);
        builder.setView(R.layout.dialog_wizard);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.ok, null);
        builder.setNegativeButton(R.string.cancel, null);
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
        mDlgBtnPos.setOnClickListener(this);
        mDlgBtnNeg.setOnClickListener(this);
        mDialog.setCanceledOnTouchOutside(false);
    }

    private void initFieldAccount() {
        tvUsername = (TextView) mDialog.findViewById(R.id.dialog_wizard_account);
        tvUsername.setText(mActivity.getString(R.string.account) + " : " + mAccount);
    }

    private void initFieldPassword() {
        etPassword = (AppCompatEditText) mDialog.findViewById(R.id.dialog_wizard_password);
    }

    private void initFieldConfirmPassword() {
        etConfirmPassword = (AppCompatEditText) mDialog.findViewById(R.id.dialog_wizard_confirm_password);
    }

    private void initProgressView() {
        mProgressView = (RelativeLayout) mDialog.findViewById(R.id.dialog_wizard_progress_view);
    }

    private void initView(){
        mWizardLayout = (LinearLayout) mDialog.findViewById(R.id.dialog_wizard_content);
        mFinishLayout = (LinearLayout) mDialog.findViewById(R.id.dialog_wizard_finish_layout);
    }

    public void showFinishView(){
        hideProgress();
        mDialog.setTitle(mActivity.getString(R.string.wizard_success));
        mDlgBtnNeg.setVisibility(View.GONE);
        mFinishLayout.setVisibility(View.VISIBLE);
        mWizardLayout.setVisibility(View.GONE);
        isFinish = true;
    }

    @Override
    public void onClick(View v) {
        if (v.equals(mDlgBtnPos)) {
            if(!isFinish) {
                String pwd = etPassword.getText().toString();
                String confirm = etConfirmPassword.getText().toString();
                if (pwd.equals("")) {
                    Toast.makeText(mActivity, mActivity.getString(R.string.empty_password), Toast.LENGTH_SHORT).show();
                    return;
                } else if (pwd.length() < 1 || pwd.length() > 32) {
                    Toast.makeText(mActivity, mActivity.getString(R.string.password_size) + " 1 ~ 32", Toast.LENGTH_SHORT).show();
                    return;
                } else if (pwd.contains(" ")) {
                    Toast.makeText(mActivity, mActivity.getString(R.string.wizard_password_space_error), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!pwd.equals(confirm)) {
                    Toast.makeText(mActivity, mActivity.getString(R.string.confirm_password_error), Toast.LENGTH_SHORT).show();
                    return;
                }

                showProgress();
                mArgs.putString("username", mAccount);
                mArgs.putString("password", pwd);
                onConfirm(mArgs);
            } else {
                mArgs.putBoolean("finish", true);
                onConfirm(mArgs);
            }
        } else if (v.equals(mDlgBtnNeg)) {
            hideProgress();
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
