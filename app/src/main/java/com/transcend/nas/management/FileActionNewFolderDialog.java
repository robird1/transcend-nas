package com.transcend.nas.management;

import android.content.Context;
import android.content.DialogInterface;
import android.net.LinkAddress;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.transcend.nas.R;

import org.apache.commons.io.FilenameUtils;

import java.util.List;

/**
 * Created by silverhsu on 16/1/21.
 */
public abstract class FileActionNewFolderDialog implements TextWatcher, View.OnClickListener {

    public abstract void onConfirm(String newName);

    private Context mContext;
    private AlertDialog mDialog;
    private Button mDlgBtnPos;
    private TextInputLayout mFieldName;

    private List<String> mFolderNames;

    public FileActionNewFolderDialog(Context context, List<String> folderNames) {
        mContext = context;
        mFolderNames = folderNames;
        initDialog();
        initFieldName();
    }

    private void initDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getResources().getString(R.string.msg_new_folder));
        builder.setIcon(R.drawable.ic_toolbar_newfolder_gray);
        builder.setView(R.layout.dialog_folder_create);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.confirm, null);
        builder.setCancelable(true);
        mDialog = builder.show();
        mDlgBtnPos = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        mDlgBtnPos.setOnClickListener(this);
    }

    private void initFieldName() {
        mFieldName = (TextInputLayout) mDialog.findViewById(R.id.dialog_folder_create_name);
        if (mFieldName.getEditText() == null)
            return;
        mFieldName.getEditText().setText(getUniqueName());
        mFieldName.getEditText().addTextChangedListener(this);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String name = s.toString();
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
            if (mFieldName.getEditText() == null) return;
            String name = mFieldName.getEditText().getText().toString();
            FileNameChecker checker = new FileNameChecker(name);
            if (checker.isContainInvalid() || checker.isStartWithSpace()) {
                Toast.makeText(mContext, R.string.toast_invalid_name, Toast.LENGTH_SHORT).show();
            } else {
                onConfirm(name);
                mDialog.dismiss();
            }
        }
    }

    private String getUniqueName() {
        int index = 2;
        String name = mContext.getResources().getString(R.string.untitled_folder);
        String unique = name;
        while (mFolderNames.contains(unique)) {
            unique = String.format(name + "_%d", index++);
        }
        return unique;
    }

    private boolean isInvalid(String name) {
        return (name == null) || (name.isEmpty());
    }

    private boolean isDuplicated(String name) {
        if (isInvalid(name)) return false;
        return mFolderNames.contains(name.toLowerCase());
    }

}
