package com.realtek.nasfun.api;

/**
 * daap(itunes)
 * <nas>
 * <running>no</running>
 * <servername>%h - iTunes/DAAP</servername>
 * <enable>no</enable>
 * <port>3689</port>
 * </nas>
 * @author mark.yang
 *
 */
public class DaapStatus extends ServiceStatus {
	public String servername;
	public int port;
	
	@Override
	void processCustomTag(String tagName, String text) {
		if(tagName.equals("servername")){
			servername = text;
		} else if(tagName.equals("port")) {
			port = Integer.parseInt(text);
		}
		
	}
	@Override
	public String toString() {
		return "DaapStatus [servername=" + servername + ", port=" + port
				+ ", isRunning=" + isRunning + ", isEnabled=" + isEnabled + "]";
	}
}
