package com.realtek.nasfun.api;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties(ignoreUnknown=true) 
public class RtCGIMusicArtistListResponse {
	@JsonIgnoreProperties(ignoreUnknown=true) 
	public static class ArtistItem{
		public int num_of_albums;
		public int num_of_songs ;
		public int id;
		public String artist;
		
		@Override
		public String toString() {
			return "ArtistItem [num_of_albums=" + Integer.toString(num_of_albums) + ", num_of_songs=" + Integer.toString(num_of_songs) + ", id="
					+ Integer.toString(id) + ", artist=" + artist +"]";
		}
	}
	
	public int status;
	public String response_type;
	public int total;
	public int offset;
	public String num_item;
	public ArrayList<ArtistItem> items;
	public String error_desc;

	@Override
	public String toString() {
		return "RtCGIMusicArtistListResponse [status=" + status
				+ ", response_type=" + response_type + ", total=" + total
				+ ", offset=" + offset + ", num_item=" + num_item + ", items="
				+ items + ", error_desc=" + error_desc + "]";
	}
}
