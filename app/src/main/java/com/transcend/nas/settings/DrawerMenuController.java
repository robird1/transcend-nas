package com.transcend.nas.settings;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASPref;
import com.transcend.nas.NASUtils;
import com.transcend.nas.R;

import java.io.IOException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import static com.transcend.nas.R.string.device;

/**
 * Created by steve_su on 2016/12/12.
 */

public class DrawerMenuController {
//    private static final String TAG = DrawerMenuController.class.getSimpleName();
    private static final int MESSAGE_FB_PROFILE_PHOTO = 999;
    private AppCompatActivity mActivity;
    private Toolbar mToolbar;
    private NavigationView.OnNavigationItemSelectedListener mItemClickListener;
    private DrawerLayout mDrawer;
    private ActionBarDrawerToggle mToggle;
    private NavigationView mNavView;
    private ImageView mNavHeaderIcon;
    private Bitmap mPhotoBitmap;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_FB_PROFILE_PHOTO:
                    mNavHeaderIcon.setImageBitmap(mPhotoBitmap);
                    break;
            }
        }
    };

    public enum DrawerMenu {
        DRAWER_FILE_MANAGE(R.id.drawer_layout, R.id.activity_file_manage_drawer),
        DRAWER_DEFAULT(R.id.drawer_layout, R.id.navigation_view);

        private int mDrawerLayoutId, mNavigationViewId;
        DrawerMenu(int drawerLayoutId, int navigationViewId) {
            mDrawerLayoutId = drawerLayoutId;
            mNavigationViewId = navigationViewId;
        }

        public int getDrawerLayoutId() { return mDrawerLayoutId;}
        public int getNavigationViewId() { return mNavigationViewId;}
    }

    DrawerMenuController(AppCompatActivity activity, Toolbar toolbar, NavigationView.OnNavigationItemSelectedListener listener) {
        mActivity = activity;
        mToolbar = toolbar;
        mItemClickListener = listener;
    }

    void initView(DrawerMenu enumInstance) {
        mDrawer = (DrawerLayout) mActivity.findViewById(enumInstance.getDrawerLayoutId());
        mToggle = new ActionBarDrawerToggle(
                mActivity, mDrawer, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.setDrawerListener(mToggle);
        mToggle.syncState();
//        mToggle.setToolbarNavigationClickListener(mToolbarNavigationListener);
        mNavView = (NavigationView) mActivity.findViewById(enumInstance.getNavigationViewId());
        mNavView.setNavigationItemSelectedListener(mItemClickListener);
        View navHeader = mNavView.inflateHeaderView(R.layout.activity_file_manage_drawer_header);
        mNavHeaderIcon = (ImageView) navHeader.findViewById(R.id.drawer_header_icon);
        setNavigationViewTitle(navHeader);
        setNavigationViewSubtitle(navHeader);
        setDrawerHeaderIcon();
        setRemoteDeviceName();
        setLocalDeviceName();
        mNavView.getMenu().findItem(R.id.nav_switch).setVisible(NASPref.useSwitchNas);
    }

    public void setDrawerLockMode(int lockMode) {
        mDrawer.setDrawerLockMode(lockMode);
    }

    public void openDrawer() {
        mDrawer.openDrawer(GravityCompat.START);
    }

    public void closeDrawer() {
        mDrawer.closeDrawer(GravityCompat.START);
    }

    public boolean isDrawerOpen() {
        return mDrawer.isDrawerOpen(GravityCompat.START);
    }

    public void setDrawerIndicatorEnabled(boolean isEnabled) {
        mToggle.setDrawerIndicatorEnabled(isEnabled);
    }

    public void setCheckdItem(int id) {
        mNavView.setCheckedItem(id);
    }

    public void setToolbarNavigationClickListener(View.OnClickListener l) {
        mToggle.setToolbarNavigationClickListener(l);
    }

    public NavigationView getNavigationView() {
        return mNavView;
    }

    private void setNavigationViewTitle(View navHeader) {
        TextView navHeaderTitle = (TextView) navHeader.findViewById(R.id.drawer_header_title);
        navHeaderTitle.setText(ServerManager.INSTANCE.getCurrentServer().getUsername());
    }

    private void setNavigationViewSubtitle(View navHeader) {
        TextView navHeaderSubtitle = (TextView) navHeader.findViewById(R.id.drawer_header_subtitle);
        String email = NASPref.getCloudUsername(mActivity);
        if (!email.equals(""))
            navHeaderSubtitle.setText(String.format("%s", email));
        else
            navHeaderSubtitle.setText(String.format("%s@%s", ServerManager.INSTANCE.getCurrentServer().getUsername(),
                    ServerManager.INSTANCE.getCurrentServer().getHostname()));
    }

    private void setRemoteDeviceName() {
        String device = NASPref.getDeviceName(mActivity);
        if(device != null && !"".equals(device))
            mNavView.getMenu().findItem(R.id.nav_storage).setTitle(device);
    }

    private void setLocalDeviceName() {
        mNavView.getMenu().findItem(R.id.nav_device).setTitle(NASUtils.getDeviceName());
    }

    private void setDrawerHeaderIcon() {
        if (!NASPref.getFBAccountStatus(mActivity))
            return;

        final String storedUrl = NASPref.getFBProfilePhotoUrl(mActivity);
        if (storedUrl != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL url = new URL(storedUrl);
                        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                        HttpsURLConnection.setFollowRedirects(true);
                        connection.setInstanceFollowRedirects(true);
                        mPhotoBitmap = BitmapFactory.decodeStream(connection.getInputStream());
                        if (mPhotoBitmap != null) {
                            Message msg = new Message();
                            msg.what = MESSAGE_FB_PROFILE_PHOTO;
                            mHandler.sendMessage(msg);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

}
