package com.transcend.nas.management.firmware;

import android.net.Uri;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerInfo;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASPref;
import com.transcend.nas.NASUtils;
import com.tutk.IOTC.P2PService;

/**
 * Created by ikelee on 17/5/8.
 */
public class WebDavFactory {
    public static Uri createUri(String path) {
        return createUri(false, path);
    }

    public static Uri createUri(boolean forceLocal, String path) {
        Uri uri;
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String hostname = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
        if (forceLocal) {
            ServerInfo info = server.getServerInfo();
            if (info != null)
                hostname = info.ipAddress;
        }

        String username = server.getUsername();
        String filepath;
        if (path.startsWith(Server.HOME))
            filepath = Server.USER_DAV_HOME + path.replaceFirst(Server.HOME, "/");
        else if (path.startsWith("/" + username + "/"))
            filepath = Server.USER_DAV_HOME + path.replaceFirst("/" + username + "/", "/");
        else {
            if (NASPref.defaultUserName.equals(username)) {
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

        String url = "http://" + hostname + NASUtils.encodeString(filepath) + "?session=" + server.getHash();
        uri = Uri.parse(url);
        return uri;
    }
}
