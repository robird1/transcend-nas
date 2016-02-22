package com.realtek.nasfun.api;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true) 
public class VideoResponse {

	@JsonIgnoreProperties(ignoreUnknown=true) 
	static class Index{
		public String id;
		public String name;

		@Override
		public String toString() {
			return "Index [id=" + id + ", name=" + name + "]";
		}
	}
	
	

	
	/**
	 * POJO for these API response:
	 *  year_idx, drct_idx, snrt_idx, pfmr_idx, genr_idx 
	 * @author mark.yang
	 *
	 */
	@JsonIgnoreProperties(ignoreUnknown=true) 
	static class ListResponse {
		@JsonIgnoreProperties(ignoreUnknown=true) 
		static class IndexData{
			public int total;
			public int length;
			public ArrayList<Index> index;

			@Override
			public String toString() {
				return "IndexData [total=" + total + ", length=" + length + ", index="
						+ index + "]";
			}
			
		}

		public IndexData data;
		public String success;

		@Override
		public String toString() {
			return "ListResponse [data=" + data + ", success=" + success + "]";
		}
		
	}
	
	/**
	 * POJO for these API response:
	 *  year,drct, snrt, pfmr, genr 
	 * @author mark.yang
	 *
	 */
	@JsonIgnoreProperties(ignoreUnknown=true) 
	static class MovieResponse {
		@JsonIgnoreProperties(ignoreUnknown=true) 
		static class MovieData{
			@JsonIgnoreProperties(ignoreUnknown=true) 
			static class Movie{
				public String movie_id;
				public String original_available;
				public String year;
				public String sort_title;
				public String tagline;
				public String title;
				
				@Override
				public String toString() {
					return "Movie [movie_id=" + movie_id + ", original_available="
							+ original_available + ", year=" + year + ", sort_title="
							+ sort_title + ", tagline=" + tagline + ", title=" + title
							+ "]";
				}
			}
			
			public int total;
			public int length;
			public ArrayList<Movie> movies;

			@Override
			public String toString() {
				return "MovieData [total=" + total + ", length=" + length + ", movies="
						+ movies + "]";
			}
			
		}
		
		public MovieData data;
		public String success;
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "MovieResponse [data=" + data + ", success=" + success + "]";
		}
		
	}
	
	/**
	 * POJO for these API response: movie_id
	 * @author mark.yang
	 *
	 */
	@JsonIgnoreProperties(ignoreUnknown=true) 
	static class MovieDetailResponse {
		@JsonIgnoreProperties(ignoreUnknown=true) 
		static class MovieDetailData{
			@JsonIgnoreProperties(ignoreUnknown=true) 
			static class MovieWrap{
				@JsonIgnoreProperties(ignoreUnknown=true) 
				static class MovieDetailWithFile{
					@JsonIgnoreProperties(ignoreUnknown=true) 
					static class File {
						public String filename;
						public String path;
						@Override
						public String toString() {
							return "File [filename=" + filename + ", path=" + path + "]";
						}
						
					}
					public String movie_id;
					public String original_available;
					public String sort_title;
					public String tagline;
					public String title;
					public String year;
					public String summary;	
					public String poster;	
					public String rating;	
					public String reference;
					public String description;
					public int id;
					public File file;

					@Override
					public String toString() {
						return "MovieDetailWithFile [movie_id=" + movie_id
								+ ", original_available=" + original_available
								+ ", sort_title=" + sort_title + ", tagline=" + tagline
								+ ", title=" + title + ", year=" + year + ", summary="
								+ summary + ", poster=" + poster + ", rating=" + rating
								+ ", reference=" + reference + ", description="
								+ description + ", id=" + id + ", file=" + file + "]";
					}
				}
				@JsonProperty("0") 
				public MovieDetailWithFile zero; // "0"
				public ArrayList<Index> genre;
				public ArrayList<Index> author;
				public ArrayList<Index> director;
				public ArrayList<Index> performer;

				@Override
				public String toString() {
					return "MovieWrap [zero=" + zero + ", genre=" + genre + ", author="
							+ author + "]";
				}
				
			}

			public int total;
			public int length;
			public MovieWrap movies;

			@Override
			public String toString() {
				return "MovieDetailData [total=" + total + ", length=" + length
						+ ", movies=" + movies + "]";
			}
			
		}

		public MovieDetailData data;
		public String success;
		
		@Override
		public String toString() {
			return "MovieDetailResponse [data=" + data + ", success=" + success
					+ "]";
		}
	
	}
	
	/**
	 * POJO for these API response: rcnt
	 * @author mark.yang
	 *
	 */
	@JsonIgnoreProperties(ignoreUnknown=true) 
	static class MovieListResponse {
		// for recent movies
		@JsonIgnoreProperties(ignoreUnknown=true) 
		static class MovieListData{
			@JsonIgnoreProperties(ignoreUnknown=true) 
			static class MovieDetail{
				public String movie_id;
				public String original_available;
				public String sort_title;
				public String tagline;
				public String title;
				public String year;
				public String summary;	
				public String poster;	
				public String rating;	
				public String reference;
				public String description;
				
				@Override
				public String toString() {
					return "MovieDetail [movie_id=" + movie_id
							+ ", original_available=" + original_available
							+ ", sort_title=" + sort_title + ", tagline=" + tagline
							+ ", title=" + title + ", year=" + year + ", summary="
							+ summary + ", poster=" + poster + ", rating=" + rating
							+ ", reference=" + reference + ", description="
							+ description + "]";
				}
				
			}
			public int total;
			public int length;
			public ArrayList<MovieDetail> movies;

			@Override
			public String toString() {
				return "MovieListData [total=" + total + ", length=" + length
						+ ", movies=" + movies + "]";
			}
		}

		public MovieListData data;
		public String success;

		@Override
		public String toString() {
			return "MovieListResponse [data=" + data + ", success=" + success
					+ "]";
		}
	}
	
	/**
	 * POJO for these API response: unknown
	 * @author Phyllis
	 *
	 */
	@JsonIgnoreProperties(ignoreUnknown=true) 
	static class UnknownVideoResponse {
		// for unknown movies
		@JsonIgnoreProperties(ignoreUnknown=true) 
		static class MovieData{
			@JsonIgnoreProperties(ignoreUnknown=true) 
			static class Video{
				public String movie_id;
				public String path;
				public String filename;
				public String filesize;
				public String ctime;
				public String duration;
				
				@Override
				public String toString() {
					return "Video [movie_id=" + movie_id
							+ ", path=" + path
							+ ", filename=" + filename + ", filesize=" + filesize
							+ ", ctime=" + ctime + ", duration=" + duration
							+ "]";
				}
			}
			
			public int total;
			public int length;
			public ArrayList<Video> movies;

			@Override
			public String toString() {
				return "MovieData [total=" + total + ", length=" + length
						+ ", movies=" + movies + "]";
			}
		}

		public MovieData data;
		public String success;

		@Override
		public String toString() {
			return "UnknownVideoResponse [data=" + data + ", success=" + success
					+ "]";
		}
	}
	
}
