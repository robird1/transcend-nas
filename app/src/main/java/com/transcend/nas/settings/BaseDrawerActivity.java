package com.transcend.nas.settings;

import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import com.transcend.nas.LoaderID;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.NASUtils;
import com.transcend.nas.R;
import com.transcend.nas.common.ManageFactory;
import com.transcend.nas.connection.LoginActivity;
import com.transcend.nas.connection.old.StartActivity;
import com.transcend.nas.management.FileManageActivity;
import com.transcend.nas.management.firmware.ShareFolderManager;
import com.transcend.nas.management.firmware.TwonkyManager;
import com.transcend.nas.service.AutoBackupService;
import com.transcend.nas.service.LanCheckManager;
import com.transcend.nas.tutk.TutkLogoutLoader;
import com.transcend.nas.view.NotificationDialog;
import com.transcend.nas.viewer.music.MusicService;

/**
 * Created by steve_su on 2016/12/12.
 */

public abstract class BaseDrawerActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        LoaderManager.LoaderCallbacks<Boolean> {

    private DrawerMenuController mDrawerController;

    public abstract int onLayoutID();

    public abstract int onToolbarID();

    public abstract DrawerMenuController.DrawerMenu onActivityDrawer();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(onLayoutID());
        initToolbar();
        initDrawer();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        toggleDrawerCheckedItem(intent);
    }

    @Override
    public void onBackPressed() {
        toggleDrawerCheckedItem();
        if (mDrawerController.isDrawerOpen()) {
            mDrawerController.closeDrawer();
        } else {
            mDrawerController.openDrawer();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case R.id.nav_storage:
            case R.id.nav_device:
            case R.id.nav_downloads:
                mDrawerController.closeDrawer();
                startFileManageActivity(id);
                break;
            case R.id.nav_settings:
                mDrawerController.closeDrawer();
                startActivity(SettingsActivity.class, id);
                break;
            case R.id.nav_help:
                mDrawerController.closeDrawer();
                startActivity(HelpActivity.class, id);
                break;
            case R.id.nav_feedback:
                mDrawerController.closeDrawer();
                startActivity(FeedbackActivity.class, id);
                break;
            case R.id.nav_logout:
                showLogoutDialog();
                break;
        }

        return true;
    }

    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        Loader loader = null;
        switch (id) {
            case LoaderID.TUTK_LOGOUT:
                loader = new TutkLogoutLoader(this);
                break;
        }
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean isSuccess) {
        if (isSuccess) {
            if (loader instanceof TutkLogoutLoader) {
                startSignInActivity();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {

    }

    protected void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(onToolbarID());
        toolbar.setTitle("");
        //toolbar.setNavigationIcon(R.drawable.ic_navigation_arrow_white_24dp);
        setSupportActionBar(toolbar);
    }

    protected void initDrawer() {
        Toolbar toolbar = (Toolbar) findViewById(onToolbarID());
        mDrawerController = new DrawerMenuController(this, toolbar, this);
        mDrawerController.initView(onActivityDrawer());
    }

    protected DrawerMenuController getDrawerController() {
        return mDrawerController;
    }

    protected void toggleDrawerCheckedItem() {
        toggleDrawerCheckedItem(getIntent());
    }

    protected void toggleDrawerCheckedItem(@NonNull Intent intent) {
        mDrawerController.setCheckdItem(intent.getIntExtra("lastSelectedItem", R.id.nav_storage));
    }

    private void startActivity(final Class invokedClass, final int itemId) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(BaseDrawerActivity.this, invokedClass);
                i.putExtra("lastSelectedItem", itemId);
                startActivity(i);
            }
        }, 280);
    }

    private void startFileManageActivity(final int itemId) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(BaseDrawerActivity.this, FileManageActivity.class);
                intent.putExtra("selectedItemId", itemId);
                intent.putExtra("path", getFileManagePath(itemId));
                startActivity(intent);
            }
        }, 280);
    }

    private String getFileManagePath(int itemId) {
        switch (itemId) {
            case R.id.nav_storage:
                return NASApp.ROOT_SMB;
            case R.id.nav_device:
                return NASApp.ROOT_STG;
            case R.id.nav_downloads:
                return NASPref.getDownloadLocation(this);
            default:
                return NASApp.ROOT_SMB;
        }
    }

    private void showLogoutDialog() {
        Bundle value = new Bundle();
        value.putString(NotificationDialog.DIALOG_MESSAGE, getString(R.string.remote_access_logout));
        new NotificationDialog(this, value) {
            @Override
            public void onConfirm() {
                Bundle args = new Bundle();
                args.putBoolean("clean", true);
                getLoaderManager().restartLoader(LoaderID.TUTK_LOGOUT, args, BaseDrawerActivity.this).forceLoad();
            }

            @Override
            public void onCancel() {

            }
        };
    }

    protected void clearDataAfterSwitch() {
        NASPref.clearDataAfterSwitch(this);

        boolean isRunning = false;

        //stop auto backup service
        isRunning = ManageFactory.isServiceRunning(this, AutoBackupService.class);
        if (isRunning) {
            Intent intent = new Intent(this, AutoBackupService.class);
            stopService(intent);
        }

        //stop music service
        isRunning = ManageFactory.isServiceRunning(this, MusicService.class);
        if (isRunning) {
            Intent intent = new Intent(this, MusicService.class);
            stopService(intent);
        }

        //clean disk info
        DiskFactory.getInstance().cleanDiskDevices();

        //clean path map
        ShareFolderManager.getInstance().cleanRealPathMap();

        //clean twonky map
        TwonkyManager.getInstance().cleanTwonky();

        //clean lan check
        LanCheckManager.getInstance().setLanConnect(false, "");
        LanCheckManager.getInstance().setInit(false);
    }

    protected void clearDataAfterLogout() {
        clearDataAfterSwitch();

        //clean email and account information
        if (NASPref.useFacebookLogin && NASPref.getFBAccountStatus(this))
            NASUtils.logOutFB(this);
        NASUtils.clearDataAfterLogout(this);
    }

    protected void startSignInActivity() {
        clearDataAfterLogout();

        //show SignIn activity
        Intent intent = new Intent();
        if (NASPref.useNewLoginFlow)
            intent.setClass(this, LoginActivity.class);
        else
            intent.setClass(this, StartActivity.class);
        startActivity(intent);
        finish();
    }

}
