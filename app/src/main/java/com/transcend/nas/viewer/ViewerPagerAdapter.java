package com.transcend.nas.viewer;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASApp;

import java.util.ArrayList;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher.OnPhotoTapListener;

/**
 * Created by silverhsu on 16/2/23.
 */
public class ViewerPagerAdapter extends PagerAdapter {

    public static final String TAG = ViewerPagerAdapter.class.getSimpleName();

    private Context mContext;
    private ArrayList<String> mList;

    private OnPhotoTapListener mOnPhotoTapListener;

    public ViewerPagerAdapter(Context context) {
        mContext = context;
        mList = new ArrayList<String>();
    }

    public void setContent(ArrayList<String> list) {
        mList = list;
    }

    public void setOnPhotoTapListener(OnPhotoTapListener listener) {
        mOnPhotoTapListener = listener;
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        String path = mList.get(position);
        PhotoView pv = new PhotoView(mContext);
        pv.setDrawingCacheEnabled(false);
        pv.setOnPhotoTapListener(mOnPhotoTapListener);
        ImageLoader.getInstance().displayImage(toPhotoURL(path), pv);
        container.addView(pv);
        Log.w(TAG, "instantiateItem [" + position + "]: " + path);
        return pv;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        if (object instanceof ImageView) {
            ImageView iv = (ImageView) object;
            ImageLoader.getInstance().cancelDisplayTask(iv);
            container.removeView(iv);
        }
    }


    public void removeView(int index) {
        mList.remove(index);
        notifyDataSetChanged();
    }

    @Override
    public int getItemPosition(Object object) {
        if (mList.contains((View) object)) {
            return mList.indexOf((View) object);
        } else {
            return POSITION_NONE;
        }
    }

    private String toPhotoURL(String path) {
        String url;
        if (path.startsWith(NASApp.ROOT_STG)) {
            url = "file://" + path;
        }
        else {
            Server server = ServerManager.INSTANCE.getCurrentServer();
            String hostname = server.getHostname();
            String username = server.getUsername();
            String hash = server.getHash();
            String filepath;
            if(path.startsWith(Server.HOME))
                filepath = Server.USER_DAV_HOME + path.replaceFirst(Server.HOME, "/");
            else if(path.startsWith("/"+username+"/"))
                filepath = Server.USER_DAV_HOME + path.replaceFirst("/"+username+"/", "/");
            else {
                String[] paths = path.replaceFirst("/","").split("/");
                filepath = Server.ADMIN_DAV_HOME;
                for(int i=0 ;i < paths.length; i++){
                    if(i == 0)
                        filepath += "/"  + paths[i].toLowerCase();
                    else
                        filepath += "/"  + paths[i];
                }
            }
            url = "http://" + hostname + filepath + "?session=" + hash + "&webview";
        }
        return url;
    }

}
