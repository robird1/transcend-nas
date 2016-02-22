package com.realtek.nasfun.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * <nas>
 * <running>yes</running>
 * <enable>yes</enable>
 * </nas>
 * @author mark.yang
 *
 */
public abstract class ServiceStatus {
	public boolean isRunning;
	public boolean isEnabled;
	public boolean isError = false;
	public String reason = null;
	
	public static class Property{
		public String name;
		public String value;
		public Property(String n, String v){
			this.name = n;
			this.value = v;
		}
	}
	
	/**
	 * getProperties is used when setting service status
	 * @return
	 */
	List<Property> getProperties(){
		ArrayList<Property> list = new ArrayList<Property>();
		list.add(new Property("enable", isEnabled ? "yes":"no"));
		return list;
	}
	
	void reset() {
		isRunning = false;
		isEnabled = false;
		isError = false;
		reason = null;
	}
	
	void parse(InputStream inputStream, String encoding){
		reset();	// reset current status 
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
					if(curTagName.equals("error"))
						isError = true;
				}
				else if(eventType == XmlPullParser.TEXT) {
					if(curTagName != null){
						text = xpp.getText();
						if(curTagName.equals("running")){
							isRunning = "yes".equals(text);
						} else if(curTagName.equals("enable")){
							isEnabled = "yes".equals(text);
						} else if(curTagName.equals("reason")){
							reason = text;
						} else {
							// call child method to parse child custom tags
							processCustomTag(curTagName, text);
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
	abstract void processCustomTag(String tagName, String text);
}
