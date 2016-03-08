package com.transcend.nas;

import android.content.Context;
import android.os.Environment;
import android.os.storage.StorageManager;

import com.transcend.nas.utils.PrefUtil;

/**
 * Created by silverhsu on 16/1/15.
 */
public class NASPref {

    private static final String TAG = NASPref.class.getSimpleName();

    public enum Sort {
        TYPE,
        DATE,
        NAME
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

    public static String getUsername(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_username);
        String def = "";
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

    /**
     *
     * Sign In
     *
     */
    public static String getCloudUsername(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_cloud_username);
        String def = "";
        return PrefUtil.read(context, name, key, def);
    }

    public static void setCloudPassword(Context context, String username) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_cloud_password);
        PrefUtil.write(context, name, key, username);
    }

    /**
     *
     * Backup scenario
     *
     */
    public static String getBackupScenario(Context context) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_backup_scenario);
        String def = context.getResources().getStringArray(R.array.backup_scenario_values)[0];
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
        String def = "/homes/Backups/";
        return PrefUtil.read(context, name, key, def);
    }

    public static void setBackupLocation(Context context, String path) {
        String name = context.getResources().getString(R.string.pref_name);
        String key = context.getResources().getString(R.string.pref_backup_location);
        PrefUtil.write(context, name, key, path);
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

    private static String getDefaultDownloadLocation(Context context) {
        StringBuffer buf = new StringBuffer();
        buf.append(Environment.getExternalStorageDirectory().getAbsolutePath());
        buf.append("/");
        buf.append(context.getResources().getString(R.string.app_name));
        buf.append("/");
        buf.append(context.getResources().getString(R.string.downloads_name));
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

}
