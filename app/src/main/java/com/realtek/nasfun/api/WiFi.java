package com.realtek.nasfun.api;


public class WiFi {
	 private String frequency = null;
	 private String ssid = null;
	 private int signal;
	 private String quality = null;
	 private String security = null;
	 private String channel = null;
	 
	@Override
	public String toString() {
		return "WiFi [ ssid=" + ssid + ", frequency=" + frequency 
				+ ", signal=" + Integer.toString(signal) + ", quality=" + quality
				+ ", security=" + security + ", channel="+channel +"]";
	}
	 
	public String getSSID() {
		return ssid;
	}
	
	public void setSSID(String ssid) {
		this.ssid = ssid;
	}
	
	public String getSecurity() {
		return security;
	}
	
	
	/**
	 * only compare SSID. 
	 *  
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WiFi other = (WiFi) obj;
		
		if (ssid == null) {
			if (other.ssid != null)
				return false;
		} else if (!ssid.equals(other.ssid))
			return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((ssid == null) ? 0 : ssid.hashCode());
		return result;
	}
	
	// call child method to parse child custom tags
	void processCustomTag(String tagName,String text){
		if(tagName.equals("frequency")){
			frequency = text;
		} else if(tagName.equals("ssid")) {
			ssid = text;
		} else if(tagName.equals("signal")) {
			signal = Integer.parseInt(text);
		} else if(tagName.equals("quality")){
			quality = text;
		} else if(tagName.equals("security")) {
			security = text;
		} else if(tagName.equals("channel")) { 	
			channel = text;
		}
	}
	 
}