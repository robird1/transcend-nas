package com.transcend.nas.management.firmware;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerInfo;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASUtils;
import com.transcend.nas.management.externalstorage.ExternalStorageController;
import com.transcend.nas.utils.MimeUtil;
import com.tutk.IOTC.P2PService;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

/**
 * Created by silverhsu on 16/1/25.
 */
public class MediaFactory {
    private static final String TAG = MediaFactory.class.getSimpleName();
    private static final List<String> CHROMECAST_MEDIA_SUPPORT_FORMAT = Arrays.asList("aac", "mp3", "mp4", "wav", "webm");

    public static void open(Activity act, String path) {
        Uri uri = createUri(act, path);
        String name = parseName(path);
        String type = MimeUtil.getMimeType(path);
        openIn(act, uri, type, name);
    }

    public static Uri createUri(Context context, String path) {
        return createUri(context, false, path);
    }

    public static Uri createUri(Context context, boolean forceLocal, String path) {
        Uri uri;
        if (path.startsWith(NASApp.ROOT_STG)) {
            uri = Uri.fromFile(new File(path));
        } else if (NASUtils.isSDCardPath(context, path)) {
            uri = new ExternalStorageController(context).getSDFileUri(path);
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

            String[] paths = filepath.replaceFirst("/", "").split("/");
            int length = paths.length;
            try {
                String newPath = "";
                for (int i = 0; i < length; i++) {
                    if(paths[i].contains(" ")) {
                        String[] tmp = paths[i].split(" ");
                        String result = "";
                        for(int j = 0 ; j < tmp.length ; j++) {
                            if(j == 0)
                                result = URLEncoder.encode(tmp[j], "UTF-8");
                            else
                                result += " " + URLEncoder.encode(tmp[j], "UTF-8");
                        }
                        newPath = newPath + "/" + result;
                    } else {
                        newPath = newPath + "/" + URLEncoder.encode(paths[i], "UTF-8");
                    }
                }
                filepath = newPath;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            if (forceLocal) {
                ServerInfo info = server.getServerInfo();
                if (info != null)
                    hostname = info.ipAddress;
            }

            url = "http://" + hostname + filepath + "?session=" + hash;
            url = url.replaceAll(" ", "%20");
            uri = Uri.parse(url);
        }
        return uri;
    }

    public static MediaInfo createMediaInfo(Context context, int mediaType, String path) {
        MediaInfo info = null;
        String ext = FilenameUtils.getExtension(path);
        if (ext != null && CHROMECAST_MEDIA_SUPPORT_FORMAT.contains(ext.toLowerCase())) {
            String uri = createUri(context, true, path).toString();
            String type = MimeUtil.getMimeType(path);
            MediaMetadata metadata = new MediaMetadata(mediaType);
            metadata.putString(MediaMetadata.KEY_TITLE, FilenameUtils.getName(path));
            metadata.addImage(new WebImage(Uri.parse(FileFactory.getInstance().getPhotoPath(context, true, path))));

            info = new MediaInfo.Builder(uri)
                    .setContentType(type)
                    .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                    .setMetadata(metadata)
                    .build();
        }

        return info;
    }

    private static void openIn(Activity act, Uri uri, String type, String title) {
        Intent intent = NASUtils.getIntentUri(act, uri);
        intent.putExtra("title", title);
        try {
            act.startActivityForResult(intent, 0);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(act, "No suitable app", Toast.LENGTH_SHORT).show();
        }
    }

    private static String parseName(String path) {
        String[] paths = path.split("/");
        String name = paths.length >= 1 ? paths[paths.length - 1] : "";
        return name;
    }
}
