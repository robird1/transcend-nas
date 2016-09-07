package com.transcend.nas.connection_new;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.transcend.nas.R;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ikelee on 16/8/31.
 */
public abstract class ListDialog implements View.OnClickListener {

    private static final String TAG = ListDialog.class.getSimpleName();

    public abstract void onConfirm(Bundle args);

    public abstract void onCancel();

    private AppCompatActivity mActivity;
    private AlertDialog mDialog;
    private Button mDlgBtnPos;
    private Button mDlgBtnNeg;
    private Button mDlgBtnNeu;
    private RelativeLayout mProgressView;
    private ListView mListView;
    private RelativeLayout mEmptyView;
    private int mSelect = -1;
    private ArrayList<HashMap<String, String>> mList;
    private boolean isEmpty = false;


    public ListDialog(Context context, ArrayList<HashMap<String, String>> list) {
        mActivity = (AppCompatActivity) context;
        mList = list;
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
        builder.setPositiveButton(R.string.wizard_try, null);
        builder.setNegativeButton(R.string.cancel, null);
        //builder.setNeutralButton(R.string.wizard_try, null);
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

    private void initListView() {
        if(mListView == null) {
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

            mListView.setAdapter(new SimpleAdapter(mActivity,
                            myListData,
                            android.R.layout.simple_list_item_2,
                            new String[]{ID_TITLE, ID_SUBTITLE},
                            new int[]{android.R.id.text1, android.R.id.text2})
            );
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    mSelect = position;
                    onClick(mDlgBtnPos);
                }
            });
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
        if(list == null) {
            mDialog.setTitle(mActivity.getString(R.string.find_nas));
            showProgress();
        } else {
            mList = list;
            isEmpty = mList.size() == 0;
            mDialog.setTitle(isEmpty ? mActivity.getString(R.string.wizard_setup_found_error) : "Add new StoreJet Cloud");
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
                Bundle args = new Bundle();
                args.putBoolean("refresh", true);
                onConfirm(args);
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
