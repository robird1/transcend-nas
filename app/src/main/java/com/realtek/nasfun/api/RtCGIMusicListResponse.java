package com.realtek.nasfun.api;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties(ignoreUnknown=true) 
public class RtCGIMusicListResponse {
	@JsonIgnoreProperties(ignoreUnknown=true) 
	public static class SongItem{
		public int id;
		public int duration;
		public String artist;
		public int size;
		public String album;
		public String disp_name;
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "SongItem [id=" + Integer.toString(id) + ", duration=" + Integer.toString(duration) + ", artist="
					+ artist + ", size=" + Integer.toString(size)+ ", album=" + album + ", disp_name=" + disp_name +"]";
		}
	}
	
	public int status;
	public String response_type;
	public int total;
	public int offset;
	public String num_item;
	public ArrayList<SongItem> items;
	public String error_desc;
	
	@Override
	public String toString() {
		return "RtCGIMusicListResponse [status=" + status + ", response_type="
				+ response_type + ", total=" + total + ", offset=" + offset
				+ ", num_item=" + num_item + ", items=" + items
				+ ", error_desc=" + error_desc + "]";
	}
}
