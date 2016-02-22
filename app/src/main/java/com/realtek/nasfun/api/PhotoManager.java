package com.realtek.nasfun.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;

import com.transcend.nas.R;

public abstract class PhotoManager {
	protected Server server;
	protected String TAG;

	PhotoManager(Server server){
		this.server = server;
	}
	
	public abstract List<MediaItemPhoto> getRecentPhotos();
	
	public List<MediaItemPhoto> getAlbums(){
		ArrayList<MediaItemPhoto> list = new ArrayList<MediaItemPhoto>();
		try {
			/* Example response (WebDAV)
			 * <D:multistatus xmlns:D="DAV:" xmlns:R="works" xmlns:dt="urn:uuid:c2f41010-65b3-11d1-a29f-00aa00c14882/">
			 *   <D:response>
			 *     <D:propstat>
			 *       <D:prop>
			 *         <D:displayname>MyPhoto</D:displayname>
			 *         <D:getcontenttype>httpd/unix-directory</D:getcontenttype>
			 *         <D:getcontentlength>0</D:getcontentlength>
			 *         <D:creationdate dt:dt="dateTime.tz">2013-11-27T11:51:56Z</D:creationdate>
			 *         <D:getlastmodified dt:dt="dateTime.rfc1123">Wed, 27 Nov 2013 11:52:21 GMT</D:getlastmodified>
			 *         <R:cover>
			 *           dav/album/YWJjZGVmZ2hpamtsbW5vcHFyNA%3D%3D.png?&session=e992d500f97a38e63ac8e842963e91b07b95375c
			 *         </R:cover>
			 *       </D:prop>
			 *     </D:propstat>
			 *     <D:responsedescription>My Travel Photos</D:responsedescription>
			 *   </D:response>
			 * </D:multistatus>
			 */
			String commandURL = "http://" + server.hostname + "/nas/get/album/owner";
			Log.d(TAG, "Post "+commandURL);
			HttpPost httpPost = new HttpPost(commandURL);

			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("hash", server.hash));
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			DefaultHttpClient httpClient = HttpClientManager.getClient();
			HttpResponse httpResponse = httpClient.execute(httpPost);
			HttpEntity httpEntity = httpResponse.getEntity();
			InputStream inputStream = httpEntity.getContent();
			String inputEncoding = EntityUtils.getContentCharSet(httpEntity);
			if (inputEncoding == null) {
				inputEncoding = HTTP.DEFAULT_CONTENT_CHARSET;
			}
			
			try {
				XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
				factory.setNamespaceAware(true);
				XmlPullParser xpp = factory.newPullParser();
				xpp.setInput(inputStream, inputEncoding);
				int eventType = xpp.getEventType();
				String curTagName = null;
				String text = null;
				String albumName = null, 
						createData = null, 
						count = null, 
						desc = null,
						cover = null;
				
				do {
					String tagName = xpp.getName();
					if(eventType == XmlPullParser.START_TAG) {
						curTagName = tagName;
					}
					else if(eventType == XmlPullParser.TEXT) {
						if(curTagName != null){
							text = xpp.getText();
							if(curTagName.equals("displayname")) {
								albumName = text;
							} else if(curTagName.equals("creationdate")){
								createData = text;
							} else if(curTagName.equals("getcontentlength")) {
								count = text;
							} else if(curTagName.equals("responsedescription")) {
								desc = text;
							} else if(curTagName.equals("cover")) {
								cover = text;
							}
						}
					}
					else if(eventType == XmlPullParser.END_TAG) {
                        if("response".equals(xpp.getName()) && albumName != null){
                        	//MediaItemPhoto mi = new MediaItemPhoto(R.drawable.icon_list_photo_album, albumName, desc, false);
							// modified by silver
							MediaItemPhoto mi = new MediaItemPhoto(R.drawable.ic_logo, albumName, desc, false);
							if(cover!=null) {
								cover = "http://"+server.hostname+ cover + "&login="+server.username; 
								mi.setPath(cover);
							}
							mi.setId(albumName);
							list.add(mi);
							
							albumName = null;
							createData = null;
							count = null;
							desc = null;
							cover = null;
						}
						curTagName = null;
					}
					eventType = xpp.next();
					
				} while (eventType != XmlPullParser.END_DOCUMENT);
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (UnsupportedEncodingException e){
			e.printStackTrace();
		} catch (ClientProtocolException e){
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		} catch(IllegalArgumentException e){
			e.printStackTrace();
		}

		return list;
	}

	public List<MediaItemPhoto> getAlbumsPhotos(String album){
		ArrayList<MediaItemPhoto> list = new ArrayList<MediaItemPhoto>();
		try {
			album = URLEncoder.encode(album, "utf-8");
			album = album.replace("+","%20");
			String commandURL = "http://" + server.hostname + "/" +
				album + "_" + server.username +	"_" + server.hash + ".rss";
			
			Log.d(TAG, "Get "+commandURL);
			HttpGet httpGet = new HttpGet(commandURL);
			DefaultHttpClient httpClient = HttpClientManager.getClient();
			HttpResponse httpResponse = httpClient.execute(httpGet);
			HttpEntity httpEntity = httpResponse.getEntity();
			InputStream inputStream = httpEntity.getContent();
			String inputEncoding = EntityUtils.getContentCharSet(httpEntity);
			if (inputEncoding == null) {
				inputEncoding = HTTP.DEFAULT_CONTENT_CHARSET;
			}

			try {
				/*
				 * Example response (Media RSS)
				 * <rss xmlns:media="http://search.yahoo.com/mrss/" xmlns:atom="http://www.w3.org/2005/Atom" version="2.0">
				 * 	<channel>
				 * 		<item>
				 * 			<title>558904_219036741603297_429310410_n.jpg</title>
				 * 			<link>dav/album/YWJjZGVmZ2hpamtsbW5vcHFyMg==.jpg?session=e992d500f97a38e63ac8e842963e91b07b95375c</link>
				 * 			<media:thumbnail type="image/jpeg" width="128" height="96" url="dav/album/YWJjZGVmZ2hpamtsbW5vcHFyMg==.jpg?session=e992d500f97a38e63ac8e842963e91b07b95375c&thumbnail"/>
				 * 			<media:content url="dav/album/YWJjZGVmZ2hpamtsbW5vcHFyMg==.jpg?session=e992d500f97a38e63ac8e842963e91b07b95375c&webview" type="image/jpeg" width="800" height="600"/>
				 * 			<date>13-11-14</date>
				 * 			<size>186902</size>
				 * 		</item>
				 * 	</channel>
				 * </rss>
				 */
				XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
				factory.setNamespaceAware(true);
				XmlPullParser xpp = factory.newPullParser();
				xpp.setInput(inputStream, inputEncoding);
				int eventType = xpp.getEventType();
				String curTagName = null;
				String text = null;
				String title=null, date=null, size=null, link=null;
								
				do {
					String tagName = xpp.getName();
					if(eventType == XmlPullParser.START_TAG){
						curTagName = tagName;
					}else if(eventType == XmlPullParser.TEXT){
						if(curTagName != null){
							text = xpp.getText();
							if(curTagName.equals("title")){
								title = text;
							} else if(curTagName.equals("link")){
								link = text;
							} else if(curTagName.equals("date")){
								date = text;
							} else if(curTagName.equals("size")){
								size = text;
							}
						}
					}else if(eventType == XmlPullParser.END_TAG){
						curTagName = null;
						if("item".equals(tagName) && title != null){
							//MediaItemPhoto mi = new MediaItemPhoto(R.drawable.icon_photo, title, null, false);
							// modified by silver
							MediaItemPhoto mi = new MediaItemPhoto(R.drawable.ic_logo, title, null, false);
							link = "http://"+server.hostname+"/"+link + "&login="+server.username;
							mi.setId(title);
							mi.setPath(link);
							mi.setAlbum(album);
							list.add(mi);
							title=null;
							date=null;
							size=null;
							link=null;
						}
					}
					eventType = xpp.next();
					
				} while (eventType != XmlPullParser.END_DOCUMENT);
			} catch (XmlPullParserException e){
				e.printStackTrace();
			} catch (IOException e){
				e.printStackTrace();
			}
		} catch (UnsupportedEncodingException e){
			e.printStackTrace();
		} catch (ClientProtocolException e){
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		} catch(IllegalArgumentException e){
			e.printStackTrace();
		}

		return list;
	}

	public boolean addAlbum(String album) {
		boolean result = false;
		String commandURL = "http://"+server.hostname+"/nas/add/album";
		Log.d(TAG, "Post "+commandURL);

		try {
			HttpPost httpPost = new HttpPost(commandURL);
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("hash", server.hash));
			nameValuePairs.add(new BasicNameValuePair("name", album));
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
			DefaultHttpClient httpClient = HttpClientManager.getClient();
			HttpResponse httpResponse = httpClient.execute(httpPost);
			HttpEntity rspEntity = httpResponse.getEntity();
			//InputStream inputStream = rspEntity.getContent();
			String inputEncoding = EntityUtils.getContentCharSet(rspEntity);
			if (inputEncoding == null) {
				inputEncoding = HTTP.UTF_8;
			}
			String rspString = EntityUtils.toString(rspEntity, inputEncoding);
			if(rspString.contains("add album")){
				result = true;
			}
		} catch (UnsupportedEncodingException e){
			e.printStackTrace();
		} catch (ClientProtocolException e){
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch(IllegalArgumentException e){
			e.printStackTrace();
		}
		Log.d(TAG, "addAlbum result="+result);
		return result;
	}

	/**
	 * POST
	 * http://172.26.5.174/nas/del/item?hash=fc8ce534d9df5840f7b7cd451a9ec5690d7e442f
	 * path:/dav/album/YWJjZGVmZ2hpamtsbW5vcHFyMQ==.jpg
	 * name:AllPhotos
	 * Response:
	 *  <?xml version="1.0" encoding="utf-8"?><nas><album>delete items</album></nas>
	 * @return
	 */
	public boolean delItem(MediaItemPhoto item){
		boolean result = false;
		String commandURL = "http://"+server.hostname+"/nas/del/item?hash="+server.hash;
		Log.d(TAG, "Post "+commandURL);
		
		int pathBegin = item.getPath().indexOf('/', "http://".length());
		int pathEnd = item.getPath().indexOf("?");
		String path = item.getPath().substring(pathBegin, pathEnd);
		try {
			HttpPost httpPost = new HttpPost(commandURL);
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			//nameValuePairs.add(new BasicNameValuePair("hash", server.hash));
			nameValuePairs.add(new BasicNameValuePair("path", path));
			nameValuePairs.add(new BasicNameValuePair("name", item.getAlbum()));
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
			DefaultHttpClient httpClient = HttpClientManager.getClient();
			HttpResponse httpResponse = httpClient.execute(httpPost);
			HttpEntity rspEntity = httpResponse.getEntity();
			//InputStream inputStream = rspEntity.getContent();
			String inputEncoding = EntityUtils.getContentCharSet(rspEntity);
			if (inputEncoding == null) {
				inputEncoding = HTTP.UTF_8;
			}
			String rspString = EntityUtils.toString(rspEntity, inputEncoding);
			if(rspString.contains("delete items")){
				result = true;
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}catch(IllegalArgumentException e){
			e.printStackTrace();
		}

		Log.d(TAG, "delItem result="+result);
		return result;
	}
	
	/**
	 * POST
	 * http://172.26.5.174/nas/del/album?hash=fc8ce534d9df5840f7b7cd451a9ec5690d7e442f
	 * name:markmark
	 *  
	 * Response:
	 * <?xml version="1.0" encoding="utf-8"?><nas><album>delete album</album></nas>
	 * @param album
	 * @return
	 */
	public boolean delAlbum(String album) {
		boolean result = false;
		String commandURL = "http://"+server.hostname+"/nas/del/album";
		Log.d(TAG, "Post "+commandURL);

		try {
			HttpPost httpPost = new HttpPost(commandURL);
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("hash", server.hash));
			nameValuePairs.add(new BasicNameValuePair("name", album));
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
			DefaultHttpClient httpClient = HttpClientManager.getClient();
			HttpResponse httpResponse = httpClient.execute(httpPost);
			HttpEntity rspEntity = httpResponse.getEntity();
			//InputStream inputStream = rspEntity.getContent();
			String inputEncoding = EntityUtils.getContentCharSet(rspEntity);
			if (inputEncoding == null) {
				inputEncoding = HTTP.UTF_8;
			}
			String rspString = EntityUtils.toString(rspEntity, inputEncoding);
			if(rspString.contains("delete album")){
				result = true;
			}
		} catch(UnsupportedEncodingException e){
			e.printStackTrace();
		} catch(ClientProtocolException e){
			e.printStackTrace();
		} catch(IOException e){
			e.printStackTrace();
		} catch(IllegalArgumentException e){
			e.printStackTrace();
		}
		Log.d(TAG, "delAlbum result="+result);
		return result;
	}
}
