package com.transcend.nas.management;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASPref;
import com.transcend.nas.utils.MimeUtil;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by silverhsu on 16/1/25.
 */
public class MediaManager {

    private final static String FOLDER_HOME   = "/homes/";
    private final static String FOLDER_PUBLIC = "/Public/";

    private final static String HOME   = "HOME";
    private final static String PUBLIC = "PUBLIC";

    public static void open(Activity act, String path) {
        if (path.startsWith(NASPref.getDownloadLocation(act))) {
            // local
            Uri uri = Uri.fromFile(new File(path));
            String type = MimeUtil.getMimeType(path);
            openIn(act, uri, type);
        }
        else {
            // remote
            Server server = ServerManager.INSTANCE.getCurrentServer();
            String hostname = server.getHostname();
            String hash = server.getHash();
            String folder = parseFolder(path);
            String file = parseFile(path);
            String redirect = "1";
            String url = "http://" + hostname + "/streaming.cgi?folder=" + folder + "&file=" + file + "&id=" + hash + "&redirect=" + redirect;
            Uri uri = Uri.parse(url);
            String type = MimeUtil.getMimeType(path);
            openIn(act, uri, type);
        }
    }

    private static String parseFolder(String path) {
        String folder = "";
        if (path.startsWith(FOLDER_HOME))
            folder = HOME;
        if (path.startsWith(FOLDER_PUBLIC))
            folder = PUBLIC;
        return folder;
    }

    private static String parseFile(String path) {
        String file = "";
        if (path.startsWith(FOLDER_HOME))
            file = path.replaceFirst(FOLDER_HOME, "/");
        if (path.startsWith(FOLDER_PUBLIC))
            file = path.replaceFirst(FOLDER_PUBLIC, "/");
        try {
            return URLEncoder.encode(file, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return file;
    }

    private static void openIn(Activity act, Uri uri, String type) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, type);
        act.startActivityForResult(intent, 0);
    }

}
