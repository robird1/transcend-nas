package com.realtek.nasfun.api;

/**
 * dlna
 * <nas>
 * <dlna>
 * <enable>yes</enable>
 * <servername>RealtekNAS</servername>
 * <tivo>no</tivo>
 * <strict>no</strict>
 * <running>yes</running>
 * <directory>/home/admin</directory>
 * <port>8200</port>
 * </dlna>
 * </nas>
 * 
 * @author mark.yang
 *
 */
public class DlnaStatus extends ServiceStatus {
	public String servername;
	public boolean tivo;
	public boolean strict;
	public String directory;
	public int port;
	
	@Override
	void processCustomTag(String tagName, String text) {
		if(tagName.equals("servername")){
			servername = text;
		} else if(tagName.equals("tivo")) {
			tivo = "yes".equals(text);
		} else if(tagName.equals("strict")) {
			strict = "yes".equals(text);
		} else if(tagName.equals("directory")) {
			directory = text;
		} else if(tagName.equals("port")) {
			port = Integer.parseInt(text);
		}
		
	}

	@Override
	public String toString() {
		return "DlnaStatus [servername=" + servername + ", tivo=" + tivo
				+ ", strict=" + strict + ", directory=" + directory + ", port="
				+ port + ", isRunning=" + isRunning + ", isEnabled="
				+ isEnabled + "]";
	}
	
	
}
