package com.transcend.nas.management.firmware;

import android.util.Log;

import com.transcend.nas.common.HttpRequestFactory;

import java.util.ArrayList;
import java.util.List;

public class TwonkyHelper {
    private static final String TAG = TwonkyHelper.class.getSimpleName();

    public boolean addTwonkySharedFolder(String ip, List<String> lists) {
        if (ip == null || lists == null)
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
        String current = getTwonkySharedFolder(ip);
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
                    isSuccess = setTwonkySharedFolder(ip, addLists);
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

    private String getTwonkySharedFolder(String ip) {
        String value = "http://" + ip + "/rpc/get_option?contentdir";
        String result = HttpRequestFactory.doGetRequest(value);
        return result;
    }

    private boolean setTwonkySharedFolder(String ip, List<String> addLists) {
        boolean isSuccess = false;
        String value = "http://" + ip + "/rpc/set_option?contentdir=";
        String folders = "";
        int length = addLists.size();

        for (int i = 0; i < length; i++) {
            String tmp = addLists.get(i);
            if (tmp.startsWith("+"))
                tmp = "%2B" + tmp.substring(1, tmp.length());
            folders = folders + tmp + (i == length - 1 ? "" : ",");
        }

        String result = HttpRequestFactory.doGetRequest(value + folders);
        if (result != null && result.equals(folders.replace("%2B", "+")))
            isSuccess = true;

        return isSuccess;
    }

    public String doTwonkyRescan(String ip) {
        String value = "http://" + ip + "/rpc/rescan";
        String result = HttpRequestFactory.doGetRequest(value, false);
        return result;
    }
}
