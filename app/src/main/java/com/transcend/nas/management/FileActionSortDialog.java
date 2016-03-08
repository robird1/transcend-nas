package com.transcend.nas.management;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import com.transcend.nas.NASPref;
import com.transcend.nas.R;

/**
 * Created by silverhsu on 16/1/26.
 */
public abstract class FileActionSortDialog implements DialogInterface.OnClickListener {

    public abstract void onConfirm();

    private Context mContext;
    private AlertDialog mDialog;

    public FileActionSortDialog(Context context) {
        mContext = context;
        initDialog();
        initPreference();
    }

    private void initDialog() {
        String[] items = mContext.getResources().getStringArray(R.array.sort_items);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getResources().getString(R.string.sort_by));
        builder.setIcon(R.drawable.ic_sort_gray_24dp);
        builder.setSingleChoiceItems(items, 0, this);
        builder.setCancelable(true);
        mDialog = builder.show();
    }

    private void initPreference() {
        int pos = NASPref.getFileSortType(mContext).ordinal();
        mDialog.getListView().setItemChecked(pos, true);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        NASPref.Sort sort = NASPref.Sort.values()[which];
        NASPref.setFileSortType(mContext, sort);
        onConfirm();
        mDialog.dismiss();
    }

}
