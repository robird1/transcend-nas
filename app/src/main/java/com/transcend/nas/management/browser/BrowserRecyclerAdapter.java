package com.transcend.nas.management.browser;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Filter;
import android.widget.Filterable;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.FileManageRecyclerAdapter;
import com.transcend.nas.management.firmware.PhotoFactory;

import java.util.ArrayList;
import java.util.List;

import static android.R.attr.thumbnail;

/**
 * Created by steve_su on 2017/8/17.
 */

public class BrowserRecyclerAdapter extends FileManageRecyclerAdapter implements Filterable {
    private static final String TAG = BrowserRecyclerAdapter.class.getSimpleName();
    ValueFilter valueFilter;
    ArrayList<FileInfo> mStringFilterList;

    public BrowserRecyclerAdapter(Context context, ArrayList<FileInfo> list) {
        super(context, list);
//        mStringFilterList = mList;
    }

    @Override
    public void updateList(ArrayList list) {
        super.updateList(list);
        mStringFilterList = list;
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
                    holder.subtitle.setText(String.valueOf(fileInfo.twonkyIndexCount) + " " + getTwonkyFolderUnit(fileInfo));
                } else {
                    String time = fileInfo.time;
                    // show total count under the index folder
                    if (!TextUtils.isEmpty(time)) {
                        holder.subtitle.setVisibility(View.VISIBLE);
                        holder.subtitle.setText(time);
                    }
                }
            }

//            Log.d(TAG, "fileInfo.thumbnail: " + fileInfo.thumbnail);
            if (!TextUtils.isEmpty(fileInfo.thumbnail)) {
//                Log.d(TAG, "[Enter] display photo...");
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
//        Log.d(TAG, "fileInfo.path: "+ fileInfo.path);
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
                } else if ("get_music_artists".equals(apiName) || "get_music_genre".equals(apiName)) {
                    unit = "albums";
                }
            }
        }
        return unit;
    }

    @Override
    public Filter getFilter() {
        if (valueFilter == null) {
            valueFilter = new ValueFilter();
        }
        return valueFilter;
    }


    private class ValueFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            Log.d(TAG, "[Enter] performFiltering constraint: "+ constraint);
            Log.d(TAG, "mStringFilterList.size: "+ mStringFilterList.size());

            FilterResults results = new FilterResults();

            if (constraint != null && constraint.length() > 0) {
                ArrayList<FileInfo> filterList = new ArrayList<>();
                for (int i = 0; i < mStringFilterList.size(); i++) {
                    String name = mStringFilterList.get(i).name.toUpperCase();
                    if (name.contains(constraint.toString().toUpperCase())) {
                        filterList.add(mStringFilterList.get(i));
                    }
                }
                results.count = filterList.size();
                results.values = filterList;
            } else {                                               // TODO check else section
                results.count = mStringFilterList.size();
                results.values = mStringFilterList;
            }
            return results;

        }

        @Override
        protected void publishResults(CharSequence constraint,
                                      FilterResults results) {
            Log.d(TAG, "[Enter] publishResults");

            mList = (ArrayList<FileInfo>) results.values;

            Log.d(TAG, "mList.size: "+ mList.size());

            notifyDataSetChanged();
        }

    }

}
