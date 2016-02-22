package com.realtek.nasfun.api;

import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class ServerInfo {
	public String registrationName;
	public String hostName;
	public String workgroupName;
	public String ipAddress;
	public String mac;
	public String firmwareVer;
	public String kernelVer;
	public String bootcodeVer;
	public String audioFirmwareVer;
	public String videoFirmwareVer;
	public String rootApVer;
	public String hardware;
	public boolean isError = false;
	public String reason = null;
	
	void parse(InputStream inputStream, String encoding){
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
			XmlPullParser xpp = factory.newPullParser();
			xpp.setInput(inputStream, encoding);
			int eventType = xpp.getEventType();
			String curTagName = null;
			String text = null;
			do {
				String tagName = xpp.getName();
				if(eventType == XmlPullParser.START_TAG) {
					curTagName = tagName;
					if(curTagName.equals("error")){
						isError = true;
					}
				}
				else if(eventType == XmlPullParser.TEXT) {
					if(curTagName != null){
						text = xpp.getText();
						if(curTagName.equals("kernel")){
							kernelVer = text;
						} else if(curTagName.equals("hostname")){
							hostName = text;
						} else if(curTagName.equals("workgroup")){
							workgroupName = text;
						} else if(curTagName.equals("bootcode_ver")){
							bootcodeVer = text;
						} else if(curTagName.equals("openvpn")){
						} else if(curTagName.equals("register")){
							registrationName = text;
						} else if(curTagName.equals("ipaddr")){
							ipAddress = text;
						} else if(curTagName.equals("audio_fw_ver")){
							audioFirmwareVer = text;
						} else if(curTagName.equals("hardware")){
							hardware = text;
						} else if(curTagName.equals("date")){
							// no need this field
						} else if(curTagName.equals("video_fw_ver")){
							videoFirmwareVer = text;
						} else if(curTagName.equals("hwaddr")){
							mac = text;
						} else if(curTagName.equals("rootap_ver")){
							rootApVer = text;
						} else if(curTagName.equals("software")){
							firmwareVer= text;
						} else if(curTagName.equals("reason")){
							reason = text;
						}
					}
				}
				else if(eventType == XmlPullParser.END_TAG) {
					curTagName = null;
				}
				eventType = xpp.next();
				
			} while (eventType != XmlPullParser.END_DOCUMENT);
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public String toString() {
		return "ServerInfo [registrationName=" + registrationName
				+ ", hostName=" + hostName + ", workgroupName=" + workgroupName
				+ ", ipAddress=" + ipAddress + ", mac=" + mac
				+ ", firmwareVer=" + firmwareVer + ", kernelVer=" + kernelVer
				+ ", bootcodeVer=" + bootcodeVer + ", audioFirmwareVer="
				+ audioFirmwareVer + ", videoFirmwareVer=" + videoFirmwareVer
				+ ", rootApVer=" + rootApVer + ", hardware=" + hardware + "]";
	}
}
