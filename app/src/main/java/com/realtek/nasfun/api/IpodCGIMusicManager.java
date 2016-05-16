package com.realtek.nasfun.api;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
// modified by silver
//import com.realtek.nasfun.R;
import android.util.Log;

import com.transcend.nas.R;

public class IpodCGIMusicManager extends MusicManager{
	private final String TAG = getClass().getSimpleName();
	
	public IpodCGIMusicManager(Server server){
		super(server);
	}
	
	/*
	public boolean addAlbumToPlaylist(String album){
		int lastIdx = playList.size();
		List<MediaItemSong> result = findSongByAlbum(album);
		return playList.addAll(lastIdx, result);
	}
	
	public boolean addArtistToPlaylist(String artist){
		int lastIdx = playList.size();
		List<MediaItemSong> result = findSongByArtist(artist);
		return playList.addAll(lastIdx, result);
	}
	*/
	
	public List<MediaItemSong> getArtists(){
		/*
		 * Response example: 
		 * "id": "hdds|audf|art|Calories Blah Blah|0",
		 * "name": "Calories Blah Blah",
		 * "type": "folder",
		 * "thumbtype": "folder"
		 */
		ArrayList<MediaItemSong> list = null;
		CGIResponse response = server.sendCGICommand("hdds|audart");
		if(response != null){
			list = new ArrayList<MediaItemSong>(response.getChildren().size());
			for(CGIResponse.Item item: response.getChildren()){
				if("folder".equals(item.getType())){
					StringTokenizer strToken = new StringTokenizer(item.getName(), "$");
					String name = null, ext=null;
					if(strToken.hasMoreTokens())
						name = strToken.nextToken();
					if(strToken.hasMoreTokens())
						ext = strToken.nextToken();
					//MediaItemSong mi = new MediaItemSong(R.drawable.icon_list_audio_singer, name, "1", false);
					// modified by silver
					MediaItemSong mi = new MediaItemSong(R.drawable.ic_logo_transcend, name, "1", false);
					if(ext != null && !"none".equals(ext)){
						mi.setThumbnail(getThumbnailPath(name, ext));
					}
					mi.setId(name);
					list.add(mi);
				}
			}
		}
		return list;
	}
	
	public List<MediaItemSong> getAlbums(){
		/*
		 * Response example:
		 * "id": "hdds|audf|alb|Coffee Break|0",
		 * "name": "Coffee Break",
		 * "type": "folder",
		 * "thumbtype": "folder"
		 */
		ArrayList<MediaItemSong> list = null;
		CGIResponse response = server.sendCGICommand("hdds|audalb");
		if(response != null){
			list = new ArrayList<MediaItemSong>(response.getChildren().size());
			for(CGIResponse.Item item: response.getChildren()){
				if("folder".equals(item.getType())){
					StringTokenizer strToken = new StringTokenizer(item.getName(), "$");
					String name = null, ext=null;
					if(strToken.hasMoreTokens())
						name = strToken.nextToken();
					if(strToken.hasMoreTokens())
						ext = strToken.nextToken();

					//MediaItemSong mi = new MediaItemSong(R.drawable.icon_list_audio_album, name, null, false);
					// modified by silver
					MediaItemSong mi = new MediaItemSong(R.drawable.ic_logo_transcend, name, null, false);
					if(ext != null && !"none".equals(ext)){
						mi.setThumbnail(getThumbnailPath(name, ext));
					}
					mi.setId(name);
					list.add(mi);
				}
			}
		}
		return list;
	}
	
	public List<MediaItemSong> getGenre() {
		ArrayList<MediaItemSong> list = null;
		return list;
	}
	
	/**
	 * 
	 * @return
	 */
	public List<MediaItemSong> getRecentSong(){
		CGIResponse response = server.sendCGICommand("hdds|audf|new");
		return getSongFromResponse(response);
	}
	
	public MediaQueryResult getRecentSong(int idx, int count){
		CGIResponse response;
		int resultCount = 0;
		MediaQueryResult result = new MediaQueryResult();
		result.index = idx;
		result.items = new ArrayList<MediaItem>(count);
		
		do{
			// iPodCGI only return max 10 records currently
			response = server.sendCGICommand("hdds|audf|new", idx, count);
			result.totalCount = response.getTotalNumberOfChildren();
			resultCount = appendSongFromResponse(response, result.items);
			if(idx >= result.totalCount || idx+count > result.totalCount){
				// wrong input parameters
				Log.w(TAG, "Wrong index or count parameters(idx="+idx+", count="+count+", total="+result.totalCount);
				break;
			}
			
			count -= resultCount;
			idx += resultCount;
		} while(count > 0);
		
		result.index = idx;
		return result;
	}
	
	public List<MediaItemSong> findSongByArtist(String artist) {
		CGIResponse response = server.sendCGICommand("hdds|audf|art|"+ artist);
		return getSongFromResponse(response);
	}
	
	public List<MediaItemSong> findSongByAlbum(String album) {
		CGIResponse response = server.sendCGICommand("hdds|audf|alb|"+ album);
		return getSongFromResponse(response);
	}
	
	private List<MediaItemSong> getSongFromResponse(CGIResponse response){
		/*
		 * Response example:
		 * "id": "hdds|audf|new|/tmp/usbmounts/sdb1/admin/Music/Savage_Garden_44.1khz_vbr/44.1khz_vbr_01-savage_garden-i_want_you.mp3|1",
		 * "name": "I Want You$Savage Garden$Truly Madly Completely$2007",
		 * "type": "audio",
		 * "thumbtype": "audio"
		 */
		ArrayList<MediaItemSong> list = null;
		if(response != null){
			list = new ArrayList<MediaItemSong>(response.getChildren().size());
			for(CGIResponse.Item item: response.getChildren()){
				if("audio".equals(item.getType())) {					
					StringTokenizer strToken = new StringTokenizer(item.getName(), "$");
					String songName = null, artist=null, album=null, year=null, ext=null;
					if(strToken.hasMoreTokens())
						songName= strToken.nextToken();
					if(strToken.hasMoreTokens())
						artist = strToken.nextToken();
					if(strToken.hasMoreTokens())
						album = strToken.nextToken();
					if(strToken.hasMoreTokens())
						year = strToken.nextToken();
					if(strToken.hasMoreTokens())
						ext = strToken.nextToken();
					//MediaItemSong mi = new MediaItemSong(R.drawable.icon_list_audio_song, songName, album, false);
					// modified by silver
					MediaItemSong mi = new MediaItemSong(R.drawable.ic_logo_transcend, songName, album, false);
					String path = Util.getPathFromID(item.getId(), server.isUserAdmin());
					path = getWebPath(path);
					if(ext != null && !"none".equals(ext)){
						mi.setThumbnail(getThumbnailPath(album, ext));
					}
					mi.setName(songName);
					mi.setArtist(artist);
					mi.setAlbum(album);
					mi.setYear(year);
					mi.setPath(path);
					list.add(mi);
				}
			}
		}
		return list;
	}
	
	private int appendSongFromResponse(CGIResponse response, List<MediaItem> list) {
		int count = 0;
		if(response != null){
			/*
			 * Response example:
			 * "id": "hdds|audf|new|/tmp/usbmounts/sdb1/admin/Music/Savage_Garden_44.1khz_vbr/44.1khz_vbr_01-savage_garden-i_want_you.mp3|1",
			 * "name": "I Want You$Savage Garden$Truly Madly Completely$2007",
			 * "type": "audio",
			 * "thumbtype": "audio"
			 */
			for(CGIResponse.Item item: response.getChildren()){
				if("audio".equals(item.getType())) {					
					StringTokenizer strToken = new StringTokenizer(item.getName(), "$");
					String songName = null, artist=null, album=null, year=null, ext=null;
					if(strToken.hasMoreTokens())
						songName= strToken.nextToken();
					if(strToken.hasMoreTokens())
						artist = strToken.nextToken();
					if(strToken.hasMoreTokens())
						album = strToken.nextToken();
					//if(strToken.hasMoreTokens())
//						year = strToken.nextToken();
					if(strToken.hasMoreTokens())
						ext = strToken.nextToken();
					//MediaItemSong mi = new MediaItemSong(R.drawable.icon_list_audio_song, songName, album, false);
					// modified by silver
					MediaItemSong mi = new MediaItemSong(R.drawable.ic_logo_transcend, songName, album, false);
					String path = Util.getPathFromID(item.getId(), server.isUserAdmin());
					path = getWebPath(path);
					if(ext != null && !"none".equals(ext)){
						mi.setThumbnail(getThumbnailPath(album, ext));
					}
					mi.setName(songName);
					mi.setArtist(artist);
					mi.setAlbum(album);
					mi.setYear(year);
					mi.setPath(path);
					mi.setId(path);
					list.add(mi);
					count++;
				}
			}
		}
		return count;
	}
	
	/**
	 * 
	 * @param relativePath begin with "/"
	 * @return full path of URL	for playback
	 */
	private String getWebPath(String relativePath){
		try {
			relativePath = URLEncoder.encode(relativePath, "UTF-8");
			relativePath = relativePath.replace("+", "%20");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String davHome = server.getDavHome();
		return "http://"+server.hostname+davHome+relativePath+"?hash="+server.hash+"&login="+server.username;
	}
	
	/**
	 * 
	 * @param artist name or album name
	 * @param ext
	 * @return music album cover's path
	 */
	private String getThumbnailPath(String name, String ext){
		return "http://"+server.hostname+"/dav/.MusicThumb/"+name.replace('/',  ';')+"."+ext;
	}
}
