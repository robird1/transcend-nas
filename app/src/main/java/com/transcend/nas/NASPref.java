package com.transcend.nas;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.Log;

import com.transcend.nas.management.FileManageRecyclerAdapter;
import com.transcend.nas.utils.PrefUtil;
import com.transcend.nas.viewer.music.MusicActivity;

/**
 * Created by silverhsu on 16/1/15.
 */
public class NASPref {

    private static final String TAG = NASPref.class.getSimpleName();
    public static final boolean useDefaultDownloadFolder = true;
    public static final boolean useNewLoginFlow = true;
    public static final String defaultUserName = "admin";

    public enum Sort {
        TYPE,
        DATE,
        REVERSEDATE,
        NAME,
        REVERSENAME
    }

    public enum Status {
        Inactive,
        Padding,
        Active,
        Bind
    }

    /**
     *
     * Init
     *
     */
    public static boolean getIntroduce(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_introduce);
        boolean def = false;
        return PrefUtil.read(context, name, key, def);
    }

    public static void setIntroduce(Context context, boolean init) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_introduce);
        PrefUtil.write(context, name, key, init);
    }

    public static boolean getInitial(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_initial);
        boolean def = false;
        return PrefUtil.read(context, name, key, def);
    }

    public static void setInitial(Context context, boolean init) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_initial);
        PrefUtil.write(context, name, key, init);
    }

    /**
     *
     * Login
     *
     */
    public static String getHostname(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_hostname);
        String def = "";
        return PrefUtil.read(context, name, key, def);
    }

    public static void setHostname(Context context, String hostname) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_hostname);
        PrefUtil.write(context, name, key, hostname);
    }

    public static String getLocalHostname(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_local_hostname);
        String def = "";
        return PrefUtil.read(context, name, key, def);
    }

    public static void setLocalHostname(Context context, String localHostname) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_local_hostname);
        PrefUtil.write(context, name, key, localHostname);
    }

    public static String getUsername(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_username);
        String def = "admin";
        return PrefUtil.read(context, name, key, def);
    }

    public static void setUsername(Context context, String username) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_username);
        PrefUtil.write(context, name, key, username);
    }

    public static String getPassword(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_password);
        String def = "";
        return PrefUtil.read(context, name, key, def);
    }

    public static void setPassword(Context context, String username) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_password);
        PrefUtil.write(context, name, key, username);
    }

    public static String getUUID(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_uuid);
        String def = "";
        return PrefUtil.read(context, name, key, def);
    }

    public static void setUUID(Context context, String uuid) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_uuid);
        PrefUtil.write(context, name, key, uuid);
    }

    public static String getSerialNum(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_serial_num);
        String def = "";
        return PrefUtil.read(context, name, key, def);
    }

    public static void setSerialNum(Context context, String serialNum) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_serial_num);
        PrefUtil.write(context, name, key, serialNum);
    }

    public static String getMacAddress(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_mac);
        String def = "";
        return PrefUtil.read(context, name, key, def);
    }

    public static void setMacAddress(Context context, String uuid) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_mac);
        PrefUtil.write(context, name, key, uuid);
    }

    public static String getSessionVerifiedTime(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_hash_verified_time);
        String def = "0";
        return PrefUtil.read(context, name, key, def);
    }

    public static void setSessionVerifiedTime(Context context, String time) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_hash_verified_time);
        PrefUtil.write(context, name, key, time);
    }

    /**
     *
     * Sign In
     *
     */
    public static int getCloudAccountStatus(Context context){
        String name = context.getString(R.string.pref_name);
        String key = context.getString(R.string.pref_cloud_account_status);
        int def = Status.Inactive.ordinal();
        return PrefUtil.read(context, name, key, def);
    }

    public static void setCloudAccountStatus(Context context, int type){
        String name = context.getString(R.string.pref_name);
        String key = context.getString(R.string.pref_cloud_account_status);
        PrefUtil.write(context, name, key, type);
    }


    public static String getCloudUsername(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_cloud_username);
        String def = "";
        return PrefUtil.read(context, name, key, def);
    }

    public static void setCloudUsername(Context context, String username) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_cloud_username);
        String def = "";
        PrefUtil.write(context, name, key, username);
    }

    public static String getCloudPassword(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_cloud_password);
        String def = "";
        return PrefUtil.read(context, name, key, def);
    }

    public static void setCloudPassword(Context context, String password) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_cloud_password);
        PrefUtil.write(context, name, key, password);
    }

    public static String getCloudAuthToken(Context context){
        String name = context.getString(R.string.pref_name);
        String key = context.getString(R.string.pref_cloud_token);
        String def = "";
        return PrefUtil.read(context, name, key, def);
    }

    public static void setCloudAuthToken(Context context, String token){
        String name = context.getString(R.string.pref_name);
        String key = context.getString(R.string.pref_cloud_token);
        PrefUtil.write(context, name, key, token);
    }

    public static String getCloudUUID(Context context){
        String name = context.getString(R.string.pref_name);
        String key = context.getString(R.string.pref_cloud_uuid);
        String def = "";
        return PrefUtil.read(context, name, key, def);
    }

    public static void setCloudUUID(Context context, String UUID){
        String name = context.getString(R.string.pref_name);
        String key = context.getString(R.string.pref_cloud_uuid);
        PrefUtil.write(context, name, key, UUID);
    }

    public static String getCloudServer(Context context){
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_cloud_ip);
        String def = "https://www.storejetcloud.com/1";
        return PrefUtil.read(context, name, key, def);
    }

    /**
     *
     * Backup setting
     *
     */
    public static void setBackupSetting(Context context, boolean backup) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_auto_backup);
        PrefUtil.write(context, name, key, backup);
    }

    public static Boolean getBackupSetting(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_auto_backup);
        boolean def = false;
        return PrefUtil.read(context, name, key, def);
    }

    /**
     *
     * Backup video
     *
     */
    public static void setBackupVideo(Context context, boolean backup) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_backup_video);
        PrefUtil.write(context, name, key, backup);
    }

    public static Boolean getBackupVideo(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_backup_video);
        boolean def = false;
        return PrefUtil.read(context, name, key, def);
    }

    /**
     *
     * Backup scenario
     *
     */
    public static void setBackupScenario(Context context, String scenario){
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_backup_scenario);
        PrefUtil.write(context, name, key, scenario);
    }

    public static String getBackupScenario(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_backup_scenario);
        String def = context.getResources().getStringArray(R.array.backup_scenario_values)[1];
        return PrefUtil.read(context, name, key, def);
    }

    /**
     *
     * Backup location
     *
     */
    public static String getBackupLocation(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_backup_location);
        String def = "/homes/" + Build.MODEL + "/";
        return PrefUtil.read(context, name, key, def);
    }

    public static void setBackupLocation(Context context, String path) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_backup_location);
        PrefUtil.write(context, name, key, path);
    }

    /**
     *
     * Backup source
     *
     */
    public static String getBackupSource(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_backup_source);
        String def = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
        return PrefUtil.read(context, name, key, def);
    }

    public static void setBackupSource(Context context, String path) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_backup_source);
        PrefUtil.write(context, name, key, path);
    }

    /**
     *
     * Backup Error Task
     *
     */
    public static void setBackupErrorTask(Context context, String errorTask) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_backup_error_task);
        PrefUtil.write(context, name, key, errorTask);
    }

    public static String getBackupErrorTask(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_backup_error_task);
        String def = "";
        return PrefUtil.read(context, name, key, def);
    }

    /**
     *
     * Download location
     *
     */
    public static String getDownloadLocation(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_download_location);
        String def = getDefaultDownloadLocation(context);
        return PrefUtil.read(context, name, key, def);
    }

    public static void setDownloadLocation(Context context, String path) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_download_location);
        PrefUtil.write(context, name, key, path);
    }

    public static String getShareLocation(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_share_location);
        String def = getDefaultShareLocation(context);
        return PrefUtil.read(context, name, key, def);
    }

    private static String getDefaultDownloadLocation(Context context) {
        StringBuffer buf = new StringBuffer();
        buf.append(Environment.getExternalStorageDirectory().getAbsolutePath());
        buf.append("/");
        buf.append(context.getResources().getString(R.string.app_name));
        buf.append("/");
        buf.append(context.getResources().getString(R.string.downloads_name));
        return buf.toString();
    }

    private static String getDefaultShareLocation(Context context) {
        StringBuffer buf = new StringBuffer();
        buf.append(Environment.getExternalStorageDirectory().getAbsolutePath());
        buf.append("/");
        buf.append(context.getResources().getString(R.string.app_name));
        buf.append("/");
        buf.append(context.getResources().getString(R.string.shares_name));
        return buf.toString();
    }

    /**
     *
     * Cache Size
     *
     */
    public static String getCacheSize(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_cache_size);
        String def = context.getResources().getStringArray(R.array.cache_size_entries)[0];
        return PrefUtil.read(context, name, key, def);
    }

    /**
     *
     * File Sort Type
     *
     */
    public static Sort getFileSortType(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_file_sort_type);
        int def = Sort.TYPE.ordinal();
        return Sort.values()[PrefUtil.read(context, name, key, def)];
    }

    public static void setFileSortType(Context context, Sort sort) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_file_sort_type);
        PrefUtil.write(context, name, key, sort.ordinal());
    }

    /**
     *
     * File View
     *
     */
    public static FileManageRecyclerAdapter.LayoutType getFileViewType(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_file_view_type);
        int def = FileManageRecyclerAdapter.LayoutType.LIST.ordinal();
        return FileManageRecyclerAdapter.LayoutType.values()[PrefUtil.read(context, name, key, def)];
    }

    public static void setFileViewType(Context context, FileManageRecyclerAdapter.LayoutType type) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_file_view_type);
        PrefUtil.write(context, name, key, type.ordinal());
    }

    /**
     *
     * Picker File View
     *
     */
    public static FileManageRecyclerAdapter.LayoutType getFilePickerViewType(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_file_picker_view_type);
        int def = FileManageRecyclerAdapter.LayoutType.LIST.ordinal();
        return FileManageRecyclerAdapter.LayoutType.values()[PrefUtil.read(context, name, key, def)];
    }

    public static void setFilePickerViewType(Context context, FileManageRecyclerAdapter.LayoutType type) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_file_picker_view_type);
        PrefUtil.write(context, name, key, type.ordinal());
    }

    /**
     *
     * Music Type
     *
     */
    public static MusicActivity.MUSIC_MODE getMusicType(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_music_type);
        int def = MusicActivity.MUSIC_MODE.NORMAL.ordinal();
        return MusicActivity.MUSIC_MODE.values()[PrefUtil.read(context, name, key, def)];
    }

    public static void setMusicType(Context context, MusicActivity.MUSIC_MODE mode) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_music_type);
        PrefUtil.write(context, name, key, mode.ordinal());
    }

}
