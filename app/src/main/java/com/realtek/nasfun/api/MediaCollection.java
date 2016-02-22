package com.realtek.nasfun.api;

/**
 * 
 * @author mark.yang
 *
 */
public abstract class MediaCollection {
	public abstract MediaItem getMediaItem(int position);
	public abstract int size();
}
