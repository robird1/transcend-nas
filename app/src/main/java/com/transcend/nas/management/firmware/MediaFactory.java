package com.transcend.nas.management.firmware;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASApp;
import com.transcend.nas.utils.MimeUtil;
import com.tutk.IOTC.P2PService;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by silverhsu on 16/1/25.
 */
public class MediaFactory {
    public static void open(Activity act, String path) {
        Uri uri = createUri(path);
        String name = parseName(path);
        String type = MimeUtil.getMimeType(path);
        openIn(act, uri, type, name);
    }

    public static Uri createUri(String path) {
        Uri uri;
        if (path.startsWith(NASApp.ROOT_STG)) {
            uri = Uri.fromFile(new File(path));
        } else {
            Server server = ServerManager.INSTANCE.getCurrentServer();
            String hostname = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
            String username = server.getUsername();
            String hash = server.getHash();
            String url;
            String filepath;
            if (path.startsWith(Server.HOME))
                filepath = Server.USER_DAV_HOME + path.replaceFirst(Server.HOME, "/");
            else if (path.startsWith("/" + username + "/"))
                filepath = Server.USER_DAV_HOME + path.replaceFirst("/" + username + "/", "/");
            else {
                if (username.equals("admin")) {
                    path = ShareFolderManager.getInstance().getRealPath(path);
                    filepath = Server.DEVICE_DAV_HOME + path.replaceFirst("/home/", "/");
                } else {
                    String newPath = "";
                    String[] paths = path.replaceFirst("/", "").split("/");
                    int length = paths.length;
                    for (int i = 0; i < length; i++) {
                        if (i == 0)
                            newPath = "/" + paths[i].toLowerCase();
                        else
                            newPath = newPath + "/" + paths[i];
                    }
                    filepath = "/dav" + newPath;
                }
            }

            url = "http://" + hostname + filepath + "?session=" + hash;
            url = url.replaceAll(" ", "%20");
            uri = Uri.parse(url);
        }
        return uri;
    }

    public static MediaInfo createMediaInfo(int mediaType, String path) {
        String uri = createUri(path).toString();
        String type = MimeUtil.getMimeType(path);
        MediaMetadata metadata = new MediaMetadata(mediaType);
        //metadata.addImage(new WebImage(Uri.parse(FileFactory.getInstance().getPhotoPath(true, path))));

        MediaInfo info = new MediaInfo.Builder(uri)
                .setContentType(type)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(metadata)
                .build();
        return info;
    }

    private static void openIn(Activity act, Uri uri, String type, String title) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, type);
        intent.putExtra("title", title);
        try {
            act.startActivityForResult(intent, 0);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(act, "No suitable app", Toast.LENGTH_SHORT).show();
        }
    }

    public static void open(Activity act, Bundle args) {
        String url = args.getString("path");
        String[] paths = url.split("/");
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String hostname = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
        if (paths.length > 0) {
            url = "http://" + hostname + "/hls/" + paths[paths.length - 1];
        }
        Uri uri = Uri.parse(url);
        String type = args.getString("type");
        String name = args.getString("name");
        openIn(act, uri, type, name);
    }

    public static String createTranslatePath(String path) {
        String url = path;
        if (!path.startsWith(NASApp.ROOT_STG)) {
            // remote
            Server server = ServerManager.INSTANCE.getCurrentServer();
            String hostname = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
            String hash = server.getHash();
            String folder = parseFolder(path);
            String file = parseFile(path);
            String redirect = "1";
            url = "http://" + hostname + "/streaming.cgi?folder=" + folder + "&file=" + file + "&id=" + hash + "&redirect=" + redirect;
        }

        return url;
    }

    private static String parseFolder(String path) {
        if (path.startsWith("/"))
            path = path.replaceFirst("/", "");
        String[] paths = path.split("/");
        String folder = paths.length >= 1 ? paths[0] : "";
        return folder;
    }

    private static String parseFile(String path) {
        String file = "/";
        if (path.startsWith("/"))
            path = path.replaceFirst("/", "");
        String[] paths = path.split("/");
        int length = paths.length;
        for (int i = 1; i < length; i++) {
            if (i == length - 1)
                file += paths[i];
            else
                file += paths[i] + "/";
        }

        try {
            return URLEncoder.encode(file, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return file;
    }

    private static String parseName(String path) {
        String[] paths = path.split("/");
        String name = paths.length >= 1 ? paths[paths.length - 1] : "";
        return name;
    }
}
