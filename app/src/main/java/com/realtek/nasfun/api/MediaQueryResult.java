package com.realtek.nasfun.api;

import java.util.List;

public class MediaQueryResult {
	int index = -1;
	int totalCount = -1;
	List<MediaItem> items = null;
	
	public int getIndex() {
		return index;
	}
	public int getTotalCount() {
		return totalCount;
	}
	public List<MediaItem> getItems() {
		return items;
	}
	
}
