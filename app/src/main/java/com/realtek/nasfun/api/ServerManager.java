package com.realtek.nasfun.api;

import java.util.ArrayList;
import java.util.List;

import android.content.SharedPreferences;
import android.util.Log;

/**
 * ServernManager is a singleton, can be access directly by 
 * ServerManager.INSTANCE
 * 
 * ServerManager need SharedPreferences to preserve connection server lists,
 * you need to call setPreference before calling any other methods. 
 * 
 * @author mark.yang
 *
 */
public enum ServerManager {
	INSTANCE;
	
	private static final String TAG = "ServerManager";
	
	private static final String CONNECTION_COUNT_TAG = "CONN_NUM";
	private static final String CONN_SERVER_NAME_TAG = "SERVER_NAME";
	private static final String CONN_USERNAME_TAG = "USERNAME";	
	private static final String CONN_PASSWORD_TAG = "PASSWORD";
	private static final String CONN_ENABLE_PASSWORD_SAVED_TAG = "ENABLE_PASSWORD_SAVED";
	private static final String CONN_ENABLE_FTP_TAG = "ENABLE_FTP";
	private static final String CONN_ENABLE_CAMERA_TAG = "ENABLE_CAMERA";
	private static final String CONN_ENABLE_DOWNLOADER_TAG = "ENABLE_DOWNLOADER";
	private static final String CONN_ENABLE_SOFTAP_TAG = "ENABLE_SOFTAP";
	private static final String CONN_ENABLE_ONE_CLICK_BACKUP_TAG = "ENABLE_ONE_CLICK_BACKUP";
	private static final String CONN_LAST_BACKUP_TIME_TAG = "LAST_BACKUP_TIME";
	private static final String CONN_HASH_TAG = "CONN_HASH";
	private static final String CONN_LAST_NO_TAG = "CONN_LAST_NO";
	private static final String CONN_FW_TYPE_TAG = "FW_TYPE";
	private static final String CONN_ENABLE_VIDEO_UNKNOWN_TAG = "VIDEO_UNKNOWN";
	
	private SharedPreferences pref = null;
	private ArrayList<Server> serverList = null;
	private Server currentServer = null;

	private ServerManager(){
		
	}
	
	/**
	 * Set the Android SharedPreferences for ServerManager to load/save data 
	 * 
	 * TODO: May design a preference saver/load interface and separate the logic
	 * from ServerManager (in order to run on non-android system)
	 * @param pref
	 */
	public void setPreference(SharedPreferences pref){
		this.pref = pref;
	}

	/**
	 * Set and save current connection server
	 * If auto login is enabled, this can be used to 
	 * get the previous connected server and reuse it
	 * 
	 * @param server current successful login server. Null if need to clear it
	 */
	public void setCurrentServer(Server server){
		this.currentServer = server;
		int count = serverList.size();
		int lastConnIdx = -1;
		for(int i=0; i<count; i++){
			if(serverList.get(i).equals(server)){
				lastConnIdx = i;
				break;
			}
		}
		
		// save index to preferences
		SharedPreferences.Editor editor = pref.edit();
		editor.putInt(CONN_LAST_NO_TAG, lastConnIdx);
		editor.commit();
	}
	
	/**
	 * Get current connected server.
	 * This can also be used for auto-login purpose if current server is not 
	 * cleared when leaving application. 
	 * @return
	 */
	public Server getCurrentServer(){
		if(currentServer == null){
			if(serverList == null) {
				serverList = loadServerList();
			}
			// try to load connection from list
			int lastConnIdx = pref.getInt(CONN_LAST_NO_TAG, -1);
			Log.i(TAG, "currentServer is null, try to load from server list["+lastConnIdx+"]");
			if(lastConnIdx != -1 && lastConnIdx < serverList.size()){
				currentServer = serverList.get(lastConnIdx);
			}
		}
		return currentServer;
	}
	
	/**
	 * Return persistent connection list
	 * @return
	 */
	public List<Server> getServerList(){
		if(serverList == null) {
			serverList = loadServerList();
		}
		return serverList;
	}
	
	/**
	 * Remove this connection info from list
	 * @param conn
	 */
	public void removeServer(Server conn) {
		int count = pref.getInt(CONNECTION_COUNT_TAG, 0);
		SharedPreferences.Editor editor = pref.edit();		
		
		if(conn.equals(currentServer)) 
			currentServer = null;
		
		if(serverList == null)
			serverList = loadServerList();
		
		for(int i=0; i<count; i++) {
			// found
			if(serverList.get(i).equals(conn)) {
				// to mark all preference values of a server in the editor,
				// which will be done in the actual preferences once commit() is called.
				editor.remove(CONN_SERVER_NAME_TAG+"["+i+"]");
				editor.remove(CONN_USERNAME_TAG+"["+i+"]");
				editor.remove(CONN_PASSWORD_TAG+"["+i+"]");
				editor.remove(CONN_FW_TYPE_TAG+"["+i+"]");
				editor.remove(CONN_ENABLE_VIDEO_UNKNOWN_TAG+"["+i+"]");
				editor.remove(CONN_ENABLE_PASSWORD_SAVED_TAG+"["+i+"]");
				editor.remove(CONN_ENABLE_FTP_TAG+"["+i+"]");
				editor.remove(CONN_ENABLE_CAMERA_TAG+"["+i+"]");
				editor.remove(CONN_ENABLE_DOWNLOADER_TAG+"["+i+"]");
				editor.remove(CONN_ENABLE_SOFTAP_TAG+"["+i+"]");
				editor.remove(CONN_ENABLE_ONE_CLICK_BACKUP_TAG+"["+i+"]");
				if(pref.contains(CONN_HASH_TAG+"["+i+"]"))
					editor.remove(CONN_HASH_TAG+"["+i+"]");
				if(pref.contains(CONN_LAST_BACKUP_TIME_TAG+"["+i+"]"))
					editor.remove(CONN_LAST_BACKUP_TIME_TAG+"["+i+"]");
				editor.commit();
				
				// remove from server list and save
				serverList.remove(i);
				saveServerList(serverList);
				break;
			}
		}
	}
	
	/**
	 * Remove all connection info 
	 * @param null
	 */
	public void removeAllServer() {
		int count = pref.getInt(CONNECTION_COUNT_TAG, 0);
		SharedPreferences.Editor editor = pref.edit();	
		
		currentServer = null;
		for(int i=0; i<count; i++) {
			editor.remove(CONN_SERVER_NAME_TAG+"["+i+"]");
			editor.remove(CONN_USERNAME_TAG+"["+i+"]");
			editor.remove(CONN_PASSWORD_TAG+"["+i+"]");
			editor.remove(CONN_FW_TYPE_TAG+"["+i+"]");
			editor.remove(CONN_ENABLE_VIDEO_UNKNOWN_TAG+"["+i+"]");
			editor.remove(CONN_ENABLE_FTP_TAG+"["+i+"]");
			editor.remove(CONN_ENABLE_CAMERA_TAG+"["+i+"]");
			editor.remove(CONN_ENABLE_DOWNLOADER_TAG+"["+i+"]");
			editor.remove(CONN_ENABLE_SOFTAP_TAG+"["+i+"]");
			editor.remove(CONN_ENABLE_ONE_CLICK_BACKUP_TAG+"["+i+"]");
			editor.remove(CONN_ENABLE_PASSWORD_SAVED_TAG+"["+i+"]");
			if(pref.contains(CONN_HASH_TAG+"["+i+"]"))
				editor.remove(CONN_HASH_TAG+"["+i+"]");
			if(pref.contains(CONN_LAST_BACKUP_TIME_TAG+"["+i+"]"))
				editor.remove(CONN_LAST_BACKUP_TIME_TAG+"["+i+"]");
		}	
		editor.remove(CONN_LAST_NO_TAG);
		editor.remove(CONNECTION_COUNT_TAG);
		editor.commit();
		
		serverList.clear();
	}
	
	/**
	 * Save this connection info 
	 * @param conn
	 */
	public void saveServer(Server conn){
		boolean isAdded = false;
		
		if(serverList == null) {
			serverList = loadServerList();
		}
		// Save only for new connection (different name or user name)
		int count = serverList.size();
		for(int i=0; i<count; i++) {
			Server c = serverList.get(i);
			
			// found same server and user name (maybe different password)
			if(c.equals(conn)) {
				// update item
				Log.d(TAG, "Update server:"+conn.hostname+", total:"+serverList.size());
				serverList.set(i, conn);
				isAdded = true;
				break;
			}
		}
		
		if(!isAdded) {
			// do not found same entry, add a new record
			serverList.add(conn);
			Log.d(TAG, "Add new server:"+conn.hostname+", total:"+serverList.size());
		}	
		saveServerList(serverList);
	}

	public Server findServerFromServerList(String hostname, String username) {
		Server server = null;
		Server target = new Server(hostname, username, Server.DEFAULT_PASSWORD);
		
		if(serverList == null) {
			serverList = loadServerList();
		}
		
		int count = serverList.size();
		for(int i=0; i<count; i++) {
			Server c = serverList.get(i);
			
			// found same server and user name (maybe different password)
			if(c.equals(target)) {
				server = c;
				break;
			}
		}
		return server;
	}
	
	/**
	 * Load connection list from preferences
	 */
	private ArrayList<Server> loadServerList(){
		Log.i(TAG, "Load server list");
		// first time get connection list, load it from preferences
		int count = pref.getInt(CONNECTION_COUNT_TAG, 0);
		ArrayList<Server> list = new ArrayList<Server>(count);
		for(int i=0; i<count; i++){
			String hostName = pref.getString(CONN_SERVER_NAME_TAG+"["+i+"]", null);
			String username = pref.getString(CONN_USERNAME_TAG+"["+i+"]", null);
			String password = pref.getString(CONN_PASSWORD_TAG+"["+i+"]", null);
			String hash = pref.getString(CONN_HASH_TAG+"["+i+"]", null);
			String lastBackupTime = pref.getString(CONN_LAST_BACKUP_TIME_TAG+"["+i+"]", "");
			int firmwareType = pref.getInt(CONN_FW_TYPE_TAG+"["+i+"]", 1);
			boolean isSupportedVideoUnKnown = pref.getBoolean(CONN_ENABLE_VIDEO_UNKNOWN_TAG+"["+i+"]", false);
			boolean isPasswordSaved = pref.getBoolean(CONN_ENABLE_PASSWORD_SAVED_TAG+"["+i+"]", false);

			Server conn = new Server(hostName, username, password);
			conn.hash = hash;
			conn.setLastBackupTime(lastBackupTime);
			conn.firmwareType = firmwareType;
			conn.isSupportedVideoUnKnown = isSupportedVideoUnKnown;
			conn.isPasswordSaved = isPasswordSaved;
			//set services' enable/disable
			boolean isAvailable = pref.getBoolean(CONN_ENABLE_FTP_TAG+"["+i+"]", false);
			conn.enableServiceAvail(Server.Service.FTP, isAvailable);
			isAvailable = pref.getBoolean(CONN_ENABLE_CAMERA_TAG+"["+i+"]", false);
			conn.enableServiceAvail(Server.Service.CAMERA, isAvailable);
			isAvailable = pref.getBoolean(CONN_ENABLE_DOWNLOADER_TAG+"["+i+"]", false);
			conn.enableServiceAvail(Server.Service.DOWNLOADER, isAvailable);
			isAvailable = pref.getBoolean(CONN_ENABLE_SOFTAP_TAG+"["+i+"]", false);
			conn.enableServiceAvail(Server.Service.SOFTAP, isAvailable);
			conn.isBackUPServiceAvailable = pref.getBoolean(CONN_ENABLE_ONE_CLICK_BACKUP_TAG, false);
			list.add(conn);
		}
		return list;
	}
	
	/**
	 * Save connection list into preferences
	 */
	private void saveServerList(ArrayList<Server> list){
		SharedPreferences.Editor editor = pref.edit();
		int count = list.size();
		int lastConnIdx = -1;
		editor.putInt(CONNECTION_COUNT_TAG, count);
		for(int i=0; i<count; i++){
			Server conn = list.get(i);
			editor.putString(CONN_SERVER_NAME_TAG+"["+i+"]", conn.hostname);
			editor.putString(CONN_USERNAME_TAG+"["+i+"]", conn.username);
			editor.putString(CONN_PASSWORD_TAG+"["+i+"]", conn.password);
			editor.putInt(CONN_FW_TYPE_TAG+"["+i+"]", conn.firmwareType);
			editor.putBoolean(CONN_ENABLE_VIDEO_UNKNOWN_TAG+"["+i+"]", conn.isSupportedVideoUnKnown);
 			editor.putBoolean(CONN_ENABLE_FTP_TAG+"["+i+"]", conn.isServiceAvail(Server.Service.FTP));
			editor.putBoolean(CONN_ENABLE_CAMERA_TAG+"["+i+"]", conn.isServiceAvail(Server.Service.CAMERA));
			editor.putBoolean(CONN_ENABLE_DOWNLOADER_TAG+"["+i+"]", conn.isServiceAvail(Server.Service.DOWNLOADER));
			editor.putBoolean(CONN_ENABLE_SOFTAP_TAG +"["+i+"]", conn.isServiceAvail(Server.Service.SOFTAP));
			editor.putBoolean(CONN_ENABLE_ONE_CLICK_BACKUP_TAG+"["+i+"]", conn.isBackUPServiceAvail());
			editor.putBoolean(CONN_ENABLE_PASSWORD_SAVED_TAG+"["+i+"]", conn.isPasswordSaved());

			if(conn.hash != null)
				editor.putString(CONN_HASH_TAG+"["+i+"]", conn.hash);
			if(conn.isBackUPServiceAvail())
				editor.putString(CONN_LAST_BACKUP_TIME_TAG+"["+i+"]", conn.getLastBackupTime());
			// update last connection index 
			if(conn.equals(this.currentServer)) {
				lastConnIdx = i;
			}
		}
		editor.putInt(CONN_LAST_NO_TAG, lastConnIdx);
		editor.commit();
	}
}
