package com.realtek.nasfun.api;

import java.util.List;


/**
 * samba
 * <nas>
 * <directory>/home/admin</directory>
 * <running>yes</running>
 * <enable>yes</enable>
 * <workgroup>WORKGROUP</workgroup>
 * </nas>
 * 
 * @author mark.yang
 *
 */
public class SambaStatus extends ServiceStatus {
	public String directory;
	public String workgroup;
	
	@Override
	List<Property> getProperties() {
		List<Property> list =  super.getProperties();
		list.add(new Property("directory", directory));
		list.add(new Property("workgroup", workgroup));
		return list;
	}

	@Override
	void processCustomTag(String tagName, String text) {
		if(tagName.equals("directory")){
			directory = text;
		} else if(tagName.equals("workgroup")){
			workgroup = text;
		}
	}

	@Override
	public String toString() {
		return "SambaStatus [directory=" + directory + ", workgroup="
				+ workgroup + ", isRunning=" + isRunning + ", isEnabled="
				+ isEnabled + "]";
	}
}
