package com.transcend.nas.management;

import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import com.transcend.nas.R;

/**
 * Created by silverhsu on 16/1/22.
 */
public class FileManageActionModeCallback implements ActionMode.Callback {

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.file_manage_viewer, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {

    }
}
