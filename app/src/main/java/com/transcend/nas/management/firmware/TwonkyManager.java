package com.transcend.nas.management.firmware;

import android.util.Log;

import com.realtek.nasfun.api.HttpClientManager;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerInfo;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASPref;
import com.transcend.nas.NASUtils;
import com.transcend.nas.common.HttpRequestFactory;
import com.tutk.IOTC.P2PService;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

        String ip = forceLocal ? getTwonkyLocalIP(server) : getTwonkyIP(server);
        String convert = NASUtils.encodeString(path);
        if (convert != null && !"".equals(convert))
            path = convert;

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
        try {
            DefaultHttpClient httpClient = HttpClientManager.getClient();
            HttpGet httpGet = new HttpGet(value);
            HttpResponse httpResponse;
            httpResponse = httpClient.execute(httpGet);
            HttpEntity httpEntity = httpResponse.getEntity();
            InputStream inputStream = httpEntity.getContent();
            String inputEncoding = EntityUtils.getContentCharSet(httpEntity);
            if (inputEncoding == null) {
                inputEncoding = HTTP.DEFAULT_CONTENT_CHARSET;
            }
            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(inputStream, inputEncoding);
                int eventType = xpp.getEventType();
                String curTagName = null;

                do {
                    String tagName = xpp.getName();
                    if (eventType == XmlPullParser.START_TAG) {
                        curTagName = tagName;
                    } else if (eventType == XmlPullParser.TEXT) {
                        if("res".equals(curTagName)) {
                            return xpp.getText();
                        }
                    }
                    eventType = xpp.next();

                } while (eventType != XmlPullParser.END_DOCUMENT);
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

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
        String value = "http://" + getTwonkyIP(server) + "/rpc/share_local_file?" + path;
        String result = HttpRequestFactory.doGetRequest(value, false);
        return result;
    }
}
