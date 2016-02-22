package com.realtek.nasfun.api;

import java.util.ArrayList;
import java.util.List;

import com.realtek.nasfun.api.ServiceStatus.Property;

/**
 * 	<nas>
 *  <ssid>dir-635</ssid>
 *  <bssid>00:1b:11:ed:3d:60</bssid>
 *  <wpa_state>COMPLETED</wpa_state>
 *  <pairwise_cipher>CCMP</pairwise_cipher>
 *  <group_cipher>CCMP</group_cipher>
 *  <address>d8:eb:97:27:5a:0d</address>
 *  <key_mgmt>WPA2-PSK</key_mgmt>
 *  <ip_address>192.168.0.194</ip_address>                  
 *  <id>0</id>
 *  <mode>station</mode>
 *	</nas>
 *  @author phyllis
 *
 */

public class WiFiStatus extends ServiceStatus {
	public final static String SECURITY_OPEN = "none";
	public final static String SECURITY_WPA = "wpa";
		
	public String ssid = null;
	public String bssid = null;
	public String wpa_state = null;
	public String pairwise_cipher = null;
	public String group_cipher = null;
	public String address = null;
	public String key_mgmt = null;
	public String security = null;
	public String ip_address = null;
	public String id = null;
	public String mode = null;
	public String state = null;
	//get from user input
	public String password = null;

	@Override
	void processCustomTag(String tagName, String text) {
		if(tagName.equals("ssid")) {
			ssid = text;
		} else if(tagName.equals("bssid")) {
			bssid = text;
		} else if(tagName.equals("wpa_state")) {
			wpa_state = text;
		} else if(tagName.equals("pairwise_cipher")) {
			pairwise_cipher = text;
		} else if(tagName.equals("group_cipher")) {
			group_cipher = text;
		} else if(tagName.equals("address")) {
			address = text;
		} else if(tagName.equals("key_mgmt")) {
			key_mgmt = text;
		} else if(tagName.equals("security")) {
			security = text;
		} else if(tagName.equals("ip_address")) {
			ip_address = text;
		} else if(tagName.equals("id")) {
			id = text;
		}else if(tagName.equals("mode")) {
			mode = text;
		}else if(tagName.equals("state")) {
			state = text;
		}	
		
	}
	
	/** 
	 * 
	 */
	@Override
	void reset() {
		super.reset();
		ssid = null;
		bssid = null;
		wpa_state = null;
		pairwise_cipher = null;
		group_cipher = null;
		address = null;
		key_mgmt = null;
		security = null;
		ip_address = null;
		id = null;
		mode = null;
		state = null;
		password = null;		
	}
	
	
	@Override
	List<Property> getProperties() {
		ArrayList<Property> list = new ArrayList<Property>();
			list.add(new Property("ssid", ssid));			
			if(!WiFiStatus.SECURITY_OPEN.equals(security)) {
				list.add(new Property("security", WiFiStatus.SECURITY_WPA));
				Server server = ServerManager.INSTANCE.getCurrentServer(); 
				String encryptedPassword = Server.encryptPassword(password, server.nasModules, server.nasPublic);
				list.add(new Property("password", encryptedPassword));
			}else{
				list.add(new Property("security",	WiFiStatus.SECURITY_OPEN));
			}
		return list;
	} 
	
	@Override
	public String toString() {
		return "WiFiStatus [ssid=" + ssid +
				", bssid="+ bssid + 
				", wpa_state=" + wpa_state + 
				", pairwise_cipher="+ pairwise_cipher  +
				", group_cipher=" + group_cipher +
				", address=" + address +
				", key_mgmt=" + key_mgmt +
				", security=" + security +
				", ip_address=" + ip_address +
				", id=" + id +
				", mode=" + mode +
				"]";
	}
	
	 
	 
}