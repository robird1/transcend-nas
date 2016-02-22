package com.realtek.nasfun.api;

import com.transcend.nas.R;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


// modified by silver
//import com.realtek.nasfun.R;

public class IpodCGIPhotoManager extends PhotoManager{
	public IpodCGIPhotoManager(Server server){
		super(server);
	}
	
	public List<MediaItemPhoto> getAllPhotos(){
		CGIResponse response = server.sendCGICommand("hdds|picf|all");
		return getPhotoFromResponse(response);
		
	}
	
	public List<MediaItemPhoto> getRecentPhotos() {
		CGIResponse response = server.sendCGICommand("hdds|picf|new");
		return getPhotoFromResponse(response);
	}

	private ArrayList<MediaItemPhoto> getPhotoFromResponse(CGIResponse response){
		/*
		 * (1)1185/1186 media nas response example:
		 * 		"id": "hdds|picf|/tmp/usbmounts/sdb1/admin/Upload/1378952949636.jpg|0",
		 *	 	"name": "2013:09:12 10:29:04-2592 x 1944",
		 * 		"type": "picture",
		 * 		"thumbtype": "photo"
		 * 
		 *		"id": "hdds|picf|/tmp/usbmounts/sdb1/admin/Photos|10|",
		 * 		"name": "MORE...",
		 * 		"type": "more",
		 * 		"thumbtype": "photo"
		 * 
		 * (2)1185/1186 pure nas response example:
		 * 
		 */
		ArrayList<MediaItemPhoto> list = null;
		String url;
		if(response != null) {
			int count = response.getChildren().size();
			if(count > 0){
				list = new ArrayList<MediaItemPhoto>(count);
				for(CGIResponse.Item item: response.getChildren()){
					if(item.getType().equals("picture")) {
						StringTokenizer strToken = new StringTokenizer(item.getName(), "-");
						String takeTime = null, resolution=null;
						if(strToken.hasMoreTokens())
							takeTime= strToken.nextToken();
						if(strToken.hasMoreTokens())
							resolution = strToken.nextToken();
						String path = Util.getPathFromID(item.getId(),server.isUserAdmin());
						String fileName = Util.getFileNameFromPath(path);
						String davHome = server.getDavHome();
						url = "http://"+server.hostname+davHome+path+"?session="+server.hash+"&login="+server.username;
						
						//MediaItemPhoto mi = new MediaItemPhoto(R.drawable.icon_photo, fileName, resolution, false);
						// modified by silver
						MediaItemPhoto mi = new MediaItemPhoto(R.drawable.ic_logo, fileName, resolution, false);
						mi.setPath(url);
						list.add(mi);
					}
				}
			}
		}
		return list;
	}
}
