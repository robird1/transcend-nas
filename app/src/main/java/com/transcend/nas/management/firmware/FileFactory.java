package com.transcend.nas.management.firmware;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerInfo;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.NASUtils;
import com.transcend.nas.R;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.externalstorage.ExternalStorageController;
import com.tutk.IOTC.P2PService;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Created by ike_lee on 2016/5/23.
 */
public class FileFactory {

    private static final String TAG = FileFactory.class.getSimpleName();
    private static FileFactory mFileFactory;
    private static final Object mMute = new Object();
    private ArrayList<FileInfo> mFileList;

    public FileFactory() {
        mFileList = new ArrayList<FileInfo>();
    }

    public static FileFactory getInstance() {
        synchronized (mMute) {
            if (mFileFactory == null)
                mFileFactory = new FileFactory();
        }
        return mFileFactory;
    }

    public void addFileTypeSortRule(ArrayList<FileInfo> fileList) {
        ArrayList<FileInfo> tmp = new ArrayList<FileInfo>();
        for (FileInfo file : fileList) {
            tmp.add(file);
        }
        fileList.clear();

        for (FileInfo file : tmp) {
            if (file.type == FileInfo.TYPE.DIR) {
                fileList.add(file);
            }
        }

        for (FileInfo file : tmp) {
            if (file.type != FileInfo.TYPE.DIR) {
                fileList.add(file);
            }
        }
    }

    public void addFolderFilterRule(String path, ArrayList<FileInfo> fileList) {
        if (NASApp.ROOT_SMB.equals(path)) {
            for (FileInfo file : fileList) {
                if (file.name.equals("homes")) {
                    fileList.remove(file);
                    break;
                }
            }

            for (FileInfo file : fileList) {
                if (file.name.equals("Public")) {
                    fileList.remove(file);
                    fileList.add(0, file);
                    break;
                }
            }

            Server server = ServerManager.INSTANCE.getCurrentServer();
            String username = server.getUsername();
            if (username != null && !username.equals("admin")) {
                ArrayList<FileInfo> tmp = new ArrayList<>();
                for (FileInfo file : fileList) {
                    if (file.name.equals("Public") || file.name.equals(username))
                        tmp.add(file);
                }

                fileList.clear();
                for (FileInfo file : tmp) {
                    fileList.add(file);
                }
            }

            for (FileInfo file : fileList) {
                if (file.time.startsWith("1970/01/01")) {
                    file.time = "";
                }
            }
        }
    }

    public boolean isTopDirectory(Context context, String mode, String root, String path) {
        if (NASApp.MODE_SMB.equals(mode)) {
            return path.equals(root);
        } else {
            if (NASUtils.isSDCardPath(context, path)) {
                root = NASUtils.getSDLocation(context);
            }

            File base = new File(root);
            File file = new File(path);
            return file.equals(base);
        }
    }

    public boolean isDownloadDirectory(Context context , String path){
        String download = NASPref.getDownloadLocation(context);
        File base = new File(download);
        File file = new File(path);
        return file.equals(base);
    }

    public void displayPhoto(Context context, boolean thumbnail, String path, ImageView view) {
        String url = getPhotoPath(context, thumbnail, path);
        if (path.startsWith(NASApp.ROOT_STG) || NASUtils.isSDCardPath(context, path)) {
            FileInfo.TYPE type = FileInfo.getType(path);
            if (type.equals(FileInfo.TYPE.PHOTO)) {
                DisplayImageOptions options = new DisplayImageOptions.Builder()
                        .bitmapConfig(Bitmap.Config.RGB_565)
                        .cacheInMemory(true)
                        .cacheOnDisk(false)
                        .build();

                if (!url.startsWith("content://")) {
                    url = Uri.decode(url);
                }
                ImageLoader.getInstance().displayImage(url, view, options);
            } else if(type.equals(FileInfo.TYPE.VIDEO)){
                //TODO : load video thumbnail
                //MICRO_KIND, size: 96 x 96 thumbnail
                //Bitmap bmThumbnail = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MICRO_KIND);
                //view.setImageBitmap(bmThumbnail);
            } else if(type.equals(FileInfo.TYPE.MUSIC)){
                //TODO : load music thumbnail
            }
        } else {
            ImageLoader.getInstance().displayImage(url, view);
        }
    }

    public String getPhotoPath(Context context, boolean thumbnail, String path) {
        return getPhotoPath(context, false, thumbnail, path);
    }

    public String getPhotoPath(Context context, boolean forceLocal, boolean thumbnail, String path) {
        String url = "";
        if (path.startsWith(NASApp.ROOT_STG)) {
            url = "file://" + path;
        } else if (NASUtils.isSDCardPath(context, path)) {
            Uri uri = new ExternalStorageController(context).getSDFileUri(path);
            if (uri != null) {
                url = uri.toString();
            }
        } else {
            //First, try twonky image, and try webdav image when twonky image empty
            url = TwonkyManager.getInstance().getUrlFromPath(thumbnail, path);
            if(null == url || "".equals(url)) {
                Server server = ServerManager.INSTANCE.getCurrentServer();
                String hostname = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
                String username = server.getUsername();
                String hash = server.getHash();
                String filepath;
                if (path.startsWith(Server.HOME))
                    filepath = Server.USER_DAV_HOME + path.replaceFirst(Server.HOME, "/");
                else if (path.startsWith("/" + username + "/"))
                    filepath = Server.USER_DAV_HOME + path.replaceFirst("/" + username + "/", "/");
                else {
                    if (username.equals("admin")) {
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

                if(forceLocal) {
                    ServerInfo info = server.getServerInfo();
                    if(info != null)
                        hostname = info.ipAddress;
                }

                if (thumbnail)
                    url = "http://" + hostname + filepath + "?session=" + hash + "&thumbnail";
                else
                    url = "http://" + hostname + filepath + "?session=" + hash + "&webview";
            }
            Log.d(TAG, "path : " + path + ", url : " + url);
        }
        return url;
    }

    public String getFileSize(String path) {
        File file = new File(path);
        long length = 0;
        if (file != null && file.exists()) {
            if (file.isDirectory()) {
                for (File f : file.listFiles()) {
                    if (f.isDirectory()) {
                        for (File child : f.listFiles()) {
                            length = length + child.length();
                        }
                    } else {
                        length += f.length();
                    }
                }
            } else {
                length = file.length();
            }
        }
        return getFileSize(length);
    }

    public String getFileSize(Long size) {
        //calculator the file size
        String s = " MB";
        double sizeMB = (double) size / 1024 / 1024;
        if (sizeMB < 1) {
            sizeMB = (double) size / 1024;
            s = " KB";
        } else if (sizeMB >= 1000) {
            sizeMB = (double) sizeMB / 1024;
            s = " GB";
            if (sizeMB >= 1000) {
                sizeMB = (double) sizeMB / 1024;
                s = " TB";
            }
        }

        //format the size
        DecimalFormat df = new DecimalFormat("#.##");
        String formatSize = df.format(sizeMB) + s;

        return formatSize;
    }

    public void setFileList(ArrayList<FileInfo> list) {
        if (mFileList == null)
            mFileList = new ArrayList<FileInfo>();
        mFileList.clear();

        for (FileInfo info : list) {
            mFileList.add(info);
        }
    }

    public ArrayList<FileInfo> getFileList() {
        return mFileList;
    }
}
