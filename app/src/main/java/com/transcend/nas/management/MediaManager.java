package com.transcend.nas.management;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.android.exoplayer.ExoPlayer;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.utils.MimeUtil;
import com.transcend.nas.viewer.PlayerActivity;
import com.tutk.IOTC.P2PService;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by silverhsu on 16/1/25.
 */
public class MediaManager {

    public static void open(Activity act, Bundle args) {
        String url = args.getString("path");
        String[] paths = url.split("/");
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String hostname = server.getHostname();
        String p2pIP = P2PService.getInstance().getP2PIP();
        if (P2PService.getInstance().isConnected() && hostname.contains(p2pIP)) {
            String newHostname = p2pIP + ":" + P2PService.getInstance().getP2PPort(P2PService.P2PProtocalType.HTTP);
            if(paths.length > 0){
                url = "http://" + newHostname + "/hls/" + paths[paths.length-1];
            }
        }
        Uri uri = Uri.parse(url);
        String type = args.getString("type");
        String name = args.getString("name");
        openIn(act, uri, type, name);
    }

    public static void open(Activity act, String path) {
        Uri uri = createUri(path);
        String name = parseName(path);
        String type = MimeUtil.getMimeType(path);
        openIn(act, uri, type, name);
    }

    public static MediaInfo createMediaInfo(int mediaType, String path){
        String uri = createUri(path).toString();
        String type = MimeUtil.getMimeType(path);
        MediaMetadata metadata = new MediaMetadata(mediaType);

        MediaInfo info = new MediaInfo.Builder(uri)
                .setContentType(type)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(metadata)
                .build();
        return info;
    }


    public static String createPath(String path){
        String url = path;
        if (!path.startsWith(NASApp.ROOT_STG)) {
            // remote
            Server server = ServerManager.INSTANCE.getCurrentServer();
            String hostname = server.getHostname();
            String p2pIP = P2PService.getInstance().getP2PIP();
            if (P2PService.getInstance().isConnected() && hostname.contains(p2pIP)) {
                hostname = p2pIP + ":" + P2PService.getInstance().getP2PPort(P2PService.P2PProtocalType.HTTP);
            }
            String hash = server.getHash();
            String folder = parseFolder(path);
            String file = parseFile(path);
            String redirect = "1";
            url = "http://" + hostname + "/streaming.cgi?folder=" + folder + "&file=" + file + "&id=" + hash + "&redirect=" + redirect;
        }

        return url;
    }

    public static Uri createUri(String path){
        Uri uri;
        if (path.startsWith(NASApp.ROOT_STG)) {
            // local
            uri = Uri.fromFile(new File(path));
        }
        else {
            // remote
            Server server = ServerManager.INSTANCE.getCurrentServer();
            String hostname = server.getHostname();
            String p2pIP = P2PService.getInstance().getP2PIP();
            if (P2PService.getInstance().isConnected() && hostname.contains(p2pIP)) {
                hostname = p2pIP + ":" + P2PService.getInstance().getP2PPort(P2PService.P2PProtocalType.HTTP);
            }
            String hash = server.getHash();
            String folder = parseFolder(path);
            String file = parseFile(path);
            String name = parseName(path);
            String redirect = "1";
            String url = "http://" + hostname + "/streaming.cgi?folder=" + folder + "&file=" + file + "&id=" + hash + "&redirect=" + redirect;
            uri = Uri.parse(url);
        }
        return uri;
    }

    public static String parseFolder(String path) {
        if(path.startsWith("/"))
            path = path.replaceFirst("/", "");
        String[] paths = path.split("/");
        String folder = paths.length >= 1 ? paths[0] : "";
        return folder;
    }

    public static String parseFile(String path) {
        String file = "/";
        if(path.startsWith("/"))
            path = path.replaceFirst("/", "");
        String[] paths = path.split("/");
        int length = paths.length;
        for(int i = 1; i < length ; i++){
            if(i == length - 1)
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

    public static String parseName(String path){
        String[] paths = path.split("/");
        String name = paths.length >= 1 ? paths[paths.length-1] : "";
        return name;
    }

    private static void openIn(Activity act, Uri uri, String type, String title) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, type);
        intent.putExtra("title", title);
        act.startActivityForResult(intent, 0);
    }

    private static void openLocal(Activity act, Uri uri, String type, String title){
        Intent mpdIntent = new Intent(act, PlayerActivity.class)
                .setData(uri)
                .putExtra(PlayerActivity.CONTENT_ID_EXTRA, "")
                .putExtra(PlayerActivity.CONTENT_TYPE_EXTRA, type)
                .putExtra(PlayerActivity.PROVIDER_EXTRA, title);
        act.startActivity(mpdIntent);
    }

}
