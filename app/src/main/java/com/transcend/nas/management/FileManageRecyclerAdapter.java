package com.transcend.nas.management;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASApp;
import com.transcend.nas.R;

import java.util.ArrayList;

/**
 * Created by silverhsu on 16/1/14.
 */
public class FileManageRecyclerAdapter extends RecyclerView.Adapter<FileManageRecyclerAdapter.ViewHolder> {

    private static final int ITEM_VIEW_TYPE_CONTENT = 0;
    private static final int ITEM_VIEW_TYPE_FOOTER  = 1;

    private ArrayList<FileInfo> mList;

    private OnRecyclerItemCallbackListener mCallback;

    public interface OnRecyclerItemCallbackListener {
        void onRecyclerItemClick(int position);
        void onRecyclerItemLongClick(int position);
    }

    public FileManageRecyclerAdapter(ArrayList<FileInfo> list) {
        updateList(list);
    }

    public void setOnRecyclerItemCallbackListener(OnRecyclerItemCallbackListener l) {
        mCallback = l;
    }

    public void updateList(ArrayList<FileInfo> list) {
        mList = list;
    }

    @Override
    public FileManageRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_VIEW_TYPE_CONTENT) {
            int resource = R.layout.listitem_file_manage;
            if (((RecyclerView) parent).getLayoutManager() instanceof GridLayoutManager)
                resource = R.layout.griditem_file_manage;
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View view = layoutInflater.inflate(resource, parent, false);
            return new ViewHolder(view, viewType);
        }
        if (viewType == ITEM_VIEW_TYPE_FOOTER) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View view = layoutInflater.inflate(R.layout.footitem_file_manage, parent, false);
            return new ViewHolder(view, viewType);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(FileManageRecyclerAdapter.ViewHolder holder, int position) {
        if (holder.viewType == ITEM_VIEW_TYPE_CONTENT) {
            FileInfo fileInfo = mList.get(position);
            String name = fileInfo.name;
            String time = fileInfo.time;
            String path = fileInfo.path;
            int resId = R.drawable.ic_file_gray_24dp;
            if (fileInfo.type.equals(FileInfo.TYPE.DIR))
                resId = R.drawable.ic_folder_gray_24dp;
            else if (fileInfo.type.equals(FileInfo.TYPE.PHOTO))
                resId = R.drawable.ic_image_gray_24dp;
            else if (fileInfo.type.equals(FileInfo.TYPE.VIDEO))
                resId = R.drawable.ic_movies_gray_24dp;
            else if (fileInfo.type.equals(FileInfo.TYPE.MUSIC))
                resId = R.drawable.ic_audiotrack_gray_24dp;
            if (holder.title != null)
                holder.title.setText(name);
            if (holder.subtitle != null)
                holder.subtitle.setText(time);
            if (holder.icon != null)
                holder.icon.setImageResource(resId);
            if (fileInfo.type.equals(FileInfo.TYPE.PHOTO))
                ImageLoader.getInstance().displayImage(toPhotoURL(path), holder.icon);

            holder.itemView.setSelected(fileInfo.checked);
            holder.mark.setVisibility(fileInfo.checked ? View.VISIBLE : View.INVISIBLE);

        }
        if (holder.viewType == ITEM_VIEW_TYPE_FOOTER) {
            // do nothing
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isFooter(position))
            return ITEM_VIEW_TYPE_FOOTER;
        return ITEM_VIEW_TYPE_CONTENT;
    }

    @Override
    public int getItemCount() {
        return hasFooter() ? mList.size() + 1 : mList.size();
    }

    public boolean isFooter(int position) {
        return (hasFooter() && (position == mList.size()));
    }

    public boolean hasFooter() {
        return mList.size() > 0;
    }

    private String toPhotoURL(String path) {
        String url;
        if (path.startsWith(NASApp.ROOT_STG)) {
            url = "file://" + path;
        }
        else {
            Server server = ServerManager.INSTANCE.getCurrentServer();
            String hostname = server.getHostname();
            String filepath = path.replaceFirst(Server.HOME, "/");
            String hash = server.getHash();
            url = "http://" + hostname + "/dav/home/" + filepath + "?session=" + hash + "&thumbnail";
        }
        return url;
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        int viewType;

        View itemView;
        ImageView mark;
        ImageView icon;
        TextView title;
        TextView subtitle;

        public ViewHolder(View itemView, int viewType) {
            super(itemView);
            this.viewType = viewType;
            this.itemView = itemView;
            if (viewType == ITEM_VIEW_TYPE_CONTENT) {
                if (itemView.getId() == R.id.listitem_file_manage) {
                    mark = (ImageView)itemView.findViewById(R.id.listitem_file_manage_mark);
                    icon = (ImageView)itemView.findViewById(R.id.listitem_file_manage_icon);
                    title = (TextView)itemView.findViewById(R.id.listitem_file_manage_title);
                    subtitle = (TextView)itemView.findViewById(R.id.listitem_file_manage_subtitle);
                }
                if (itemView.getId() == R.id.griditem_file_manage) {
                    mark = (ImageView)itemView.findViewById(R.id.griditem_file_manage_mark);
                    icon = (ImageView)itemView.findViewById(R.id.griditem_file_manage_icon);
                    title = (TextView)itemView.findViewById(R.id.griditem_file_manage_title);
                }
                itemView.setOnClickListener(this);
                itemView.setOnLongClickListener(this);
            }
            if (viewType == ITEM_VIEW_TYPE_FOOTER) {
                // do nothing
            }
        }

        @Override
        public void onClick(View v) {
            if (mCallback != null) {
                int position = getAdapterPosition();
                mCallback.onRecyclerItemClick(position);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (mCallback != null) {
                int position = getAdapterPosition();
                mCallback.onRecyclerItemLongClick(position);
            }
            return true;
        }
    }

}
