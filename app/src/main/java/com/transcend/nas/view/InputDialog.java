package com.transcend.nas.view;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.transcend.nas.R;

/**
 * Created by ikelee on 17/4/19.
 */
public abstract class InputDialog implements View.OnClickListener {

    private static final String TAG = InputDialog.class.getSimpleName();

    public abstract void onConfirm(String inputText);
    public abstract void onCancel();

    private AppCompatActivity mActivity;
    private AlertDialog mDialog;
    private Button mDlgBtnPos;
    private Button mDlgBtnNeg;
    private AppCompatEditText mEditText;
    private RelativeLayout mProgressView;
    private Options mOption;

    public class Options {
        int icon;
        String title;
        String text;
        String positiveText;
        String negativeText;
    }

    public InputDialog(Context context) {
        this(context, null);
    }

    public InputDialog(Context context, Options option) {
        mActivity = (AppCompatActivity) context;
        mOption = option;
        initDialog();
        initField();
        initProgressView();
    }

    private void initDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setView(R.layout.dialog_input);
        builder.setCancelable(true);

        if (mOption != null && mOption.title != null)
            builder.setTitle(mOption.title);

        if (mOption != null && mOption.icon >= 0)
            builder.setIcon(mOption.icon);

        if (mOption != null && mOption.negativeText != null) {
            builder.setNegativeButton(mOption.negativeText, null);
            mDlgBtnNeg = mDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
            mDlgBtnNeg.setOnClickListener(this);
        }

        if (mOption != null && mOption.positiveText != null) {
            builder.setPositiveButton(mOption.positiveText, null);
            mDlgBtnPos = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            mDlgBtnPos.setOnClickListener(this);
        }

        builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (event.equals(KeyEvent.KEYCODE_BACK)) {
                    onCancel();
                    return true;
                }
                return false;
            }
        });

        mDialog = builder.show();
    }

    private void initField() {
        mEditText = (AppCompatEditText) mDialog.findViewById(R.id.dialog_text);
        if (mOption != null && mOption.text != null) {
            mEditText.setText(mOption.text);
        }
    }

    private void initProgressView() {
        mProgressView = (RelativeLayout) mDialog.findViewById(R.id.dialog_progress_view);
    }

    @Override
    public void onClick(View v) {
        if (v.equals(mDlgBtnPos)) {
            String text = mEditText.getText().toString();
            showProgress();
            onConfirm(text);
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
