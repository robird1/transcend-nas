package com.realtek.nasfun.api;

import com.realtek.nasfun.api.VideoResponse.Index;
import com.realtek.nasfun.api.VideoResponse.ListResponse;
import com.realtek.nasfun.api.VideoResponse.MovieResponse;
import com.realtek.nasfun.api.VideoResponse.MovieResponse.MovieData.Movie;

public class VideoCollection {

	

	public static class ListResponseCollection extends MediaCollection{
		private ListResponse resp;
		private int type;
		
		
		public ListResponseCollection(ListResponse resp, int type) {
			this.resp = resp;
			this.type = type;
		}
		
		@Override
		public MediaItem getMediaItem(int position) {
			Index index = resp.data.index.get(position);
			MediaItem item = new MediaItem(VideoManager.getIconRes(type),
					index.name, null, false);
			item.setId(index.id);
			return item;
		}

		@Override
		public int size() {
			return resp.data.total;
		}
		
	}
	
	public static class MovieResponseCollection extends MediaCollection{
		private MovieResponse resp;
		private int type;
		
		MovieResponseCollection(MovieResponse resp, int type) {
			this.resp = resp;
			this.type = type;
		}
		
		@Override
		public MediaItem getMediaItem(int position) {
			Movie movie = resp.data.movies.get(position);
			MediaItem item = new MediaItem(VideoManager.getIconRes(type),
					movie.title, movie.original_available, false);
			item.setId(movie.movie_id);
			return item;
		}

		@Override
		public int size() {
			return resp.data.total;
		}
		
	}
	
	
}
