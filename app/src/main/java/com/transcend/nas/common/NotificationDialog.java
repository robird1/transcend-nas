package com.transcend.nas.common;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.transcend.nas.R;

/**
 * Created by ikelee on 16/4/1.
 */
public abstract class NotificationDialog implements View.OnClickListener {

    private static final String TAG = NotificationDialog.class.getSimpleName();

    public abstract void onConfirm();
    public abstract void onCancel();

    private AppCompatActivity mActivity;
    private AlertDialog mDialog;
    private Button mDlgBtnPos;
    private Button mDlgBtnNeg;
    private String mTitle;

    public NotificationDialog(Context context, Bundle args) {
        mActivity = (AppCompatActivity)context;
        mTitle = args.getString("title");
        initDialog();
    }

    private void initDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setMessage(mTitle);
        builder.setCancelable(true);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.ok, null);
        mDialog = builder.show();
        mDlgBtnPos = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        mDlgBtnNeg = mDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        mDlgBtnPos.setOnClickListener(this);
        mDlgBtnNeg.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.equals(mDlgBtnPos)) {
            mDialog.dismiss();
            onConfirm();
        }

        if (v.equals(mDlgBtnNeg)) {
            mDialog.dismiss();
            onCancel();
        }
    }

    public void dismiss() {
        mDialog.dismiss();
    }

}
