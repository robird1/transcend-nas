package com.realtek.nasfun.api;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
// modified by silver
//import com.realtek.nasfun.R;
import com.realtek.nasfun.api.RtCGIPhotoListResponse.PhotoItem;
import com.transcend.nas.R;

public class RtCGIPhotoManager extends PhotoManager {
	private static final String TAG = "RtCGIPhotoManager";

	public RtCGIPhotoManager(Server server) {
		super(server);
	}

	@Override
	public List<MediaItemPhoto> getRecentPhotos() {
		String url = "photo&max=200";
		String cmd = "http://" + server.hostname + "/rtCGI.fcgi?id=3&url="+ url;
		RtCGIPhotoListResponse response = (RtCGIPhotoListResponse) server
				.getRtCGIResponse(cmd, RtCGIPhotoListResponse.class);
		return getPhotoFromResponse(response);
	}

	private ArrayList<MediaItemPhoto> getPhotoFromResponse(
			RtCGIPhotoListResponse response) {
		ArrayList<MediaItemPhoto> list = new ArrayList<MediaItemPhoto>();

		if (response == null){
			Log.w(TAG, "getPhotoFromResponse(), response is null");	
		} else if (response.status != 0) {
			Log.w(TAG, "getPhotoFromResponse(), response="+response);
		} else {
			Log.d(TAG, "queryFile, total=" + response.total);
			for (PhotoItem item : response.items) {
				String fileName = null, resolution = null, id = null, path = null, url = null;
				fileName = item.disp_name;
				resolution = Integer.toString(item.width) + "*"
						+ Integer.toString(item.height);
				//MediaItemPhoto mi = new MediaItemPhoto(R.drawable.icon_photo, fileName, resolution, false);
				// modified by silver
				MediaItemPhoto mi = new MediaItemPhoto(R.drawable.ic_logo_transcend, fileName, resolution, false);
				id = Integer.toString(item.id);
				path = getPathFromId(id);
				if (path != null) {
					String davHome = server.getDavHome();
					url = "http://"+server.hostname+davHome+path+"?session="+server.hash+"&login="+server.username;
					mi.setPath(url);				
				} else {
					Log.d(TAG, "A photo's id =" + item.id
							+ ", its relative path is null");
				}

				list.add(mi);
			}
		}
		return list;
	}

	/**
	 * get relative path from ID
	 * 
	 * Step 1: get full path from id
	 * \/storage\/sda1\/admin\/Photos\/theriseofaplanet14401.jpg
	 * 
	 * Step2:
	 * extract relative path from full path 
	 *
	 * @param id
	 * @return relative path, 	e.g./Photos/theriseofaplanet14401.jpg(user)
	 * 							e.g./admin/Photos/theriseofaplanet14401.jpg(admin)
	 */
	private String getPathFromId(String id) {
		String path = null;
		String url = "photo:" + id;
		
		String cmd = "http://"+server.hostname+"/rtCGI.fcgi?id=3&action=get_property&url="+url;
		RtCGIContentFilePropertyResponse response = (RtCGIContentFilePropertyResponse) server
				.getRtCGIResponse(cmd, RtCGIContentFilePropertyResponse.class);

		if(response != null){
			path = response.path;
			
			if(path != null){
				path = Util.extractRelativePath(path, server.isUserAdmin());
			}else{
				Log.d(TAG,"photo's full path is null" + "id = "+ id);
			}		
		}else{
			Log.d(TAG,"getFilePathFromResponse(), response is null"+ ", url ="+url);
		}

		return path;
	}
}
