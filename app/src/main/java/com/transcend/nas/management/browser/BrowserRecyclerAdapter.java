package com.transcend.nas.management.browser;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;

import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.FileManageRecyclerAdapter;
import com.transcend.nas.management.firmware.PhotoFactory;

import java.util.ArrayList;

/**
 * Created by steve_su on 2017/8/17.
 */

public class BrowserRecyclerAdapter extends FileManageRecyclerAdapter {

    public BrowserRecyclerAdapter(Context context, ArrayList<FileInfo> list) {
        super(context, list);
    }

    @Override
    public void updateList(ArrayList<FileInfo> list) {
        super.updateList(list);
    }

    @Override
    public void onBindViewHolder(FileManageRecyclerAdapter.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        if (holder.viewType == ITEM_VIEW_TYPE_CONTENT) {
            FileInfo fileInfo = mList.get(position);
            if (holder.subtitle != null) {

                // show total count under the index folder
                if (fileInfo.isTwonkyIndexFolder && fileInfo.twonkyIndexCount != 0) {
                    holder.title.setGravity(Gravity.BOTTOM);
                    holder.subtitle.setVisibility(View.VISIBLE);
                    holder.subtitle.setText(String.valueOf(fileInfo.twonkyIndexCount) + " " +
                            getTwonkyFolderUnit(fileInfo));
                } else {
                    String time = fileInfo.time;
                    // show total count under the index folder
                    if (!TextUtils.isEmpty(time)) {
                        holder.subtitle.setVisibility(View.VISIBLE);
                        holder.subtitle.setText(time);
                    }
                }
            }

            if (!TextUtils.isEmpty(fileInfo.thumbnail)) {
                PhotoFactory.getInstance().displayPhoto(fileInfo.thumbnail, holder.icon);
            }

            // show video subtitle
            if (fileInfo.type.equals(FileInfo.TYPE.VIDEO) && mLayoutType == LayoutType.GRID) {
                holder.title.setVisibility(View.VISIBLE);
                holder.title.setText(fileInfo.name);

                // disable indicate icon
                if (holder.indicate != null) {
                    holder.indicate.setVisibility(View.GONE);
                }
            }
        }
    }

    @NonNull
    private String getTwonkyFolderUnit(FileInfo fileInfo) {
        String unit = "";
        if (fileInfo.path != null) {
            if (fileInfo.path.startsWith("||get_photo")) {
                unit = "photos";
            } else if (fileInfo.path.startsWith("||get_video")) {
                unit = "videos";
            } else if (fileInfo.path.startsWith("||get_music")) {
                String apiName = RequestAction.getAPIName(fileInfo.path);
                if ("get_music_album".equals(apiName)) {
                    unit = "songs";
                } else if ("get_music_artists".equals(apiName) ||
                        "get_music_genre".equals(apiName)) {
                    unit = "albums";
                }
            }
        }
        return unit;
    }

}
