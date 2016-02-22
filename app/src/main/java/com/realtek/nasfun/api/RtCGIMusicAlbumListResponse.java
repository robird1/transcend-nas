package com.realtek.nasfun.api;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties(ignoreUnknown=true) 
public class RtCGIMusicAlbumListResponse {
	@JsonIgnoreProperties(ignoreUnknown=true) 
	public static class AlbumItem{
		public int num_of_songs;
		public int id ;
		public String artist;
		public String album;

		@Override
		public String toString() {
			return "AlbumItem [num_of_songs=" + Integer.toString(num_of_songs) + ", id=" + Integer.toString(id) 
					+", artist=" + artist + ", album=" + album +"]";
		}
	}
	
	public int status;
	public String response_type;
	public int total;
	public int offset;
	public String num_item;
	public ArrayList<AlbumItem> items;
	public String error_desc;

	@Override
	public String toString() {
		return "RtCGIMusicAlbumListResponse [status=" + status
				+ ", response_type=" + response_type + ", total=" + total
				+ ", offset=" + offset + ", num_item=" + num_item + ", items="
				+ items + ", error_desc=" + error_desc + "]";
	}
}
