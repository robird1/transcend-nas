package com.realtek.nasfun.api;

/**
 * bt
 * <nas>
 * <username>admin</username>
 * <enable>yes</enable>
 * <password>realteknas</password>
 * <running>yes</running>
 * <port>9091</port>
 * </nas>
 * 
 * @author mark.yang
 *
 */
public class BtStatus extends ServiceStatus {
	public String username;
	public String password;
	public int port;
	
	@Override
	void processCustomTag(String tagName, String text) {
		if(tagName.equals("username")){
			username = text;
		} else if(tagName.equals("password")){
			password = text;
		} else if(tagName.equals("port")) {
			port = Integer.parseInt(text);
		}
	}

	@Override
	public String toString() {
		return "BtStatus [username=" + username + ", password=" + password
				+ ", port=" + port + ", isRunning=" + isRunning
				+ ", isEnabled=" + isEnabled + "]";
	}
}
