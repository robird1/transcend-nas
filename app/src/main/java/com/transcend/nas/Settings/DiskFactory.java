package com.transcend.nas.settings;

import android.util.Log;

import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.realtek.nasfun.api.HttpClientManager;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.tutk.IOTC.P2PService;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ikelee on 16/07/20.
 */
public class DiskFactory {
    private static final String TAG = DiskFactory.class.getSimpleName();
    private static final Object mMute = new Object();
    private static DiskFactory mDiskFactory;

    private List<DiskStructDevice> mDevices;
    private DiskStructDevice mCurrentDevice;

    public DiskFactory() {
        mDevices = new ArrayList<>();
    }

    public static DiskFactory getInstance() {
        synchronized (mMute) {
            if (mDiskFactory == null)
                mDiskFactory = new DiskFactory();
        }
        return mDiskFactory;
    }

    public List<DiskStructDevice> createDiskDevices(List<DiskStructDevice> devices) {
        for(DiskStructDevice tmp : devices){
            mDevices.add(tmp);
        }

        //move the raid disk to first page
        for (DiskStructDevice tmp : mDevices) {
            if (tmp.raid != null) {
                mDevices.remove(tmp);
                mDevices.add(0, tmp);
                break;
            }
        }

        //move the usb device to last page
        //TODO : multiple usb devices
        DiskStructDevice usb = null;
        for (DiskStructDevice tmp : mDevices) {
            String external = tmp.infos.get("external");
            if (external != null && external.equals("yes")) {
                usb = tmp;
                mDevices.remove(tmp);
                break;
            }
        }
        if (usb != null) {
            mDevices.add(usb);
        }

        return mDevices;
    }

    public boolean cleanDiskDevices() {
        if (mDevices == null)
            return false;
        mDevices.clear();
        return true;
    }

    public List<DiskStructDevice> getDiskDevices() {
        if (mDevices != null) {
            List<String> raiddevices = new ArrayList<>();
            for (DiskStructDevice device : mDevices) {
                if (device.raid != null) {
                    String tmp = device.raid.infos.get("raiddevice");
                    if (tmp != null && !tmp.equals("")) {
                        String[] tmps = tmp.split(",");
                        for (String result : tmps) {
                            raiddevices.add(result);
                        }
                    }
                    break;
                }
            }
            for (String raiddevice : raiddevices) {
                for (DiskStructDevice device : mDevices) {
                    if (device.partitions != null && device.raid == null) {
                        for (DiskStructPartition partition : device.partitions) {
                            if (raiddevice != null && raiddevice.equals(partition.infos.get("path"))) {
                                //remove the device from list
                            }
                        }
                    }
                }
            }
        }
        return mDevices;
    }

    public Map<String, String> createDiskDevicesTemperature() {
        Map<String, String> mTemperature = new HashMap<String, String>();

        boolean startParser = false;
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String hostname = server.getHostname();
        String p2pIP = P2PService.getInstance().getP2PIP();
        if (hostname.contains(p2pIP)) {
            hostname = p2pIP + ":" + P2PService.getInstance().getP2PPort(P2PService.P2PProtocalType.HTTP);
        }
        DefaultHttpClient httpClient = HttpClientManager.getClient();
        String commandURL = "http://" + hostname + "/nas/get/info";
        HttpResponse response = null;
        InputStream inputStream = null;
        try {
            do {
                HttpGet httpGet = new HttpGet(commandURL);
                response = httpClient.execute(httpGet);
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

                do {
                    String tagName = xpp.getName();
                    if (eventType == XmlPullParser.START_TAG) {
                        curTagName = tagName;
                        if("hddtemp".equals(curTagName))
                            startParser = true;
                    } else if (eventType == XmlPullParser.TEXT) {
                        if (curTagName != null && startParser) {
                            text = xpp.getText();
                            mTemperature.put(curTagName, text);
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        if ("hddtemp".equals(tagName))
                            startParser = false;
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
                e.printStackTrace();
            }
        }


        for(String key : mTemperature.keySet()){
            for(DiskStructDevice device : mDevices){
                String path = device.infos.get("path");
                if(path != null && path.contains(key)){
                    device.infos.put("temperature", mTemperature.get(key) + "\u00B0" + "C");
                    break;
                }
            }
        }

        return mTemperature;
    }

    public float getDeviceTotalSize(DiskStructDevice device) {
        if (device.totalSize != 0) {
            return device.totalSize;
        }

        float sectorsize = 0, totalSize = 0;
        String tmp = device.infos.get("sectorsize");
        if (tmp != null && !tmp.equals("")) {
            sectorsize = Float.parseFloat(tmp);
        }

        tmp = device.infos.get("length");
        if (tmp != null && !tmp.equals("")) {
            float totalLength = Float.parseFloat(tmp);
            totalSize = sectorsize * totalLength;
        }
        return totalSize;
    }

    public float getDeviceAvailableSize(DiskStructDevice device) {
        if (device.availableSize != 0) {
            return device.availableSize;
        }

        float availSize = 0;
        int partitionCount = device.partitions.size();
        for (int i = 0; i < partitionCount; i++) {
            DiskStructPartition partition = device.partitions.get(i);
            //TODO: only calculate the mount point size
            String available = partition.infos.get("available");
            if (available != null && !available.equals("")) {
                availSize = availSize + Float.parseFloat(available);
            }
        }
        return availSize;
    }

    public String getDeviceAvailableSizePercent(DiskStructDevice device) {
        float totalSize = getDeviceTotalSize(device);
        float availSize = getDeviceAvailableSize(device);

        //format the size
        DecimalFormat df = new DecimalFormat("##.##");
        String formatSize = df.format(availSize / totalSize * 100) + "%";
        return formatSize;
    }

    public float getDeviceBlocksSize(DiskStructDevice device) {
        if (device.blockSize != 0) {
            return device.blockSize;
        }

        float blocksSize = 0;
        int partitionCount = device.partitions.size();
        for (int i = 0; i < partitionCount; i++) {
            DiskStructPartition partition = device.partitions.get(i);
            String blocks = partition.infos.get("blocks");
            if (blocks != null && !blocks.equals("")) {
                blocksSize = blocksSize + Float.parseFloat(blocks);
            }
        }
        return blocksSize;
    }

    public PieDataSet getPieChartDataSet(boolean showOtherEntry, DiskStructDevice device) {
        float totalSize = getDeviceTotalSize(device);
        float blocksSize = getDeviceBlocksSize(device);
        float availSize = getDeviceAvailableSize(device);

        ArrayList<PieEntry> entries = new ArrayList<PieEntry>();
        float size = 0;

        if (!showOtherEntry) {
            //we merge other partition into the used partition
            size = totalSize - availSize;
            if (size > 0 && totalSize != size) {
                PieEntry usedEntry = new PieEntry(size, "Used");
                entries.add(usedEntry);
            }
        } else {
            size = blocksSize - availSize;
            if (size > 0) {
                PieEntry usedEntry = new PieEntry(size, "Used");
                entries.add(usedEntry);
            }

            size = totalSize - blocksSize;
            if (size > 0 && totalSize != size) {
                PieEntry otherEntry = new PieEntry(size, "Other");
                entries.add(otherEntry);
            }
        }

        size = availSize;
        if (size > 0) {
            PieEntry availEntry = new PieEntry(availSize, "Available");
            entries.add(availEntry);
        }

        if (entries.size() > 0) {
            PieDataSet dataSet = new PieDataSet(entries, "");
            return dataSet;
        } else {
            return null;
        }
    }

    public void setCurrentDevice(DiskStructDevice device) {
        mCurrentDevice = device;
    }

    public DiskStructDevice getCurrentDevice() {
        return mCurrentDevice;
    }
}
