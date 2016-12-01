package com.transcend.nas.settings;

import android.graphics.Color;
import android.os.Environment;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.transcend.nas.NASApp;
import com.transcend.nas.R;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by ikelee on 16/7/28.
 */
public class DiskInfoDropdownAdapter extends BaseAdapter {

    private static final String TAG = DiskInfoDropdownAdapter.class.getSimpleName();
    private Spinner mDropdown;
    private List<String> mList;
    private OnDropdownItemSelectedListener mCallback;

    public interface OnDropdownItemSelectedListener {
        void onDropdownItemSelected(int position);
    }

    public DiskInfoDropdownAdapter() {

    }

    public void setContentList(List<String> lists){
        mList = lists;
    }

    public void setOnDropdownItemSelectedListener(OnDropdownItemSelectedListener l) {
        mCallback = l;
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

        ((TextView)convertView).setText(parent.getContext().getString(R.string.disk_info));
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

        ImageView iv = ViewHolder.get(convertView, R.id.dropdown_icon);
        iv.setImageResource(R.drawable.ic_storage_white_24dp);
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
