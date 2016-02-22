package com.transcend.nas;

import android.content.Context;

import com.transcend.nas.utils.PrefUtil;

/**
 * Created by silverhsu on 16/1/15.
 */
public class NASPref {

    private static final String TAG = NASPref.class.getSimpleName();

    private static final String SIGN_IN_HOSTNAME = "Sign In Hostname";
    private static final String SIGN_IN_USERNAME = "Sign In Username";
    private static final String SIGN_IN_PASSWORD = "Sign In Password";

    private static final String LOGIN_HOSTNAME = "Login Hostname";
    private static final String LOGIN_USERNAME = "Login Username";
    private static final String LOGIN_PASSWORD = "Login Password";

    private static final String DOWNLOADS_PATH = "Downloads Path";

    private static final String FILE_SORT_TYPE = "File Sort Type";


    /**
     *
     * Sign In
     *
     */
    public static String getSignInHostname(Context context) {
        return PrefUtil.read(context, TAG, SIGN_IN_HOSTNAME, "");
    }

    public static void setSignInHostname(Context context, String hostname) {
        PrefUtil.write(context, TAG, SIGN_IN_HOSTNAME, hostname);
    }

    public static String getSignInUsername(Context context) {
        return PrefUtil.read(context, TAG, SIGN_IN_USERNAME, "");
    }

    public static void setSignInUsername(Context context, String username) {
        PrefUtil.write(context, TAG, SIGN_IN_USERNAME, username);
    }

    public static String getSignInPassword(Context context) {
        return PrefUtil.read(context, TAG, SIGN_IN_PASSWORD, "");
    }

    public static void setSignInPassword(Context context, String password) {
        PrefUtil.write(context, TAG, SIGN_IN_PASSWORD, password);
    }


    /**
     *
     * Login
     *
     */
    public static String getLoginHostname(Context context) {
        return PrefUtil.read(context, TAG, LOGIN_HOSTNAME, "");
    }

    public static void setLoginHostname(Context context, String hostname) {
        PrefUtil.write(context, TAG, LOGIN_HOSTNAME, hostname);
    }

    public static String getLoginUsername(Context context) {
        return PrefUtil.read(context, TAG, LOGIN_USERNAME, "");
    }

    public static void setLoginUsername(Context context, String username) {
        PrefUtil.write(context, TAG, LOGIN_USERNAME, username);
    }

    public static String getLoginPassword(Context context) {
        return PrefUtil.read(context, TAG, LOGIN_PASSWORD, "");
    }

    public static void setLoginPassword(Context context, String password) {
        PrefUtil.write(context, TAG, LOGIN_PASSWORD, password);
    }


    /**
     *
     * Downloads Path
     *
     */
    public static String getDownloadsPath(Context context) {
        return PrefUtil.read(context, TAG, DOWNLOADS_PATH, null);
    }

    public static void setDownloadsPath(Context context, String path) {
        PrefUtil.write(context, TAG, DOWNLOADS_PATH, path);
    }


    /**
     *
     * File Sort Type
     *
     */
    public enum Sort {
        TYPE,
        DATE,
        NAME
    }

    public static Sort getFileSort(Context contex) {
        int value = PrefUtil.read(contex, TAG, FILE_SORT_TYPE, Sort.TYPE.ordinal());
        return Sort.values()[value];
    }

    public static void setFileSort(Context context, Sort sort) {
        PrefUtil.write(context, TAG, FILE_SORT_TYPE, sort.ordinal());
    }

}
