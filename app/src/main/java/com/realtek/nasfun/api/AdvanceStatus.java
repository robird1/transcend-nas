package com.realtek.nasfun.api;

/**
 * advance
 * <nas>
 * <restorestatus>n</restorestatus>
 * <running>yes</running>
 * <enable>yes</enable>
 * </nas>
 * 
 * @author mark.yang
 *
 */
public class AdvanceStatus extends ServiceStatus {
	public boolean restoreStatus;

	@Override
	void processCustomTag(String tagName, String text) {
		if(tagName.equals("restorestatus")){
			restoreStatus = "y".equals(text);
		}
		
	}

	@Override
	public String toString() {
		return "AdvanceStatus [restoreStatus=" + restoreStatus + ", isRunning="
				+ isRunning + ", isEnabled=" + isEnabled + "]";
	}
}
