package com.transcend.nas.view;

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
    public static String DIALOG_TITLE = "title";
    public static String DIALOG_MESSAGE = "message";
    public static String DIALOG_LAYOUT = "layout";

    private AppCompatActivity mActivity;
    private AlertDialog mDialog;
    private Button mDlgBtnPos;
    private Button mDlgBtnNeg;
    private String mTitle;
    private String mMessage;
    private int mLayoutID = 0;

    public NotificationDialog(Context context, Bundle args) {
        this(context, args, true, true);
    }

    public NotificationDialog(Context context, Bundle args, boolean showPositiveButton, boolean showNegativeButton) {
        mActivity = (AppCompatActivity)context;
        mTitle = args.getString(DIALOG_TITLE);
        mMessage = args.getString(DIALOG_MESSAGE);
        mLayoutID = args.getInt(DIALOG_LAYOUT);
        initDialog(showPositiveButton, showNegativeButton);
    }

    private void initDialog(boolean showPositiveButton, boolean showNegativeButton) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setCancelable(true);

        if(mLayoutID != 0)
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

        mDialog = builder.show();
        mDlgBtnPos = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if(mDlgBtnPos != null)
            mDlgBtnPos.setOnClickListener(this);
        mDlgBtnNeg = mDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        if(mDlgBtnNeg != null)
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
