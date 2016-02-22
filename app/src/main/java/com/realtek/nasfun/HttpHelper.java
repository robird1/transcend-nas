package com.realtek.nasfun;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.util.Log;

public class HttpHelper {

	private final String tag = getClass().getSimpleName();

	public static String set_samba = "/nas/set/samba";
	public static String set_ftp = "/nas/set/ftp";
	public static String set_camera = "/nas/set/ipcam";
	public static String get_samba = "/nas/get/samba";
			
	private static Document getDomElement(String xml) {
		Document document = null;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();

			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(xml));
			document = db.parse(is);
		}
		catch (ParserConfigurationException e) {
			Log.e("Error: ", e.getMessage());
			return null;
		}
		catch (SAXException e) {
			Log.e("Error: ", e.getMessage());
			return null;
		}
		catch (IOException e) {
			Log.e("Error: ", e.getMessage());
			return null;
		}
		// return DOM
		return document;
	}

	private static final String getElementValue( Node elem ) {
		Node child;
		if( elem != null){
			if (elem.hasChildNodes()){
				for( child = elem.getFirstChild(); child != null; child = child.getNextSibling() ){
					if( child.getNodeType() == Node.TEXT_NODE  ){
						return child.getNodeValue();
					}
				}
			}
		}
		return "";
	}

	public static String byteArrayToHexString(byte[] bytes)
	{
		StringBuffer buffer = new StringBuffer();
		for(int i=0; i<bytes.length; i++)
		{
			if((bytes[i] & 0xff) < 0x10) {
				buffer.append("0");
			}
			buffer.append(Long.toString(bytes[i] & 0xff, 16));
		}
		return buffer.toString();
	}

	public static byte[] hexStringToByteArray(String str) {
		int len = str.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte)((Character.digit(str.charAt(i), 16) << 4)
					+ Character.digit(str.charAt(i+1), 16));
		}
		return data;
	}
		
	public static boolean setServiceEnable(String server, String path, String hash, String enable, String play) {
		String xml = null;

		try {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost("http://" + server + path);

			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("hash", hash));
			nameValuePairs.add(new BasicNameValuePair("enable", enable));
			if (path.equals(set_samba)) {
				nameValuePairs.add(new BasicNameValuePair("workgroup", "WORKGROUP"));
				nameValuePairs.add(new BasicNameValuePair("directory", "/home"));
			} else if (path.equals(set_ftp)) {
				nameValuePairs.add(new BasicNameValuePair("directory", "/home"));
				nameValuePairs.add(new BasicNameValuePair("port", "21"));
			} else if (path.equals(set_camera)) {
				if (play != null && (play.equalsIgnoreCase("yes") || play.equalsIgnoreCase("no"))) {
					nameValuePairs.add(new BasicNameValuePair("play", play));
				} else {
					nameValuePairs.add(new BasicNameValuePair("resolution", "480p"));
					nameValuePairs.add(new BasicNameValuePair("port", "8100"));
				}
			}
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			HttpResponse httpResponse = httpClient.execute(httpPost);
			HttpEntity httpEntity = httpResponse.getEntity();
			xml = EntityUtils.toString(httpEntity);
		} catch (UnsupportedEncodingException e){
			Log.d("setServiceEnable", "catch UnsupportedEncodingException");
		} catch (ClientProtocolException e){
			Log.d("setServiceEnable", "catch ClientProtocolException");
		} catch (IOException e){
			Log.d("setServiceEnable", "catch IOException");
		} catch(IllegalArgumentException e){
			Log.d("setServiceEnable", "catch IllegalArgumentException");
		}

		Document document = getDomElement(xml);
		Element root = document.getDocumentElement();
		root.normalize();

		NodeList running = root.getElementsByTagName("running");
		for(int i = 0; i < running.getLength(); i++) {
			Element entry = (Element)running.item(i);
			String txt = entry.getFirstChild().getNodeValue();
			if (txt.equals(enable)) {
				return true;
			}
		}

		NodeList enable2 = root.getElementsByTagName("enable");
		for(int i = 0; i < enable2.getLength(); i++) {
			Element entry = (Element)enable2.item(i);
			String txt = entry.getFirstChild().getNodeValue();
			if (txt.equals(enable)) {
				return true;
			}
		}

		NodeList recorder = root.getElementsByTagName("recorder");
		for(int i = 0; i < recorder.getLength(); i++) {
			Element entry = (Element)recorder.item(i);
			String txt = entry.getFirstChild().getNodeValue();
			if (txt.equals(play)) {
				return true;
			}
		}

		return false;
	}

	
	public static String setStreaming(String server, String folder, String file, String id, String redirect) {
		String result = "";
		String url = "http://" + server + "/streaming.cgi?";

		List<NameValuePair> params = new LinkedList<NameValuePair>();
		params.add(new BasicNameValuePair("folder", folder));
		params.add(new BasicNameValuePair("file", file));
		params.add(new BasicNameValuePair("id", id));
		params.add(new BasicNameValuePair("redirect", redirect));
		String paramString = URLEncodedUtils.format(params, HTTP.DEFAULT_CONTENT_CHARSET);
		url += paramString;

		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(url);
		HttpResponse response;
		try {
			response = httpclient.execute(httpGet);

			int statusCode = response.getStatusLine().getStatusCode();
			System.out.println("statusCode = " + statusCode);
			if (HttpStatus.SC_OK == statusCode) {
				// TODO: handle 200 OK
				System.out.println(response.getStatusLine());

				Header[] headers = response.getAllHeaders();
				for (int i = 0; i < headers.length; i ++) {
					//System.out.println(headers[i].getName());
					System.out.println(headers[i].toString());
				}

				HttpEntity httpEntity = response.getEntity();
				String entityString = EntityUtils.toString(httpEntity);

				if (entityString.indexOf("Success!") >= 0) {
					Integer linkIndex = entityString.indexOf("streaming link:");
					if (linkIndex >= 0) {
						Integer httpIndex = entityString.indexOf("http://", linkIndex);
						if (httpIndex >= 0) {
							Integer brIndex = entityString.indexOf("<br>", httpIndex);
							if (brIndex >= 0) {
								result = entityString.substring(httpIndex, brIndex);
							}
						}
					}
				}
			}
			else if (HttpStatus.SC_NOT_FOUND == statusCode) {
				// TODO: handle 404 Not Found
			}
			else {
				// TODO: handle other codes here
			}
			/*
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				InputStream instream = entity.getContent();
				//String result= convertStreamToString(instream);
				instream.close();
			}
			*/
		}
		catch (ClientProtocolException e) {
			Log.d("setStreaming", "got ClientProtocolException");
		} catch (IOException e) {
			Log.d("setStreaming", "got IOException");
		} catch(IllegalArgumentException e){
			Log.d("setStreaming", "catch IllegalArgumentException");
		}

		return result;
	}
}
