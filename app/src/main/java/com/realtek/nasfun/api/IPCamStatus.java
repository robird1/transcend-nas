package com.realtek.nasfun.api;

import java.util.List;


/**
 * ipcam
 * <nas>
 * <running>no</running>
 * <enable>no</enable>
 * <resolution>480p</resolution>
 * <recorder>no</recorder>
 * <duration>1</duration> 
 * <end_time>2013/12/24-11:35:53</end_time>
 * <port>8100</port>
 * </nas>
 * @author mark.yang
 *
 */
public class IPCamStatus extends ServiceStatus {
	public String resolution = null;
	public boolean isRecording = false;
	public int port = 8100;
	public String endTime = null;
	public int duration = 0;
	
	@Override
	List<Property> getProperties() {
		List<Property> list = super.getProperties();
		list.add(new Property("resolution", resolution));
		list.add(new Property("port", String.valueOf(port)));
		return list;
	}

	@Override
	void processCustomTag(String tagName, String text) {
		if(tagName.equals("resolution")){
			resolution = text;
		} else if(tagName.equals("recorder")) {
			isRecording = "yes".equals(text);
		} else if(tagName.equals("port")) {
			port = Integer.parseInt(text);
		} else if(tagName.equals("end_time")){
			endTime = text;
		}
		
	}

	@Override
	public String toString() {
		return "IPCamStatus [resolution=" + resolution + ", isRecording="
				+ isRecording + ", port=" + port + ", isRunning=" + isRunning
				+ ", isEnabled=" + isEnabled + "]";
	}
}
