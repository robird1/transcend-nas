package com.realtek.nasfun.api;

import java.util.ArrayList;
import java.util.List;

/**
 * <nas>
 * <enable>yes</enable>
 * <ssid>RealtekNAS-WiFi</ssid>
 * <channels>1</channels>
 * <channels>2</channels>
 * <channels>3</channels>
 * <channels>4</channels>
 * <channels>5</channels>
 * <channels>6</channels>
 * <channels>7</channels>
 * <channels>8</channels>
 * <channels>9</channels>
 * <channels>10</channels>
 * <channels>11</channels>
 * <channels>12</channels>
 * <channels>13</channels>
 * <channels>36</channels>
 * <channels>40</channels>
 * <channels>44</channels>
 * <channels>48</channels>
 * <running>no</running>
 * <frequency>2.4G</frequency>
 * <security>none</security>
 * <wpa>0</wpa>
 * <channel>1</channel>
 * </nas>
 * 
 * @author mark.yang
 *
 */
public class SoftAPStatus extends ServiceStatus {

	public final static String FREQUENCY_2_4G = "2.4G";
	public final static String FREQUENCY_5G = "5G";
	
	public final static String CHANNEL_AUTO = "auto";
	
	public final static String SECURITY_OPEN = "none";
	public final static String SECURITY_WPA_PSK = "wpa-psk";
	public final static String SECURITY_WPA2_PSK = "wpa2-psk";
	public final static String SECURITY_MIXED_WPA = "mixed-wpa";
	//public final static String SECURITY_WEP = "wep";
	
	public String ssid;
	public String frequency;
	public String security;
	public String channel;
	public ArrayList<String> channels;
	public int wpa;
	public String password;
	public boolean isAuto = false;
	
	SoftAPStatus() {
		channels = new ArrayList<String>();
		channels.add(CHANNEL_AUTO);
	}
	
	/** 
	 * 
	 */
	@Override
	void reset() {
		super.reset();
		password = null;
		isAuto = false;
		channels.clear();
		channels.add(CHANNEL_AUTO);
	}
	
	@Override
	List<Property> getProperties() {
		List<Property> list = super.getProperties();
		
		if(isEnabled) {
			list.add(new Property("ssid", ssid));
			list.add(new Property("frequency", frequency));
			if(isAuto)
				list.add(new Property("channel", CHANNEL_AUTO));
			else
				list.add(new Property("channel", channel));
			list.add(new Property("security", security));
			if(!SECURITY_OPEN.equals(security)) {
				Server server = ServerManager.INSTANCE.getCurrentServer(); 
				String encryptedPassword = Server.encryptPassword(password, server.nasModules, server.nasPublic);
				list.add(new Property("password", encryptedPassword));
			}
		}
		return list;
	}

	@Override
	void processCustomTag(String tagName, String text) {
		if(tagName.equals("ssid")) {
			ssid = text;
		} else if(tagName.equals("channels")) {
			String ch = text;
			channels.add(ch);
		} else if(tagName.equals("frequency")) {
			frequency = text;
		} else if(tagName.equals("security")) {
			security = text;
		} else if(tagName.equals("wpa")) {
			wpa = Integer.parseInt(text);
		} else if(tagName.equals("channel")) {
			channel = text;
		} else if(tagName.equals("password")) {
			password = text;
		} else if(tagName.equals("auto")) {
			isAuto = "yes".equals(text);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "SoftAPStatus [ssid=" + ssid + ", frequency=" + frequency
				+ ", security=" + security + ", channel=" + channel
				+ ", channels=" + channels + ", wpa=" + wpa + ", password="
				+ password + ", isAuto=" + isAuto + "]";
	}
	
	

}
