package com.realtek.nasfun.api;

/**
 * Network
 * <nas>
 * <name>eth0</name>
 * <dns2/>
 * <dns1>172.21.1.10</dns1>
 * <hostname>RealtekNAS</hostname>
 * <ipaddr>172.26.5.174</ipaddr>
 * <netmask>255.255.255.0</netmask>
 * <hwaddr>c4:01:42:00:08:71</hwaddr>
 * <dhcp>no</dhcp>
 * <gateway>172.26.5.254</gateway>
 * </nas>
 * 
 * @author mark.yang
 *
 */
public class NetworkStatus  extends ServiceStatus {

	public String name;
	public String dns1;
	public String hostname;
	public String ipaddr;
	public String netmask;
	public String hwaddr;
	public boolean dhcp;
	public String gateway;
	
	@Override
	void processCustomTag(String tagName, String text) {
		if(tagName.equals("name")){
			name = text;
		} else if(tagName.equals("dns1")){
			dns1 = text;
		} else if(tagName.equals("hostname")) {
			hostname = text;
		} else if(tagName.equals("ipaddr")) {
			ipaddr = text;
		} else if(tagName.equals("netmask")) {
			netmask = text;
		} else if(tagName.equals("hwaddr")) {
			hwaddr = text;
		} else if(tagName.equals("dhcp")) {
			dhcp = "yes".equals(text);
		} else if(tagName.equals("gateway")) {
			gateway = text;
		}
	}

	@Override
	public String toString() {
		return "NetworkStatus [name=" + name + ", dns1=" + dns1 + ", hostname="
				+ hostname + ", ipaddr=" + ipaddr + ", netmask=" + netmask
				+ ", hwaddr=" + hwaddr + ", dhcp=" + dhcp + ", gateway="
				+ gateway + "]";
	}
	

}
