package com.transcend.nas.management;

import android.content.Context;
import android.content.DialogInterface;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.transcend.nas.R;

import org.apache.commons.io.FilenameUtils;

import java.util.List;

/**
 * Created by silverhsu on 16/1/30.
 */
public abstract class FileActionRenameDialog implements TextWatcher, View.OnClickListener {

    public abstract void onConfirm(String newName);

    private Context mContext;
    private AlertDialog mDialog;
    private Button mDlgBtnPos;
    private TextInputLayout mFieldName;

    private String mName;
    private String mType;
    private List<String> mNames;
    private boolean mIgnoreType;

    public FileActionRenameDialog(Context context,boolean ignoreType, String name, List<String> names) {
        mContext = context;
        mIgnoreType = ignoreType;
        if(!ignoreType){
            mName = FilenameUtils.getBaseName(name);
            mType = FilenameUtils.getExtension(name);
        } else {
            mName = name;
        }
        mNames = names;
        initDialog();
        initFieldName();
    }

    private void initDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getResources().getString(R.string.rename));
        builder.setIcon(R.drawable.ic_toolbar_rename_gray);
        builder.setView(R.layout.dialog_rename);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.confirm, null);
        builder.setCancelable(true);
        mDialog = builder.show();
        mDlgBtnPos = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        mDlgBtnPos.setOnClickListener(this);
    }

    private void initFieldName() {
        mFieldName = (TextInputLayout)mDialog.findViewById(R.id.dialog_rename_name);
        if (mFieldName.getEditText() == null)
            return;
        mFieldName.getEditText().setText(mName);
        mFieldName.getEditText().addTextChangedListener(this);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String name = addExtension(s.toString());
        String error = null;
        boolean enabled = true;
        if (isInvalid(name)) {
            error = mContext.getResources().getString(R.string.invalid_name);
            enabled = false;
        } else if (isDuplicated(name)) {
            error = mContext.getResources().getString(R.string.duplicate_name);
            enabled = false;
        }
        mFieldName.setError(error);
        mDlgBtnPos.setEnabled(enabled);
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public void onClick(View v) {
        if (v.equals(mDlgBtnPos)) {
            if (mFieldName.getEditText() == null)
                return;
            String text = mFieldName.getEditText().getText().toString();
            FileNameChecker checker = new FileNameChecker(text);
            if (checker.isContainInvalid() || checker.isStartWithSpace()) {
                Toast.makeText(mContext, R.string.toast_invalid_name, Toast.LENGTH_SHORT).show();
            } else {
                if(!mName.equals(text))
                    onConfirm(addExtension(text));
                mDialog.dismiss();
            }
        }
    }

    private boolean isInvalid(String name) {
        return (name == null) || (name.isEmpty());
    }

    private boolean isDuplicated(String name) {
        if (isInvalid(name)) return false;
        return mNames.contains(name.toLowerCase());
    }

    private String addExtension(String name){
        if(!mIgnoreType)
            name = name + "." + mType;
        return name;
    }
}
