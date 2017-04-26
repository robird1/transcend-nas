package com.transcend.nas.management;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.transcend.nas.R;
import com.transcend.nas.management.firmware.FileFactory;
import com.transcend.nas.management.firmware.PhotoFactory;

import java.util.ArrayList;

/**
 * Created by silverhsu on 16/1/14.
 */
public class FileManageRecyclerAdapter extends RecyclerView.Adapter<FileManageRecyclerAdapter.ViewHolder> {

    private static final int ITEM_VIEW_TYPE_CONTENT = 0;
    private static final int ITEM_VIEW_TYPE_FOOTER  = 1;

    private Context mContext;
    private ArrayList<FileInfo> mList;
    private LayoutType mLayoutType = LayoutType.LIST;

    private OnRecyclerItemCallbackListener mCallback;

    public enum LayoutType{
        LIST, GRID
    }

    public interface OnRecyclerItemCallbackListener {
        void onRecyclerItemClick(int position);
        void onRecyclerItemLongClick(int position);
        void onRecyclerItemInfoClick(int position);
        void onRecyclerItemIconClick(int position);
    }

    public FileManageRecyclerAdapter(Context context, ArrayList<FileInfo> list) {
        mContext = context;
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
            mLayoutType = LayoutType.LIST;
            if (((RecyclerView) parent).getLayoutManager() instanceof GridLayoutManager) {
                resource = R.layout.griditem_file_manage;
                mLayoutType = LayoutType.GRID;
            }
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
            int resId = getPreviewResourceId(fileInfo.type);
            if (holder.title != null) {
                holder.title.setText(name);
                holder.title.setVisibility(((fileInfo.type.equals(FileInfo.TYPE.PHOTO) || fileInfo.type.equals(FileInfo.TYPE.VIDEO))
                        && mLayoutType == LayoutType.GRID) ? View.GONE : View.VISIBLE);
                if(mLayoutType == LayoutType.GRID)
                    holder.title.setGravity(Gravity.CENTER);
                else
                    holder.title.setGravity((time == null || "".equals(time)) ? Gravity.CENTER_VERTICAL : Gravity.BOTTOM);
            }
            if (holder.subtitle != null) {
                holder.subtitle.setVisibility((time == null || "".equals(time)) ? View.GONE : View.VISIBLE);
                holder.subtitle.setText(time);
            }
            if(holder.info != null){
                if (fileInfo.type.equals(FileInfo.TYPE.DIR)) {
                    holder.info.setImageResource(R.drawable.ic_navi_backaarow_gray);
                    holder.info.setRotation(180);
                }
                else {
                    holder.info.setImageResource(R.drawable.ic_toolbar_info_gray);
                    holder.info.setRotation(0);
                }
            }
            if (holder.icon != null)
                holder.icon.setImageResource(resId);
            if (holder.indicate != null){
                holder.indicate.setVisibility( fileInfo.type.equals(FileInfo.TYPE.VIDEO) ? View.VISIBLE : View.GONE);
            }
            if (fileInfo.type.equals(FileInfo.TYPE.PHOTO) || fileInfo.type.equals(FileInfo.TYPE.VIDEO) || fileInfo.type.equals(FileInfo.TYPE.MUSIC) )
                PhotoFactory.getInstance().displayPhoto(mContext, true, path, holder.icon);
            else
                ImageLoader.getInstance().cancelDisplayTask(holder.icon);


            //holder.itemView.setSelected(fileInfo.checked);
            holder.background.setVisibility(fileInfo.checked ? View.VISIBLE : View.INVISIBLE);
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

    private int getPreviewResourceId(FileInfo.TYPE type){
        int resId = (mLayoutType == LayoutType.GRID) ? R.drawable.ic_file_gray_big : R.drawable.ic_file_gray_24dp;
        switch(mLayoutType) {
            case GRID:
                if (type.equals(FileInfo.TYPE.DIR))
                    resId = R.drawable.ic_folder_gray_big;
                else if (type.equals(FileInfo.TYPE.PHOTO))
                    resId = R.drawable.ic_image_gray_big;
                else if (type.equals(FileInfo.TYPE.VIDEO))
                    resId = R.drawable.ic_movies_gray_big;
                else if (type.equals(FileInfo.TYPE.MUSIC))
                    resId = R.drawable.ic_audiotrack_gray_big;
                break;
            default:
                if (type.equals(FileInfo.TYPE.DIR))
                    resId = R.drawable.ic_folder_gray_24dp;
                else if (type.equals(FileInfo.TYPE.PHOTO))
                    resId = R.drawable.ic_image_gray_24dp;
                else if (type.equals(FileInfo.TYPE.VIDEO))
                    resId = R.drawable.ic_movies_gray_24dp;
                else if (type.equals(FileInfo.TYPE.MUSIC))
                    resId = R.drawable.ic_audiotrack_gray_24dp;
                    break;
        }

        return resId;
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        int viewType;

        View itemView;
        View background;
        ImageView mark;
        ImageView icon;
        ImageView info;
        ImageView indicate;
        TextView title;
        TextView subtitle;

        public ViewHolder(View itemView, int viewType) {
            super(itemView);
            this.viewType = viewType;
            this.itemView = itemView;
            if (viewType == ITEM_VIEW_TYPE_CONTENT) {
                if (itemView.getId() == R.id.listitem_file_manage) {
                    background = (View) itemView.findViewById(R.id.listitem_file_manage_background);
                    mark = (ImageView)itemView.findViewById(R.id.listitem_file_manage_mark);
                    icon = (ImageView)itemView.findViewById(R.id.listitem_file_manage_icon);
                    info = (ImageView)itemView.findViewById(R.id.listitem_file_manage_info);
                    title = (TextView)itemView.findViewById(R.id.listitem_file_manage_title);
                    subtitle = (TextView)itemView.findViewById(R.id.listitem_file_manage_subtitle);
                    setOnItemInfoClickListener();
                }
                if (itemView.getId() == R.id.griditem_file_manage) {
                    background = (View) itemView.findViewById(R.id.griditem_file_manage_background);
                    mark = (ImageView)itemView.findViewById(R.id.griditem_file_manage_mark);
                    icon = (ImageView)itemView.findViewById(R.id.griditem_file_manage_icon);
                    title = (TextView)itemView.findViewById(R.id.griditem_file_manage_title);
                    indicate = (ImageView)itemView.findViewById(R.id.griditem_file_indicate_icon);
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

        private void setOnItemInfoClickListener() {
            info.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCallback == null) return;
                    int position = getAdapterPosition();
                    mCallback.onRecyclerItemInfoClick(position);
                }
            });

            icon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCallback == null) return;
                    int position = getAdapterPosition();
                    mCallback.onRecyclerItemIconClick(position);
                }
            });
        }

    }
}
