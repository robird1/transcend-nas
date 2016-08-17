package com.transcend.nas.common;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.transcend.nas.R;

/**
 * Created by ikelee on 16/4/1.
 */
public abstract class ProgressDialog implements View.OnClickListener {

    private static final String TAG = ProgressDialog.class.getSimpleName();

    public abstract void onConfirm();
    public abstract void onCancel();
    public static String DIALOG_TITLE = "title";
    public static String DIALOG_MESSAGE = "message";

    private AppCompatActivity mActivity;
    private AlertDialog mDialog;
    private Button mDlgBtnPos;
    private Button mDlgBtnNeg;
    private String mTitle;
    private String mMessage;
    private RelativeLayout mProgressView;
    private int mLayoutID = R.layout.dialog_progress;

    public ProgressDialog(Context context, Bundle args) {
        this(context, args, true, true);
    }

    public ProgressDialog(Context context, Bundle args, boolean showPositiveButton, boolean showNegativeButton) {
        mActivity = (AppCompatActivity)context;
        mTitle = args.getString(DIALOG_TITLE);
        mMessage = args.getString(DIALOG_MESSAGE);
        initDialog(showPositiveButton, showNegativeButton);
    }

    private void initDialog(boolean showPositiveButton, boolean showNegativeButton) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setCancelable(true);
        builder.setView(mLayoutID);

        if(mTitle != null && !mTitle.equals(""))
            builder.setTitle(mTitle);

        if(mMessage != null && !mMessage.equals(""))
            builder.setMessage(mMessage);

        if(showNegativeButton) {
            builder.setNegativeButton(R.string.cancel, null);
        }

        if(showPositiveButton) {
            builder.setPositiveButton(R.string.ok, null);
        }

        builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if(event.equals(KeyEvent.KEYCODE_BACK)){
                    onCancel();
                    return true;
                }
                return false;
            }
        });

        mDialog = builder.show();
        mDlgBtnPos = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if(mDlgBtnPos != null)
            mDlgBtnPos.setOnClickListener(this);
        mDlgBtnNeg = mDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        if(mDlgBtnNeg != null)
            mDlgBtnNeg.setOnClickListener(this);

        mProgressView = (RelativeLayout) mDialog.findViewById(R.id.dialog_progress_view);
    }

    @Override
    public void onClick(View v) {
        if (v.equals(mDlgBtnPos)) {
            mDialog.setTitle(null);
            mDialog.setMessage(null);
            showProgress();
            onConfirm();
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
