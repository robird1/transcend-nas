package com.realtek.nasfun.api;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Base abstract class for media items. 
 * A media item can holds the followings
 * Photo, Title, Subtitle, Dragger
 * 
 * TODO: Remove UI and android related code 
 * @author mark.yang
 *
 */
public class MediaItem {
	
	private int photoRes;
	private String title;
	private String subTitle;
	private boolean isDraggable;
	private String path;
	private String id;
	
	// TODO: change back to protected 
	// (change to public due to create video categories in MediaListFragment)
	public MediaItem(int photoRes, String title, String subTitle, boolean draggable){
		this.photoRes = photoRes;
		this.title = title;
		this.subTitle = subTitle;
		this.isDraggable = draggable;
	}
	
	public void setThumbnailView(Context c, ImageView i){
		setPhotoView(c, i);
	}
	
	public void setPhotoView(Context c, ImageView i) {
		if(photoRes > 0) {
			i.setImageResource(photoRes);
			i.setVisibility(View.VISIBLE);
		} else {
			i.setVisibility(View.GONE);
		}
	}
	
	public void setTitleView(Context c, TextView t) {
		if(title != null) {
			t.setText(title);
			t.setVisibility(View.VISIBLE);
		} else {
			t.setVisibility(View.INVISIBLE);
		}
	}
	
	public void setSubtitleView(Context c, TextView st, int inVisibility) {
		if(subTitle != null) {
			st.setText(subTitle);
			st.setVisibility(View.VISIBLE);
		} else {
			st.setVisibility(inVisibility);
		}
	}
	
	public void setDraggerView(Context c, ImageView d) {
		if(isDraggable) {
			d.setVisibility(View.VISIBLE);
		} else {
			d.setVisibility(View.GONE);
		}
	}

	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getPhotoRes() {
		return photoRes;
	}

	public void setPhotoRes(int photoRes) {
		this.photoRes = photoRes;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSubTitle() {
		return subTitle;
	}

	public void setSubTitle(String subTitle) {
		this.subTitle = subTitle;
	}

	public boolean isDraggable() {
		return isDraggable;
	}

	public void setDraggable(boolean isDraggable) {
		this.isDraggable = isDraggable;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
}
