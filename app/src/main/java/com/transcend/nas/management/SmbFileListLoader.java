package com.transcend.nas.management;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.realtek.nasfun.api.HttpClientManager;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.common.FileFactory;
import com.transcend.nas.service.TwonkyManager;
import com.tutk.IOTC.P2PService;

import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.net.HttpURLConnection;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * Created by silverhsu on 16/1/7.
 */
public class SmbFileListLoader extends SmbAbstractLoader {

    private static final String TAG = SmbFileListLoader.class.getSimpleName();

    private ArrayList<FileInfo> mFileList;
    private String mPath;
    private String mError;

    public SmbFileListLoader(Context context, String path) {
        super(context);
        mFileList = new ArrayList<FileInfo>();
        mPath = format(path);
    }

    @Override
    public Boolean loadInBackground() {
        try {
            super.loadInBackground();
            return updateFileList();
        } catch (Exception e) {
            e.printStackTrace();
            setException(e);
        }
        return false;
    }

    private boolean updateFileList() throws MalformedURLException, SmbException {
        String url = super.getSmbUrl(mPath);
        SmbFile target = new SmbFile(url);
        if (target.isFile())
            return false;

        SmbFile[] files = target.listFiles();
        Log.w(TAG, "SmbFile[] size: " + files.length);
        for (SmbFile file : files) {
            if (file.isHidden())
                continue;
            FileInfo fileInfo = new FileInfo();
            fileInfo.path = TextUtils.concat(mPath, file.getName()).toString();
            fileInfo.name = file.getName().replace("/", "");
            fileInfo.time = FileInfo.getTime(file.getLastModified());
            fileInfo.type = file.isFile() ? FileInfo.getType(file.getPath()) : FileInfo.TYPE.DIR;
            fileInfo.size = file.length();
            mFileList.add(fileInfo);
        }
        Log.w(TAG, "mFileList size: " + mFileList.size());

        //get shared folder mapping path for admin user
        if (mPath.equals(NASApp.ROOT_SMB) && "admin".equals(mUsername)) {
            int size = FileFactory.getInstance().getRealPathMapSize();
            int shardFolderSize = 0;
            for (FileInfo file : mFileList) {
                if (!file.name.equals(mUsername) && !file.name.equals("homes"))
                    shardFolderSize++;
            }
            Log.w(TAG, "ShardFolderSize : " + shardFolderSize + ", RealPathMapSize : " + size);
            if (shardFolderSize > 0) {
                if (shardFolderSize != size || !FileFactory.getInstance().checkRealPathMapLifeCycle()) {
                    FileFactory.getInstance().cleanRealPathMap();
                    getSharedList();
                    Log.d(TAG, "folder mapping size : " + FileFactory.getInstance().getRealPathMapSize());
                }
            }
        }

        if (NASPref.useTwonkyServer) {
            if (mPath.equals(NASApp.ROOT_SMB))
                TwonkyManager.getInstance().doTwonkyRescan(false);
            TwonkyManager.getInstance().startTwonkyParser(mPath, 0, 100);
        }

        return true;
    }

    private boolean getSharedList() {
        boolean isSuccess = false;
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String hostname = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
        String hash = server.getHash();
        DefaultHttpClient httpClient = HttpClientManager.getClient();
        String commandURL = "http://" + hostname + "/nas/get/sharelist";
        HttpResponse response = null;
        InputStream inputStream = null;
        try {
            do {
                HttpPost httpPost = new HttpPost(commandURL);
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("hash", hash));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                response = httpClient.execute(httpPost);
                if (response == null) {
                    Log.e(TAG, "response is null");
                    break;
                }
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    Log.e(TAG, "response entity is null");
                    break;
                }
                inputStream = entity.getContent();
                String inputEncoding = EntityUtils.getContentCharSet(entity);
                if (inputEncoding == null) {
                    inputEncoding = HTTP.DEFAULT_CONTENT_CHARSET;
                }

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(inputStream, inputEncoding);
                int eventType = xpp.getEventType();
                String curTagName = null;
                String text = null;
                String name = null;
                String path = null;
                boolean add = false;

                do {
                    String tagName = xpp.getName();
                    if (eventType == XmlPullParser.START_TAG) {
                        curTagName = tagName;
                    } else if (eventType == XmlPullParser.TEXT) {
                        if (curTagName != null) {
                            text = xpp.getText();
                            if (curTagName.equals("name")) {
                                name = text;
                            } else if (curTagName.equals("path")) {
                                path = text;
                                if (name != null) {
                                    isSuccess = true;
                                    if (add) {
                                        FileFactory.getInstance().addRealPathToMap(name, path);
                                        boolean addToSmbList = true;
                                        for (FileInfo info : mFileList) {
                                            if (name.equals(info.name)) {
                                                addToSmbList = false;
                                                break;
                                            }
                                        }

                                        if(addToSmbList) {
                                            FileInfo fileInfo = new FileInfo();
                                            fileInfo.path = TextUtils.concat(mPath, name + "/").toString();
                                            fileInfo.name = name;
                                            fileInfo.time = "1970/01/01";
                                            fileInfo.type = FileInfo.TYPE.DIR;
                                            fileInfo.size = Long.valueOf(0);
                                            mFileList.add(fileInfo);
                                        }
                                    }
                                    name = null;
                                    path = null;
                                    add = false;
                                }
                            } else if (curTagName.equals("services")) {
                                if (text.equals("smb")) {
                                    add = true;
                                }
                            }
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        curTagName = null;
                    }
                    eventType = xpp.next();
                } while (eventType != XmlPullParser.END_DOCUMENT);
            } while (false);

        } catch (XmlPullParserException e) {
            Log.d(TAG, "XML Parser error");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "Fail to connect to server");
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "catch IllegalArgumentException");
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                isSuccess = false;
                e.printStackTrace();
            }
        }

        return isSuccess;
    }

    public String getPath() {
        return mPath;
    }

    public ArrayList<FileInfo> getFileList() {
        return mFileList;
    }

}
