package com.transcend.nas.management.firmware;

import android.util.Log;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerInfo;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASPref;
import com.transcend.nas.NASUtils;
import com.transcend.nas.common.HttpRequestFactory;
import com.tutk.IOTC.P2PService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwonkyManager {
    private static final String TAG = TwonkyManager.class.getSimpleName();
    private static final Object mMute = new Object();
    private static TwonkyManager mTwonkyManager;
    private int defaultLifeCycle = 10;
    private int mLifeCycle = 0;
    private STATUS useTwonkyServer = STATUS.UNKNOW;

    public enum STATUS {
        UNKNOW, DISABLE, ENABLE
    }

    public TwonkyManager() {
        initTwonky();
    }

    public static TwonkyManager getInstance() {
        synchronized (mMute) {
            if (mTwonkyManager == null)
                mTwonkyManager = new TwonkyManager();
        }
        return mTwonkyManager;
    }

    public void initTwonky() {
        if (NASPref.useTwonkyServer) {
            String firmware = NASPref.defaultFirmwareVersion;
            Server server = ServerManager.INSTANCE.getCurrentServer();
            ServerInfo info = server.getServerInfo();
            if (info != null)
                firmware = info.firmwareVer;

            if (firmware != null && !firmware.equals("")) {
                int version = Integer.parseInt(firmware);
                if (version >= NASPref.useTwonkyMinFirmwareVersion)
                    useTwonkyServer = STATUS.ENABLE;
                else
                    useTwonkyServer = STATUS.DISABLE;
            } else {
                useTwonkyServer = STATUS.UNKNOW;
            }
            Log.d(TAG, "Firmware version : " + firmware + ", Use Twonky Thumbnail : " + useTwonkyServer);
        } else {
            useTwonkyServer = STATUS.DISABLE;
        }
    }

    public void cleanTwonky() {

    }

    public boolean checkLifeCycle() {
        mLifeCycle--;
        if (mLifeCycle > 0)
            return true;
        else {
            mLifeCycle = defaultLifeCycle;
            return false;
        }
    }

    public void updateTwonky() {
        if (useTwonkyServer != STATUS.ENABLE)
            return;

        Server server = ServerManager.INSTANCE.getCurrentServer();
        String twonkyServerIP = getTwonkyIP(server);

        List<String> smbSharedList = new ArrayList<>();
        //add current user's home folder
        smbSharedList.add("/home/" + server.getUsername() + "/");

        //add other smb shared folder
        List<String> realPathList = ShareFolderManager.getInstance().getAllValue();
        for (String realPath : realPathList)
            smbSharedList.add(realPath);

        TwonkyHelper helper = new TwonkyHelper();
        helper.addTwonkySharedFolder(twonkyServerIP, smbSharedList);
        //helper.doTwonkyRescan(twonkyServerIP);
    }

    public String getPhotoUrl(Server server, boolean forceLocal, boolean thumbnail, String path) {
        if (useTwonkyServer != STATUS.ENABLE || server == null)
            return null;

        if(isInValid(path))
            return null;

        path = NASUtils.encodeString(path);
        String ip = forceLocal ? getTwonkyLocalIP(server) : getTwonkyIP(server);

        String value = "http://" + ip + "/rpc/get_thumbnail?path=";
        if (thumbnail)
            value += path + "&scale=256x256";
        else
            value += path + "&scale=orig";
        return value;
    }

    public String getFileUrl(Server server, String path) {
        if (useTwonkyServer != STATUS.ENABLE || server == null)
            return null;

        String serverUDN = getServerUDN(server);
        if(serverUDN == null || "".equals(serverUDN))
            return null;

        String fileID = getFileID(server, path);
        if(fileID == null || "".equals(fileID))
            return null;

        String value = "http://" + getTwonkyIP(server) + "/nmc/rss/server/RBuuid%3A" + serverUDN + "/IBuuid%3A" + serverUDN
                + "," + fileID;
        ArrayList<String> keywords = new ArrayList<>();
        keywords.add("res");
        HashMap<String, String> results = HttpRequestFactory.doXmlGetRequest(value, keywords);
        if(results != null)
            return results.get("res");

        return null;
    }

    private String getTwonkyIP(Server server) {
        return P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.TWONKY);
    }

    private String getTwonkyLocalIP(Server server) {
        String hostname = "";
        ServerInfo info = server.getServerInfo();
        if (info != null)
            hostname = info.ipAddress + ":9000";

        if (hostname == null || "".equals(hostname))
            hostname = getTwonkyIP(server);

        return hostname;
    }

    private String getServerUDN(Server server){
        String value = "http://" + getTwonkyIP(server) + "/rpc/info_status";
        String result = HttpRequestFactory.doGetRequest(value, false);
        HashMap<String, String> serverInfo = new HashMap<>();
        String[] lines = result.split("\n");
        for(String line : lines) {
            //each line structure : key|content
            String[] info = line.split("\\|");
            if(info != null && info.length >= 2) {
                String key = info[0];
                String content = info[1];
                serverInfo.put(key, content);
            }
        }

        String serverUDN = serverInfo.get("server_udn").replace("uuid:", "");
        return serverUDN;
    }

    private String getFileID(Server server, String path) {
        String value = "http://" + getTwonkyIP(server) + "/rpc/share_local_file?" + NASUtils.encodeString(path);
        String result = HttpRequestFactory.doGetRequest(value, false);
        return result;
    }

    private boolean isInValid(String path){
        Pattern special = Pattern.compile ("[+%&]");
        Matcher hasSpecial = special.matcher(path);
        return hasSpecial.find();
    }
}
