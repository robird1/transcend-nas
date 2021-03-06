package com.transcend.nas.management;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;

import com.transcend.nas.R;

import java.util.ArrayList;

/**
 * Created by silverhsu on 16/2/1.
 */
public abstract class FileActionDeleteDialog implements View.OnClickListener {

    public abstract void onConfirm(ArrayList<String> paths);

    private Context mContext;
    private AlertDialog mDialog;
    private Button mDlgBtnPos;

    private ArrayList<String> mPaths;

    public FileActionDeleteDialog(Context context, ArrayList<String> paths) {
        mContext = context;
        mPaths = paths;
        initDialog();
    }

    private void initDialog() {
        String format = mContext.getResources().getString(R.string.msg_conj_deleted);
        String message = String.format(format, mPaths.size());
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getResources().getString(R.string.delete));
        builder.setIcon(R.drawable.ic_toolbar_delete_gray);
        builder.setMessage(message);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.confirm, null);
        builder.setCancelable(true);
        mDialog = builder.show();
        mDlgBtnPos = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        mDlgBtnPos.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.equals(mDlgBtnPos)) {
            onConfirm(mPaths);
            mDialog.dismiss();
        }
    }

}
