package com.realtek.nasfun.api;

import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;


public class LoginInfo {
    //login
    public String username;
    //error
    public boolean isError = false;
    public String reason = null;

    /**
       *  Response:
       *    I. login successfully
       *    format 1 (before  20150819)
       *    <nas><info>...</info></nas>
       *    format 2 (after 20150819)
       *    <nas><login>username</login></nas>:
       *
     *      II. login unsuccessfully
       *    <nas><error><reason><detail/></error></nas>
       *
       */
    void parse(InputStream inputStream, String encoding){
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(inputStream, encoding);
            int eventType = xpp.getEventType();
            boolean isNasAsParent = false;
            String parentTagName = null;
            String curTagName = null;
            String text = null;
            do {
                String tagName = xpp.getName();
                if(eventType == XmlPullParser.START_TAG) {
                    curTagName = tagName;

                    if(isNasAsParent && curTagName.equals("error")) {
                        isError = true;
                    } else if(isNasAsParent && curTagName.equals("info")) {
                        break;
                    } else if(curTagName.equals("login")){
                        break;
                    }

                    if(curTagName.equals("nas")) {
                        isNasAsParent = true;
                    } else {
                        isNasAsParent = false;
                    }
                } else if(eventType == XmlPullParser.TEXT) {
                    if(curTagName != null) {
                        text = xpp.getText();

                        if (isError && curTagName.equals("reason")) {
                            reason = text;
                        } else if(curTagName.equals("login")){
                            username = text;
                        }
                    }
                } else if(eventType == XmlPullParser.END_TAG) {
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
        return "LoginInfo [isError=" + isError + ", reason=" + reason + ", username=" + username + "]";
    }
}