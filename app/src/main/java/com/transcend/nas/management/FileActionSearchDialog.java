package com.transcend.nas.management;

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
import android.widget.Toast;

import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.common.LoaderID;
import com.transcend.nas.common.NotificationDialog;
import com.transcend.nas.management.FileManageActivity;
import com.tutk.IOTC.P2PService;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by silverhsu on 16/1/4.
 */
public abstract class FileActionSearchDialog implements View.OnClickListener {

    private static final String TAG = FileActionSearchDialog.class.getSimpleName();

    public abstract void onConfirm(String keyword);

    private AppCompatActivity mActivity;
    private AlertDialog mDialog;
    private Button mDlgBtnPos;
    private Button mDlgBtnNeg;
    private RelativeLayout mProgressView;
    private AppCompatEditText etSearch;
    private String mKeyword;

    public FileActionSearchDialog(Context context) {
        mActivity = (AppCompatActivity) context;
        initDialog();
        initField();
        initProgressView();
    }

    private void initDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setView(R.layout.dialog_search);
        builder.setTitle(mActivity.getResources().getString(R.string.search));
        builder.setIcon(R.drawable.ic_search_gray_24dp);
        builder.setCancelable(true);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.search, null);
        mDialog = builder.show();
        mDlgBtnPos = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        mDlgBtnNeg = mDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        mDlgBtnPos.setOnClickListener(this);
        mDlgBtnNeg.setOnClickListener(this);
    }

    private void initField() {
        etSearch = (AppCompatEditText) mDialog.findViewById(R.id.dialog_search_text);
    }

    private void initProgressView() {
        mProgressView = (RelativeLayout) mDialog.findViewById(R.id.dialog_search_progress_view);
    }

    @Override
    public void onClick(View v) {
        if (v.equals(mDlgBtnPos)) {
            String keyword = etSearch.getText().toString();
            if(keyword.equals("")){
                dismiss();
                return;
            }

            showProgress();
            onConfirm(keyword);
        } else if (v.equals(mDlgBtnNeg)) {
            hideProgress();
            mDialog.dismiss();
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
