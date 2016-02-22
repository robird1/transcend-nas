package com.realtek.nasfun.api;

public class MediaItemVideo extends MediaItem{
	public static final int TYPE_YEAR_LIST=0;
	private String filename;
	
	public MediaItemVideo(int photoRes, String title, String subTitle,
			boolean draggable) {
		super(photoRes, title, subTitle, draggable);
	}
	
	public String getFileName() {
		return filename;
	}
	
	public void setFileName(String filename) {
		this.filename = filename;
	}
}
