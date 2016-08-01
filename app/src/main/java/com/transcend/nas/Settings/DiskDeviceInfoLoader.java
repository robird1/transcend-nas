package com.transcend.nas.settings;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import com.realtek.nasfun.api.HttpClientManager;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASPref;
import com.tutk.IOTC.P2PService;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ikelee on 16/7/22.
 */
public class DiskDeviceInfoLoader extends AsyncTaskLoader<Boolean> {

    private static final String TAG = DiskDeviceInfoLoader.class.getSimpleName();
    private List<DiskStructDevice> mDevices;
    private int mRetry = 1;

    public DiskDeviceInfoLoader(Context context) {
        super(context);
        mDevices = new ArrayList<DiskStructDevice>();
    }

    @Override
    public Boolean loadInBackground() {
        boolean isSuccess = getDevicesInfo(mRetry);
        return isSuccess;
    }

    private boolean getDevicesInfo(int retry) {
        if(retry < 0)
            return false;

        Server server = ServerManager.INSTANCE.getCurrentServer();
        String hostname = server.getHostname();
        String hash = server.getHash();
        String p2pIP = P2PService.getInstance().getP2PIP();
        if (hostname.contains(p2pIP)) {
            hostname = p2pIP + ":" + P2PService.getInstance().getP2PPort(P2PService.P2PProtocalType.HTTP);
        }
        DefaultHttpClient httpClient = HttpClientManager.getClient();
        String commandURL = "http://" + hostname + "/nas/get/devices";
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

                DiskFactory.getInstance().cleanDiskDevices();
                DiskStructDevice device = null;
                DiskStructPartition partition = null;
                DiskStructRAID raid = null;
                int eventType = xpp.getEventType();
                String curTagName = null;
                String text = null;

                do {
                    String tagName = xpp.getName();
                    if (eventType == XmlPullParser.START_TAG) {
                        curTagName = tagName;
                        if (curTagName.equals("device"))
                            device = new DiskStructDevice();
                        else if (curTagName.equals("partition")) {
                            partition = new DiskStructPartition();
                        } else if (curTagName.equals("raid")) {
                            raid = new DiskStructRAID();
                        }
                    } else if (eventType == XmlPullParser.TEXT) {
                        if (curTagName != null) {
                            text = xpp.getText();
                            if (raid != null && DiskStructRAID.FORMAT.contains(curTagName)) {
                                String specialkey = "raiddevice";
                                if (curTagName.equals(specialkey)) {
                                    String raiddevice = raid.infos.get(specialkey);
                                    if (raiddevice != null) {
                                        text = raiddevice + "," + text;
                                    }
                                }
                                raid.infos.put(curTagName, text);
                            } else if (partition != null && DiskStructPartition.FORMAT.contains(curTagName)) {
                                String specialkey = "mountpoint";
                                if (curTagName.equals(specialkey)) {
                                    String mountpoint = partition.infos.get(specialkey);
                                    if (mountpoint != null) {
                                        text = mountpoint + "," + text;
                                    }
                                }
                                partition.infos.put(curTagName, text);
                            } else if (device != null && DiskStructDevice.FORMAT.contains(curTagName)) {
                                device.infos.put(curTagName, text);
                            } else if (curTagName.equals("reason")) {
                                if (text != null && text.equals("No Permission")) {
                                    boolean success = server.connect();
                                    if (success) {
                                        ServerManager.INSTANCE.saveServer(server);
                                        ServerManager.INSTANCE.setCurrentServer(server);
                                        NASPref.setSessionVerifiedTime(getContext(), Long.toString(System.currentTimeMillis()));
                                        return getDevicesInfo(retry-1);
                                    } else {
                                        String error = server.getLoginError();
                                        Log.d(TAG, "login fail due to : " + error);
                                        return false;
                                    }
                                }
                            } else {
                                Log.d("ike", "other " + curTagName + " : " + text);
                            }
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        if (tagName != null) {
                            if (tagName.equals("raid") && raid != null) {
                                if (device == null)
                                    device = new DiskStructDevice();
                                device.raid = raid;
                                raid = null;
                            } else if (tagName.equals("partition") && partition != null) {
                                if (device == null)
                                    device = new DiskStructDevice();
                                if (device.partitions == null)
                                    device.partitions = new ArrayList<DiskStructPartition>();
                                device.partitions.add(partition);
                                partition = null;
                            } else if (tagName.equals("device") && device != null) {
                                //revise device model name
                                if (device.raid != null) {
                                    String model = device.infos.get("model") + " (" + device.raid.infos.get("level") + ")";
                                    device.infos.put("model", model);
                                }
                                //calculate device total size
                                device.totalSize = DiskFactory.getInstance().getDeviceTotalSize(device);
                                //calculate device available size
                                device.availableSize = DiskFactory.getInstance().getDeviceAvailableSize(device);
                                //calculate device block size
                                device.blockSize = DiskFactory.getInstance().getDeviceBlocksSize(device);
                                mDevices.add(device);
                                device = null;
                            }
                        }
                        curTagName = null;
                    }
                    eventType = xpp.next();
                } while (eventType != XmlPullParser.END_DOCUMENT);
                mDevices = DiskFactory.getInstance().createDiskDevices(mDevices);
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
                e.printStackTrace();
            }
        }

        return (mDevices != null && mDevices.size() > 0);
    }

    public List<DiskStructDevice> getDevices(){
        return mDevices;
    }
}
