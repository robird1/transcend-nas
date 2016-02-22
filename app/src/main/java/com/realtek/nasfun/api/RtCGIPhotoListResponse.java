package com.realtek.nasfun.api;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties(ignoreUnknown=true) 
public class RtCGIPhotoListResponse {
	@JsonIgnoreProperties(ignoreUnknown=true) 
	public static class PhotoItem{
		public int bucket_id;
		public int id;
		public String title;
		public long date_taken;
		public int height;
		public int width;
		public String disp_name;
		public String bucket_disp_name;
		public int size;
		
		@Override
		public String toString() {
			return "PhotoItem [bucket_id=" + Integer.toString(bucket_id) + ", id=" + Integer.toString(id) + ", title="
					+title + ", date_taken="+ Long.toString(date_taken) +", width=" + Integer.toString(width) +", height=" + Integer.toString(height)
					+ ", disp_name=" + disp_name + ", bucket_disp_name=" + bucket_disp_name + ", size="+ Integer.toString(size) +"]";
		}
	}

	public int status;
	public int num_item;
	public int total;
	public String response_type;	
	public int offset;
	public ArrayList<PhotoItem> items;
	public String error_desc;

	@Override
	public String toString() {
		return "RtCGIPhotoListResponse [status=" + status + ", num_item="
				+ num_item + ", total=" + total + ", response_type="
				+ response_type + ", offset=" + offset + ", items=" + items
				+ ", error_desc=" + error_desc + "]";
	}
	
	
}
