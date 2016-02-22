package com.realtek.nasfun.api;

import java.util.List;

public abstract class MusicManager {
	protected Playlist playList = null;
	protected Server server;
	
	MusicManager(Server server){
		this.server = server;
	}

	public Playlist getCurrentPlaylist(){
		if(playList == null){
			playList = new Playlist();
			// load data
		}
		return playList;
	}
	
	public abstract List<MediaItemSong> getArtists();
	public abstract List<MediaItemSong> getAlbums();

	public abstract List<MediaItemSong> getRecentSong();
	public abstract List<MediaItemSong> findSongByArtist(String artist);
	public abstract List<MediaItemSong> findSongByAlbum(String album);
}