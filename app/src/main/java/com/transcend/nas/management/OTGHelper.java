//package com.transcend.nas.management;
//
//import android.Manifest;
//import android.app.Activity;
//import android.app.PendingIntent;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.hardware.usb.UsbDevice;
//import android.hardware.usb.UsbManager;
//import android.support.v4.app.ActivityCompat;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Iterator;
//
//import static com.transcend.nas.R.string.device;
//
///**
// * Created by steve_su on 2016/12/22.
// */
//
//public class OTGHelper {
//    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
//                synchronized (this) {
//                    mOTGRequestDialog = null;
//                    device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
//                    SecurityListener.getInstance().notifySecurityListener(SecurityListener.SecurityStatus.Detached);
//                    if (mUsbManager.getDeviceList().size() != 0) {
//                        HashMap<String, UsbDevice> usbMap = mUsbManager.getDeviceList();
//                        Iterator<UsbDevice> deviceIterator = usbMap.values().iterator();
//                        boolean isOTGExist = false;
//                        while (deviceIterator.hasNext()) {
//                            UsbDevice device = deviceIterator.next();
//                            if (device.getSerialNumber() != null) {
//                                if (!isOTGExist)
//                                    isOTGExist = true;
//
//                            }
//                        }
//                        itemOTG.setVisible(isOTGExist);
//                        itemSecurity.setVisible(isOTGExist);
//
//                    } else {
//                        itemOTG.setVisible(false);
//                        itemSecurity.setVisible(false);
//                    }
//                    Constant.nowMODE = Constant.MODE.LOCAL;
//                    Constant.usbDevice = device;
//                    Constant.isSecurityLogin = false;
//                    itemOTG.setTitle("OTG");
//                    mRoot = Constant.ROOT_STG;
//                    doLoad(Pref.getMainPageLocation(context));
//                }
//            }
//            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
//                synchronized (this) {
//                    resetSerailNumber();
//                    resetDropDownMapAndList();
//                    device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
//                    Constant.usbDevice = device ;
//                    if (!hasPermission()) {//if no permission, then ask for it
//                        ActivityCompat.requestPermissions((Activity) context,
//                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                                REQUEST_WRITE_STORAGE);
//                    } else {
//                        try {
//                            Thread.sleep(1500);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        itemOTG.setVisible(true);
//                        if (!doCheckOTGPermission())
//                            doOTGRequestPermission();
//
//                        if( device.getProductName().contains("TS") ){
//                            SecurityListener.getInstance().notifySecurityListener(SecurityListener.SecurityStatus.Attached);
//                            //itemOTG.setTitle("SSD");
//                            if( !doCheckUSBPermission() )
//                                doUSBRequestPermission(intent);
//                        }
//                    }
//                }
//            }
//            if(Constant.USB_Permission.equals(action)){
//                synchronized (this){
//                    if( doCheckUSBPermission() ) {
//                        doLoadScsiIDInfo();
//                    }
//                }
//            }
//        }
//    };
//
//    void init() {
//        Constant.nowMODE = Constant.MODE.LOCAL;
//        mPath = mRoot = Constant.ROOT_STG;
//        resetSerailNumber();
////        resetDropDownMapAndList();
//        mFileList = new ArrayList<FileInfo>();
//        pendingIntent = PendingIntent.getBroadcast(this , 0 , new Intent(ACTION_USB_PERMISSION), 0);
//        intentFilter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
//        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
//        intentFilter.addAction(ACTION_USB_PERMISSION);
//        registerReceiver(mUsbReceiver, intentFilter);
////        Pref.setFileSortType(this, Pref.Sort.NAME);
//
//    }
//
//    private void resetSerailNumber() {
//        mSerialNumber = new ArrayList<String>();
//        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
//        Constant.usbManager = mUsbManager ;
//        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
//        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
//        while (deviceIterator.hasNext()) {
//            UsbDevice device = deviceIterator.next();
//            mSerialNumber.add(device.getSerialNumber());
//        }
//    }
//
//}
