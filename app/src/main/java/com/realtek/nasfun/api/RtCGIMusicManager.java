package com.realtek.nasfun.api;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;
// modified by silver
//import com.realtek.nasfun.R;
import com.realtek.nasfun.api.RtCGIMusicAlbumListResponse.AlbumItem;
import com.realtek.nasfun.api.RtCGIMusicArtistListResponse.ArtistItem;
import com.realtek.nasfun.api.RtCGIMusicListResponse.SongItem;
import com.transcend.nas.R;

public class RtCGIMusicManager extends MusicManager{
	private final String TAG = getClass().getSimpleName();
	
	public RtCGIMusicManager(Server server){
		super(server);
	}
	
	public List<MediaItemSong> getAlbums(){
		String url = "music::album"+"&max=200";
		String cmd = "http://"+server.hostname+"/rtCGI.fcgi?id=3&url="+url;
		RtCGIMusicAlbumListResponse response = (RtCGIMusicAlbumListResponse)server.getRtCGIResponse(cmd, RtCGIMusicAlbumListResponse.class);
		return getAlbumFromResponse(response);
	}
	
	public List<MediaItemSong> getArtists(){
		String url = "music::artist"+"&max=200";
		String cmd = "http://"+server.hostname+"/rtCGI.fcgi?id=3&url="+url;
		RtCGIMusicArtistListResponse response = (RtCGIMusicArtistListResponse)server.getRtCGIResponse(cmd, RtCGIMusicArtistListResponse.class);
		return getArtistFromResponse(response);	
	}
	
	public List<MediaItemSong> getRecentSong(){
		String url = "music"+"&max=200";
		String cmd = "http://"+server.hostname+"/rtCGI.fcgi?id=3&url="+url;
		RtCGIMusicListResponse response = (RtCGIMusicListResponse)server.getRtCGIResponse(cmd, RtCGIMusicListResponse.class);
		return getSongFromResponse(response);
	}

	public List<MediaItemSong> findSongByArtist(String artistID){
		String url = "music&artist="+artistID+"&max=200";
		String cmd = "http://"+server.hostname+"/rtCGI.fcgi?id=3&url="+url;
		RtCGIMusicListResponse response = (RtCGIMusicListResponse)server.getRtCGIResponse(cmd, RtCGIMusicListResponse.class);
		return getSongFromResponse(response);
	}
	
	public List<MediaItemSong> findSongByAlbum(String albumID){
		String url = "music&album="+albumID+"&max=200"; 
		String cmd = "http://"+server.hostname+"/rtCGI.fcgi?id=3&url="+url;
		RtCGIMusicListResponse response = (RtCGIMusicListResponse)server.getRtCGIResponse(cmd, RtCGIMusicListResponse.class);
		return  getSongFromResponse(response);
	}
	
	private List<MediaItemSong> getSongFromResponse(RtCGIMusicListResponse response){
		ArrayList<MediaItemSong> list = new ArrayList<MediaItemSong>();
		if (response == null){
			Log.w(TAG, "getSongFromResponse(), response is null");	
		} else if (response.status != 0) {
			Log.w(TAG, "getSongFromResponse(), response="+response);
		} else {
			Log.w(TAG, "queryFile, total="+response.total);
			for(SongItem item: response.items){
				String songName = null, artist=null, album=null, thumbNailPath=null, path = null;
				songName = item.disp_name;
				album = item.album;
				artist = item.artist;
				//MediaItemSong mi = new MediaItemSong(R.drawable.icon_list_audio_song, songName, album, false);
				// modified by silver
				MediaItemSong mi = new MediaItemSong(R.drawable.ic_logo, songName, album, false);
				thumbNailPath = getThumbnailPath(album);
				Log.d(TAG,songName +"'s thumbNailPath = "+thumbNailPath);
				mi.setName(songName);
				mi.setArtist(artist);
				mi.setAlbum(album);
				mi.setThumbnail(thumbNailPath);
				//set the path for the song's playback 
				path = getPathFromId(Integer.toString(item.id));
				if(path != null){
					path = getWebPath(path);
					mi.setPath(path);
				}else{
					Log.d(TAG,"A song's id ="+item.id+ ", its relative path is null");
				}
				list.add(mi);
			}
		}
		return list;
	}
	
	private List<MediaItemSong> getAlbumFromResponse(RtCGIMusicAlbumListResponse response){
		ArrayList<MediaItemSong> list = new ArrayList<MediaItemSong>();
		if (response == null){
			Log.w(TAG, "getAlbumFromResponse(), response is null");	
		} else if (response.status != 0) {
			Log.w(TAG, "getAlbumFromResponse(), response="+response);
		} else {
			for(AlbumItem item: response.items){
				String album=null, id = null;
				album = item.album;
				id = Integer.toString(item.id);
				//MediaItemSong mi = new MediaItemSong(R.drawable.icon_list_audio_album, album, null, false);
				// modified by silver
				MediaItemSong mi = new MediaItemSong(R.drawable.ic_logo, album, null, false);
				mi.setId(id);                     
				mi.setThumbnail(getThumbnailPath(album));
				list.add(mi);
			}
		}
		return list;
	}
	
	private List<MediaItemSong> getArtistFromResponse(RtCGIMusicArtistListResponse response){
		ArrayList<MediaItemSong> list = new ArrayList<MediaItemSong>();
		if (response == null){
			Log.w(TAG, "getArtistFromResponse(), response is null");	
		} else if (response.status != 0) {
			Log.w(TAG, "getArtistFromResponse(), response="+response);
		} else {
			for(ArtistItem item: response.items){
				String artist = null, numOfAlbum = "0", id = null;
				artist = item.artist;
				numOfAlbum = Integer.toString(item.num_of_albums);
				id = Integer.toString(item.id);
				//MediaItemSong mi = new MediaItemSong(R.drawable.icon_list_audio_singer, artist, numOfAlbum, false);
				// modified by silver
				MediaItemSong mi = new MediaItemSong(R.drawable.ic_logo, artist, numOfAlbum, false);
				mi.setId(id);
				mi.setThumbnail(getThumbnailPath(artist));
				list.add(mi);
			}
		}
		return list;
	}
	
	/**
	 * 
	 * get relative path from ID
	 * 
	 * Step 1: get full path from id
	 * \/storage\/sda1\/admin\/Music\/1-01 THE TIDE IS HIGH.mp3
	 *  
	 * Step2:
	 * extract relative path from full path 
	 * @param id
	 * @return  relative path, e.g. /Music/1-01 THE TIDE IS HIGH.mp3(user)
	 *                         e.g. /admin/Music/1-01 THE TIDE IS HIGH.mp3(admin)
	 */
	private String getPathFromId(String id){
		String path = null;
		String url = "music:"+id;
		
		String cmd = "http://"+server.hostname+"/rtCGI.fcgi?id=3&action=get_property&url="+url;
		RtCGIContentFilePropertyResponse response = (RtCGIContentFilePropertyResponse) server
				.getRtCGIResponse(cmd, RtCGIContentFilePropertyResponse.class);

		if(response != null){
			path = response.path;	
			
			if(path != null){
				path = Util.extractRelativePath(path, server.isUserAdmin());
			}else{
				Log.d(TAG,"music's full path is null" + "id = "+ id);
			}			
		}else{
			Log.d(TAG,"getFilePathFromResponse(), response is null"+ ", url ="+url);
		}
		
		return path;
	}
	
	/**
	 * 
	 * @param album name or artist name
	 * @return music album cover's path
	 */
	private String getThumbnailPath(String name){
		return "http://"+server.hostname+"/dav/.MusicThumb/"+name.replace('/',  ';')+".jpg?session="+server.hash;
	}
	
	/**
	 * 
	 * @param relativePath begin with "/"
	 * @return full path of URL for playback
	 */
	private String getWebPath(String relativePath){
		try {
			relativePath = URLEncoder.encode(relativePath, "UTF-8");
			relativePath = relativePath.replace("+", "%20");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		String davHome = server.getDavHome();
		return "http://" + server.hostname +davHome+ relativePath 
				+ "?hash=" + server.hash + "&login="+ server.username;
	}
}
