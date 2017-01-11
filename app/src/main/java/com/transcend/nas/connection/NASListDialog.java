package com.transcend.nas.connection;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;

import com.transcend.nas.R;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ikelee on 16/8/31.
 */
public abstract class NASListDialog implements View.OnClickListener {

    private static final String TAG = NASListDialog.class.getSimpleName();

    public abstract void onConfirm(Bundle args);

    public abstract void onCancel();

    private AppCompatActivity mActivity;
    private AlertDialog mDialog;
    private Button mDlgBtnPos;
    private Button mDlgBtnNeg;
    private Button mDlgBtnNeu;
    private RelativeLayout mProgressView;
    private ListView mListView;
    private SimpleAdapter mListViewAdapter;
    private RelativeLayout mEmptyView;
    private int mSelect = -1;
    private ArrayList<HashMap<String, String>> mList;
    private boolean mCheckList[];
    private boolean isEmpty = false;


    public NASListDialog(Context context, ArrayList<HashMap<String, String>> list) {
        mActivity = (AppCompatActivity) context;
        mList = list;
        initCheckList();
        initDialog();
        initListView();
        initEmptyView();
        initProgressView();
        showProgress();
    }

    private void initDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(mActivity.getString(R.string.find_nas));
        builder.setView(R.layout.dialog_list);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.ok, null);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setNeutralButton(R.string.wizard_try, null);
        builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    onClick(mDlgBtnNeg);
                    return true;
                }
                return false;
            }
        });
        mDialog = builder.show();
        mDlgBtnPos = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        mDlgBtnNeg = mDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        mDlgBtnNeu = mDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        mDlgBtnPos.setOnClickListener(this);
        mDlgBtnNeg.setOnClickListener(this);
        mDlgBtnNeu.setOnClickListener(this);
        mDialog.setCanceledOnTouchOutside(false);
    }

    private void initCheckList() {
        if (mList != null) {
            int size = mList.size();
            mCheckList = new boolean[size];
            for (int i = 0; i < size; i++) {
                mCheckList[i] = (i == 0);
            }
        }
    }

    private void initListView() {
        if (mListView == null) {
            mListView = (ListView) mDialog.findViewById(R.id.dialog_list_view);
            mListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
            mListView.setSelector(R.color.colorSelected);
            mListView.addFooterView(new View(mActivity));
        } else {
            String ID_TITLE = "title";
            String ID_SUBTITLE = "subTitle";
            ArrayList<HashMap<String, String>> myListData = new ArrayList<HashMap<String, String>>();
            for (HashMap<String, String> nas : mList) {
                HashMap<String, String> item = new HashMap<String, String>();
                item.put(ID_TITLE, nas.get("nickname"));
                item.put(ID_SUBTITLE, nas.get("hostname"));
                myListData.add(item);
            }

            mListViewAdapter = new SimpleAdapter(mActivity,
                    myListData,
                    R.layout.listitem_checkbox,
                    new String[]{ID_TITLE, ID_SUBTITLE},
                    new int[]{R.id.listitem_checkbox_title, R.id.llistitem_checkbox_subtitle}) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    CheckBox check = (CheckBox) view.findViewById(R.id.listitem_checkbox_icon);
                    check.setChecked(mCheckList[position]);
                    return view;
                }
            };

            mListView.setAdapter(mListViewAdapter);
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if(mSelect == position) {
                        onClick(mDlgBtnPos);
                    } else {
                        CheckBox check = (CheckBox) view.findViewById(R.id.listitem_checkbox_icon);
                        check.setChecked(true);

                        if(0 <= mSelect && mSelect < mCheckList.length) {
                            mCheckList[mSelect] = false;
                            View previous = mListView.getChildAt(mSelect);
                            if (previous != null) {
                                CheckBox tmp = (CheckBox) previous.findViewById(R.id.listitem_checkbox_icon);
                                if (tmp != null) {
                                    tmp.setChecked(false);
                                }
                            }
                        }

                        mSelect = position;
                        mCheckList[position] = true;
                    }
                }
            });

            if(!isEmpty) {
                int defaultValue = 0;
                mSelect = defaultValue;
                mCheckList[defaultValue] = true;

                //because we have checkbox in layout, call requestFocusFromTouch first
                mListView.requestFocusFromTouch();
                mListView.setSelection(defaultValue);

            }
        }
    }

    private void initEmptyView() {
        mEmptyView = (RelativeLayout) mDialog.findViewById(R.id.dialog_list_empty_view);
        mEmptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void initProgressView() {
        mProgressView = (RelativeLayout) mDialog.findViewById(R.id.dialog_list_progress_view);
    }

    public void updateListView(ArrayList<HashMap<String, String>> list) {
        mSelect = -1;
        if (list == null) {
            mDialog.setTitle(mActivity.getString(R.string.find_nas));
            showProgress();
        } else {
            mList = list;
            isEmpty = mList.size() == 0;
            mDialog.setTitle(mActivity.getString(isEmpty ? R.string.wizard_setup_found_error : R.string.add_device));
            initCheckList();
            initListView();
            initEmptyView();
            hideProgress();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.equals(mDlgBtnPos)) {
            if (mSelect >= 0) {
                showProgress();
                HashMap<String, String> item = mList.get(mSelect);
                Bundle args = new Bundle();
                args.putString("nickname", item.get("nickname"));
                args.putString("hostname", item.get("hostname"));
                onConfirm(args);
            } else {
                //Bundle args = new Bundle();
                //args.putBoolean("refresh", true);
                //onConfirm(args);
                hideProgress();
                onCancel();
            }
        } else if (v.equals(mDlgBtnNeg)) {
            hideProgress();
            onCancel();
        } else if (v.equals(mDlgBtnNeu)) {
            Bundle args = new Bundle();
            args.putBoolean("refresh", true);
            onConfirm(args);
        }
    }

    public void showProgress() {
        mProgressView.setVisibility(View.VISIBLE);
    }

    public void hideProgress() {
        mProgressView.setVisibility(View.INVISIBLE);
    }

    public void dismiss() {
        mDialog.dismiss();
    }

}
