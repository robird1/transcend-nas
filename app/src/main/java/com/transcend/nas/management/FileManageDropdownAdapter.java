package com.transcend.nas.management;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.transcend.nas.NASApplication;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by silverhsu on 16/1/12.
 */
public class FileManageDropdownAdapter extends BaseAdapter {

    private static final String TAG = FileManageDropdownAdapter.class.getSimpleName();

    private static final String PREFIX_REMOTE = NASApplication.getContext().getResources().
            getString(R.string.app_name);
    private static final String PREFIX_LOCAL = NASApplication.getContext().getResources().
            getString(R.string.downloads_name);

    private Context mContext;
    private Spinner mDropdown;
    private List<String> mList;

    private OnDropdownItemSelectedListener mCallback;

    public interface OnDropdownItemSelectedListener {
        void onDropdownItemSelected(int position);
    }

    public FileManageDropdownAdapter(String path) {
        mContext = NASApplication.getContext();
        updateList(path);
    }

    public void setOnDropdownItemSelectedListener(OnDropdownItemSelectedListener l) {
        mCallback = l;
    }

    public void updateList(String path) {
        List<String> list = new ArrayList<String>();
        String downloadsPath = NASPref.getDownloadsPath(mContext);
        boolean isLocal = path.startsWith(downloadsPath);
        if (isLocal) {
            path = path.replaceFirst(downloadsPath, PREFIX_LOCAL);
        }
        else {
            path = PREFIX_REMOTE + path;
        }
        String[] items = path.split("/");
        list = Arrays.asList(items);
        Collections.reverse(list);
        mList = list;
    }

    public String getPath(int position) {
        List<String> list = mList.subList(position, mList.size());
        Collections.reverse(list);
        StringBuilder builder = new StringBuilder();
        for (String item : list) {
            builder.append(item);
            builder.append("/");
        }
        String path = builder.toString();
        boolean isLocal = path.startsWith(PREFIX_LOCAL);
        if (isLocal) {
            String downloadsPath = NASPref.getDownloadsPath(mContext);
            path = path.replaceFirst(PREFIX_LOCAL, downloadsPath);
        }
        else {
            path = path.replaceFirst(PREFIX_REMOTE, "");
        }
        return path;
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (parent instanceof Spinner)
            mDropdown = (Spinner)parent;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view = inflater.inflate(R.layout.dropdown, parent, false);
            convertView = ViewHolder.get(view, R.id.dropdown_text);
        }
        ((TextView)convertView).setText(mList.get(0));
        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, final ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(R.layout.dropdown, parent, false);
        }
        convertView.setOnTouchListener(new OnDropdownItemTouchListener(position));
        TextView tv = ViewHolder.get(convertView, R.id.dropdown_text);
        tv.setText(mList.get(position));
        return convertView;
    }

    public static class ViewHolder {
        @SuppressWarnings("unchecked")
        public static <T extends View> T get(View view, int id) {
            SparseArray<View> holder = (SparseArray<View>)view.getTag();
            if (holder == null) {
                holder = new SparseArray<View>();
                view.setTag(holder);
            }
            View child = holder.get(id);
            if (child == null) {
                child = view.findViewById(id);
                holder.put(id, child);
            }
            return (T) child;
        }
    }

    public class OnDropdownItemTouchListener implements View.OnTouchListener {

        private int mPosition;

        public OnDropdownItemTouchListener(int position) {
            mPosition = position;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (mCallback != null) {
                    mCallback.onDropdownItemSelected(mPosition);
                }
                dismissDropdownList();
            }
            return true;
        }

        /**
         *
         * In order to make dropdown list scrollable,
         * onDropdownItemSelected callback should be called in ACTION_UP instead of ACTION_DOWN.
         * That causes one problem that dropdown list would not dismiss automatically.
         * One solution is to detach spinner from window by reflection method,
         * and dropdown list will disappear.
         *
         */
        private void dismissDropdownList() {
            try {
                Method method = Spinner.class.getDeclaredMethod("onDetachedFromWindow");
                method.setAccessible(true);
                method.invoke(mDropdown);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
