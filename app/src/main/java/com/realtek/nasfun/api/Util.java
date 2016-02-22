package com.realtek.nasfun.api;

import java.util.StringTokenizer;

import android.util.Log;

public class Util {
	private static final String TAG = "Util";
	
	/**
	*
	* 1185/1186 media nas's ID example:
	*	music:hdds|aud|/tmp/usbmounts/sda1/admin/Music/1-01 Zielono Mi.mp3|13
	*	photo:hdds|picf|/tmp/usbmounts/sda1/admin/Upload/1378952949636.jpg|0
	*
	* 1185/1186 pure nas's and 1195 pure nas's ID examples are like above.
	* We only should replace "/tmp/usbmounts/sda1/" by "/home/".
	*
	*@param id, including a file's full path. 
	*@return a file's relative path
	*
	*/
	static String getPathFromID(String id, boolean isUserAdmin){
		StringTokenizer token = new StringTokenizer(id, "|");
		String path = null;
		int prefixLen, pathIdx, nextPathIdx;
		while(token.hasMoreTokens()){
			path = token.nextToken();
			//for 1185/1186 media nas
			if(path.startsWith("/tmp/usbmounts/")){
				// this field is path
				prefixLen = "/tmp/usbmounts/".length();
				pathIdx = path.indexOf('/', prefixLen);//for removing /tmp/usbmounts/sda1
				nextPathIdx = path.indexOf('/', pathIdx+1); //for removing /tmp/usbmounts/sda1/accountName
				
				if(isUserAdmin) {
					path = path.substring(pathIdx);
				}else{
					path = path.substring(nextPathIdx);
				}
				
				break;
			//for 1185/1186 pure nas or 1195 pure nas
			}else if(path.startsWith("/home/")){
				//this field is path
				prefixLen = "/home/".length();
				pathIdx = path.indexOf("/", prefixLen); //for removing /home/accountName
				
				if(isUserAdmin) {
					path = path.substring(prefixLen-1);
				}else{
					path = path.substring(pathIdx);
				}
				
				break;
			}
		}
		return path;
	}
	
	/**
	 * 
	 * full path example:
	 * 	music: \/storage\/sda1\/admin\/Music\/1-01 THE TIDE IS HIGH.mp3
	 * 	photo: \/storage\/sda1\/admin\/Photos\/theriseofaplanet14401.jpg
	 * 
	 * relative path example:
	 * 	music: /Music/1-01 THE TIDE IS HIGH.mp3(user)
	 *         /admin/Music/1-01 THE TIDE IS HIGH.mp3(admin)
	 *	photo: /Photos/theriseofaplanet14401.jpg(user)            
	 *         /admin/Photos/theriseofaplanet14401.jpg(admin)   
     *
	 * @param   full path
	 * @return  relative path 
	 * 
	 */
	static String extractRelativePath(String fullPath, boolean isUserAdmin){
		int prefixLen, pathIdx, nextPathIdx;
		String path  = fullPath;

		path = path.replace("\\","");
		Log.d(TAG,"====================At first, path = "+path);
		if(path.startsWith("/storage/")){
			prefixLen = "/storage/".length();
			pathIdx = fullPath.indexOf('/', prefixLen);	//for removing /storage/sda1 
			nextPathIdx = fullPath.indexOf("/", pathIdx+1);	//for removing /storage/sda1/accountName 
				
			if(isUserAdmin) {
				path = path.substring(pathIdx);
			}else{
				path = path.substring(nextPathIdx);
			}
			
			Log.d(TAG, "====================final path = "+path);
		}
		return path;
	}
	
	static String getFileNameFromPath(String path){
		int sepIdx = path.lastIndexOf("/");
		return path.substring(sepIdx+1);
	}
	
	public static String join(Object[] array, char separator, int startIndex, int endIndex) {
	    if (array == null) {
	        return null;
	    }
	    int bufSize = (endIndex - startIndex);
	    if (bufSize <= 0) {
	        return "";
	    }

	    bufSize *= ((array[startIndex] == null ? 16 : array[startIndex].toString().length()) + 1);
	    StringBuffer buf = new StringBuffer(bufSize);

	    for (int i = startIndex; i < endIndex; i++) {
	        if (i > startIndex) {
	            buf.append(separator);
	        }
	        if (array[i] != null) {
	            buf.append(array[i]);
	        }
	    }
	    return buf.toString();
	}
}
