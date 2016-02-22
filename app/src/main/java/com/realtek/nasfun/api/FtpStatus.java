package com.realtek.nasfun.api;

import java.util.List;

/**
 * ftpd
 * <nas>
 * <enable>yes</enable>
 * <maxperip>2</maxperip>
 * <running>yes</running>
 * <maxclients>5</maxclients>
 * <anonymous_enable>no</anonymous_enable>
 * <directory>/home/admin</directory>
 * <port>21</port>
 * </nas>
 * 
 * @author mark.yang
 *
 */
public class FtpStatus extends ServiceStatus{
	public int maxPerIP;
	public int maxClients;
	public boolean enableAnonymous;
	public String directory;
	public int port;
	
	@Override
	List<Property> getProperties() {
		List<Property> list = super.getProperties();
		list.add(new Property("anonymous_enable", enableAnonymous ? "yes":"no"));
		list.add(new Property("port", String.valueOf(port)));
		list.add(new Property("maxClients", String.valueOf(maxClients)));
		list.add(new Property("maxperip", String.valueOf(maxPerIP)));
		list.add(new Property("write_enable", "yes"));
		list.add(new Property("directory", directory));
		return list;
	}
	
	@Override
	void processCustomTag(String tagName, String text) {
		if(tagName.equals("maxperip")){
			maxPerIP = Integer.parseInt(text);
		} else if(tagName.equals("maxclients")) {
			maxClients = Integer.parseInt(text);
		} else if(tagName.equals("anonymous_enable")) {
			enableAnonymous = "yes".equals(text);
		} else if(tagName.equals("directory")) {
			directory = text;
		} else if(tagName.equals("port")) {
			port = Integer.parseInt(text);
		}
		
	}
	@Override
	public String toString() {
		return "FtpStatus [maxPerIP=" + maxPerIP + ", maxClients=" + maxClients
				+ ", enableAnonymous=" + enableAnonymous + ", directory="
				+ directory + ", port=" + port + ", isRunning=" + isRunning
				+ ", isEnabled=" + isEnabled + "]";
	}
	
}
