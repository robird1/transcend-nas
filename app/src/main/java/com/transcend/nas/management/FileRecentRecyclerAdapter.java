package com.transcend.nas.management;

import android.content.Context;
import android.view.View;

import com.transcend.nas.R;

import java.util.ArrayList;

/**
 * Created by ikelee on 17/3/30.
 */
public class FileRecentRecyclerAdapter extends FileManageRecyclerAdapter {

    public FileRecentRecyclerAdapter(Context context, ArrayList<FileInfo> list) {
        super(context, list);
    }

    @Override
    public void onBindViewHolder(FileRecentRecyclerAdapter.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        if (holder.info != null) {
            holder.info.setImageResource(R.drawable.ic_toolbar_close_gray);
            holder.info.setAlpha(0.7f);
            //holder.info.setVisibility(View.INVISIBLE);
        }
    }
}
