package com.transcend.nas.viewer.music;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import com.transcend.nas.R;
import com.transcend.nas.management.FileInfo;

import java.util.ArrayList;

/**
 * Created by ikelee on 17/4/7.
 */
public abstract class MusicListDialog implements DialogInterface.OnClickListener {

    public abstract void onConfirm(int position);

    private Context mContext;
    private AlertDialog mDialog;
    private ArrayList<FileInfo> mList;

    public MusicListDialog(Context context, ArrayList<FileInfo> list) {
        this(context, list, 0);
    }

    public MusicListDialog(Context context, ArrayList<FileInfo> list, int index) {
        mContext = context;
        mList = list;
        initDialog(index);
    }

    private void initDialog(int index) {
        if (mList != null && mList.size() > 0) {
            int position = Math.max(index, 0);
            int length = mList.size();
            String[] items = new String[length];
            for (int i = 0; i < length; i++) {
                items[i] = mList.get(i).name;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle("List");
            builder.setIcon(R.drawable.ic_audiotrack_gray_24dp);
            builder.setSingleChoiceItems(items, position, this);
            builder.setCancelable(true);
            mDialog = builder.show();
            mDialog.getListView().setItemChecked(position, true);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        onConfirm(which);
        mDialog.dismiss();
    }

}
