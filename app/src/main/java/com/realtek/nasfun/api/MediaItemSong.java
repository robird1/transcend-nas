package com.realtek.nasfun.api;

import android.content.Context;
import android.widget.ImageView;

// modified by silver
//import com.nostra13.universalimageloader.core.DisplayImageOptions;
//import com.nostra13.universalimageloader.core.ImageLoader;
//import com.realtek.nasfun.R;

public class MediaItemSong extends MediaItem{
	// modified by silver
	/*
	private static DisplayImageOptions thumbnaiOptions;
	private static DisplayImageOptions photoOptions;

	static{
		photoOptions = new DisplayImageOptions.Builder()
        .showStubImage(R.drawable.icon_thumbnails_audio_noalbumphoto) // resource or drawable
        .cacheInMemory(true)
		.cacheOnDisc(true)
        .build();
		thumbnaiOptions = new DisplayImageOptions.Builder()
        .showStubImage(R.drawable.icon_list_audio_song) // resource or drawable
        .cacheInMemory(true)
		.cacheOnDisc(true)
        .build();
	};
	*/
	private String name;
	private String artist;
	private String album;
	private String year;
	private String thumbnail;
	
	MediaItemSong(int photoRes, String title, String subTitle,
			boolean draggable) {
		super(photoRes, title, subTitle, draggable);
	}
	
	@Override
	public void setThumbnailView(Context c, ImageView i) {
		// modified by silver
		/*
		if(thumbnail != null){
			ImageLoader imageLoader = ImageLoader.getInstance();
			imageLoader.displayImage(thumbnail, i, thumbnaiOptions);			
		} else {
			i.setImageResource(this.getPhotoRes());
		}
		*/
	}

	@Override
	public void setPhotoView(Context c, ImageView i) {
		// modified by silver
		/*
		if(thumbnail != null){
			ImageLoader imageLoader = ImageLoader.getInstance();
			imageLoader.displayImage(thumbnail, i, photoOptions);			
		} else {
			i.setImageResource(R.drawable.icon_thumbnails_audio_noalbumphoto);
		}
		*/
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getAlbum() {
		return album;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	public String getYear() {
		return year;
	}

	public void setYear(String year) {
		this.year = year;
	}

	public String getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
	}

}
