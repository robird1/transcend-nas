package com.realtek.nasfun.api;

/**
 * afpd
 * <nas>
 * <running>no</running>
 * <directory>/dav/home/</directory>
 * <enable>no</enable>
 * </nas>
 *
 * @author mark.yang
 *
 */
public class AfpdStatus extends ServiceStatus {

	@Override
	void processCustomTag(String tagName, String text) {
		// TODO: need implement
	}

	@Override
	public String toString() {
		return "AfpdStatus [isRunning=" + isRunning + ", isEnabled="
				+ isEnabled + "]";
	}

}
