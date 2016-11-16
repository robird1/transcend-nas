package com.transcend.nas.service;

import android.util.Log;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerInfo;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.common.HttpFactory;
import com.transcend.nas.firmware_api.ShareFolderManager;
import com.transcend.nas.management.FileInfo;
import com.tutk.IOTC.P2PService;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TwonkyManager {
    private static final String TAG = "TwonkyManager";
    private static final Object mMute = new Object();
    private static TwonkyManager mTwonkyManager;

    private String KEYWORD_FOLDER = "TWONKY_FOLDER";
    private HashMap<String, String> mImageMap;
    private HashMap<String, String> mFolderMap;
    private Set<String> mCacheMap;

    public TwonkyManager() {
        mImageMap = new HashMap<>();
        mFolderMap = new HashMap<>();
        mCacheMap = new HashSet<>();
    }

    public static TwonkyManager getInstance() {
        synchronized (mMute) {
            if (mTwonkyManager == null)
                mTwonkyManager = new TwonkyManager();
        }
        return mTwonkyManager;
    }

    public void cleanTwonky() {
        if (mCacheMap != null)
            mCacheMap.clear();
        if (mFolderMap != null)
            mFolderMap.clear();
        if (mImageMap != null)
            mImageMap.clear();
    }

    public boolean initTwonky() {
        String firmware = NASPref.defaultFirmwareVersion;
        Server server = ServerManager.INSTANCE.getCurrentServer();
        ServerInfo info = server.getServerInfo();
        if (info != null)
            firmware = info.firmwareVer;

        if (NASPref.useTwonkyServer && firmware != null && !firmware.equals("")) {
            int version = Integer.parseInt(firmware);
            NASPref.useTwonkyServer = version >= NASPref.useTwonkyMinFirmwareVersion;
        } else {
            NASPref.useTwonkyServer = false;
        }
        Log.d(TAG, "Firmware version : " + firmware + ", Use Twonky Thumbnail : " + NASPref.useTwonkyServer);

        return NASPref.useTwonkyServer;
    }

    public void updateTwonky(String path) {
        if (!NASPref.useTwonkyServer)
            return;

        if (path.equals(NASApp.ROOT_SMB)) {
            Server server = ServerManager.INSTANCE.getCurrentServer();
            String username = server.getUsername();
            List<String> smbSharedList = new ArrayList<>();
            //add current user's home folder
            smbSharedList.add("/home/" + username + "/");
            //add other smb shared folder
            List<String> realPathList = ShareFolderManager.getInstance().getAllValue();
            for (String realPath : realPathList)
                smbSharedList.add(realPath);

            addTwonkySharedFolder(smbSharedList);
            doTwonkyRescan(false);
        }

        //old version
        //startTwonkyParser(mPath, 0, 100);
    }

    private String getTwonkyIP() {
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String hostname = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.TWONKY);
        return hostname;
    }

    public String getUrlFromPath(boolean thumbnail, String path) {
        if (!NASPref.useTwonkyServer)
            return null;

        //old version
        //String twonkyUrl = getUrlFromMap(thumbnail, FileInfo.TYPE.PHOTO, path);
        //url = twonkyUrl + (thumbnail ? "?scale=192x192" : "");

        String value = "http://" + getTwonkyIP() + "/rpc/get_thumbnail?path=";
        path = ShareFolderManager.getInstance().getRealPath(path);
        if (thumbnail)
            value += path;
        else
            value += path + "&scale=orig";
        return value;
    }



    private boolean addTwonkySharedFolder(List<String> lists) {
        if (lists == null)
            return false;

        boolean isSuccess = false;
        List<String> addLists = new ArrayList<>();
        for (String folder : lists) {
            String convert = folder.replaceFirst("/home", "+A|");
            if (convert.endsWith("/"))
                addLists.add(convert.substring(0, convert.length() - 1));
            else
                addLists.add(convert);
        }

        //compare current twonky shared list, only add the non-add folder
        int check = addLists.size();
        String current = getTwonkySharedFolder();
        if (current != null && !current.equals("")) {
            String[] currentFolders = current.split(",");
            for (String newFolder : addLists) {
                for (String currentFolder : currentFolders) {
                    if (currentFolder != null && currentFolder.equals(newFolder)) {
                        check--;
                        break;
                    }
                }
            }

            //value "check" less than or equal 0 mean all smb shared folder already exist in twonky shared folder
            if (check > 0) {
                for (String currentFolder : currentFolders) {
                    boolean add = true;
                    int length = currentFolder.length();
                    if (length > 2) {
                        String tmp = currentFolder.substring(2, length);
                        for (String newFolder : addLists) {
                            length = newFolder.length();
                            if (length > 2) {
                                String tmp2 = newFolder.substring(2, length);
                                if (tmp2.equals(tmp)) {
                                    add = false;
                                    break;
                                }
                            }
                        }

                        if (add)
                            addLists.add(currentFolder);
                    }
                }

                if (addLists.size() > 0)
                    isSuccess = setTwonkySharedFolder(addLists);
                else
                    isSuccess = true;
            } else {
                isSuccess = true;
                Log.d(TAG, "All smb shared folder already add to twonky shared folder");
            }
        }

        Log.d(TAG, "Add twonky shared folder : " + isSuccess);
        return isSuccess;
    }

    private String getTwonkySharedFolder() {
        String value = "http://" + getTwonkyIP() + "/rpc/get_option?contentdir";
        String result = HttpFactory.doGetRequest(value);
        return result;
    }

    private boolean setTwonkySharedFolder(List<String> addLists) {
        boolean isSuccess = false;
        String value = "http://" + getTwonkyIP() + "/rpc/set_option?contentdir=";
        String folders = "";
        int length = addLists.size();

        for (int i = 0; i < length; i++) {
            String tmp = addLists.get(i);
            if (tmp.startsWith("+"))
                tmp = "%2B" + tmp.substring(1, tmp.length());
            folders = folders + tmp + (i == length - 1 ? "" : ",");
        }

        String result = HttpFactory.doGetRequest(value + folders);
        if (result != null && result.equals(folders.replace("%2B", "+")))
            isSuccess = true;

        return isSuccess;
    }

    private String doTwonkyRescan(boolean force) {
        //TODO: add lifecycle check
        if (!force && mCacheMap.contains(NASApp.ROOT_SMB))
            return null;

        String value = "http://" + getTwonkyIP() + "/rpc/rescan";
        String result = HttpFactory.doGetRequest(value, false);
        return result;
    }

    private boolean startTwonkyParser(String path, int start, int count) {
        path = ShareFolderManager.getInstance().getRealPath(path).replaceFirst("/home/", "/");
        Log.d(TAG, "twonky origin path : " + path);

        if (mCacheMap.contains(path)) {
            Log.d(TAG, "twonky cache contain : " + path);
            return true;
        }

        String[] paths = path.split("/");
        int length = paths.length;
        Log.d(TAG, "twonky origin path length : " + length);
        if (length == 0) {
            //root folder, get twonky server
            String server = parserTwonkyServer();
            if (server != null && !server.equals("")) {
                //get twonky category "Photo"
                String photos = parserTwonkyCategory(server, "Photos", "?start=" + start + "&fmt=json");
                if (photos != null && !photos.equals("")) {
                    //get twonky category "By Folder"
                    String byFolder = parserTwonkyCategory(photos, "By Folder", "?start=" + start + "&fmt=json");
                    Log.d(TAG, "twonky byFolder " + byFolder);
                    if (byFolder != null && !byFolder.equals("")) {
                        mFolderMap.put(KEYWORD_FOLDER, byFolder);
                        mCacheMap.add(path);
                        return true;
                    }
                }
            }
        } else if (0 < length && length <= 2) {
            return parserTwonkyFolder(path, start, count, getFolderUrlFromMap(false, KEYWORD_FOLDER));
        } else {
            //other folder
            String currentUrl = "";
            String tmp = "/" + paths[1] + "/";
            if (!mCacheMap.contains(tmp))
                parserTwonkyFolder(tmp, start, count, getFolderUrlFromMap(false, KEYWORD_FOLDER));

            //loop check the folder
            for (int i = 2; i < length; i++) {
                tmp += paths[i] + "/";
                Log.d(TAG, "twonky folder path : " + tmp);

                String nextUrl = getFolderUrlFromMap(false, tmp);
                if (nextUrl == null || nextUrl.equals("")) {
                    parserTwonkyFolder(tmp.replaceFirst(paths[i] + "/", ""), start, count, currentUrl);
                    nextUrl = getFolderUrlFromMap(false, tmp);
                }

                Log.d(TAG, "twonky folder url : " + nextUrl);

                if (nextUrl != null && !"".equals(nextUrl)) {
                    currentUrl = nextUrl;
                } else {
                    return false;
                }
            }

            return parserTwonkyFolder(path, start, count, getFolderUrlFromMap(false, path));
        }

        return false;
    }

    private String getUrlFromMap(boolean convertLink, FileInfo.TYPE type, String path) {
        String convertPath = path.replaceFirst("." + FilenameUtils.getExtension(path), "");
        if (type.equals(FileInfo.TYPE.DIR))
            return getFolderUrlFromMap(convertLink, convertPath);
        else if (type.equals(FileInfo.TYPE.PHOTO))
            return getImageUrlFromMap(convertLink, convertPath);
        else
            return null;
    }

    private String getFolderUrlFromMap(boolean convertLink, String path) {
        String result = "";
        if (mFolderMap != null) {
            result = mFolderMap.get(path);
            if (convertLink && result != null && !result.equals(""))
                result = convertUrl(result);
        }
        return result;
    }

    private String getImageUrlFromMap(boolean convertLink, String path) {
        path = ShareFolderManager.getInstance().getRealPath(path).replaceFirst("/home/", "/");

        String result = "";
        if (mImageMap != null) {
            result = mImageMap.get(path);
            if (convertLink && result != null && !result.equals(""))
                result = convertUrl(result);
        }
        return result;
    }

    private String parserTwonkyServer() {
        String value = "http://" + getTwonkyIP() + "/nmc/rss/server?start=0&fmt=json";
        String result = HttpFactory.doGetRequest(value, true);

        String target = "";
        try {
            JSONObject obj = new JSONObject(result);
            JSONArray items = new JSONArray(obj.optString("item"));
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                JSONObject serverJson = new JSONObject(item.optString("server"));
                String[] baseURLs = serverJson.optString("baseURL").split("/");
                String baseURL = "";
                for (String base : baseURLs) {
                    if (base.contains(":9000")) {
                        baseURL = base;
                        break;
                    }
                }

                JSONObject enclosureJson = new JSONObject(item.optString("enclosure"));
                String url = enclosureJson.optString("url");
                if (!baseURL.equals("") && url.contains(baseURL)) {
                    target = url;
                    break;
                }

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return target;
    }

    private String parserTwonkyCategory(String url, String keyword, String param) {
        String newUrl = convertUrl(url) + param;
        String result = HttpFactory.doGetRequest(newUrl, true);

        String target = "";
        try {
            JSONObject obj = new JSONObject(result);
            JSONArray items = new JSONArray(obj.optString("item"));
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                String title = item.optString("title");
                if (title.equals(keyword)) {
                    JSONObject enclosureJson = new JSONObject(item.optString("enclosure"));
                    target = enclosureJson.optString("url");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return target;
    }

    private boolean parserTwonkyFolder(String path, int start, int count, String url) {
        if (url != null && !url.equals("")) {
            String newUrl = convertUrl(url) + "?start=" + start + "&count=" + count + "&fmt=json";
            String result = HttpFactory.doGetRequest(newUrl, true);
            String target = "";
            try {
                JSONObject obj = new JSONObject(result);
                String folderChildCount = obj.optString("childCount");
                JSONArray items = new JSONArray(obj.optString("item"));
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.optJSONObject(i);
                    String title = item.optString("title");
                    String metaString = item.optString("meta");
                    if (metaString != null && !metaString.equals("")) {
                        JSONObject meta = new JSONObject(metaString);
                        String childCount = meta.optString("childCount");
                        if (childCount != null && !childCount.equals("")) {
                            //folder item, record it to folder hash map
                            JSONObject enclosure = new JSONObject(item.optString("enclosure"));
                            target = enclosure.optString("url");
                            mFolderMap.put(path + title + "/", target);
                            //Log.d(TAG, "key: " + path + title + "/, value: " + target);
                        } else {
                            //image item, record it to image hash map
                            String extension = meta.optString("pv:extension");
                            JSONArray res = new JSONArray(meta.optString("res"));
                            for (int j = 0; j < res.length(); j++) {
                                JSONObject tmp = res.optJSONObject(j);
                                target = tmp.optString("value");
                                if (target.contains("?"))
                                    target = target.substring(0, target.indexOf('?'));
                                mImageMap.put(path + title, target);
                                //Log.d(TAG, "key: " + path + title + ", value: " + target);
                            }
                        }
                    }
                }
                Log.d(TAG, "twonky folder size : " + mFolderMap.size());
                Log.d(TAG, "twonky image size : " + mImageMap.size());
                int nextStart = start + count;
                if (Integer.parseInt(folderChildCount) > nextStart)
                    return parserTwonkyFolder(path, nextStart, count, url);
                else {
                    mCacheMap.add(path);
                    return true;
                }
            } catch (JSONException e) {
                e.printStackTrace();

            }
        }

        return false;
    }

    //because we can't check the url in twonky url map is correct, convert it before you start to use the url
    private String convertUrl(String url) {
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String hostname = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.TWONKY);
        String[] splits = url.split("/");
        String newUrl = "";
        boolean add = false;

        for (String split : splits) {
            if (add) {
                newUrl = newUrl + "/" + split;
            } else {
                if (split.contains(":9000")) {
                    newUrl = "http://" + hostname;
                    add = true;
                }
            }
        }
        return newUrl;
    }
}
