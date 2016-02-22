package com.realtek.nasfun.api;

public class MovieBean {
	
	public static class VideoInfoObj{
		String id;
		String name;
		
		public VideoInfoObj(String id, String name) {
			super();
			this.id = id;
			this.name = name;
		}
		/**
		 * @return the id
		 */
		public String getId() {
			return id;
		}
		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}
		
		public void setId(String id) {
			this.id = id;
		}
		/**
		 * @return the name
		 */
		public void setName(String name) {
			this.name = name;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return name;
		}
	}
	
	int id;
	String movieId;
	String originalAvailable;
	String sortTitle;
	String tagline;
	String title;
	String year;
	String summary;	
	String poster;	
	String rating;	
	String reference;
	String description;
	VideoInfoObj[] genre;
	VideoInfoObj[] author;
	VideoInfoObj[] director;
	VideoInfoObj[] performer;
	String filename;
	String path;
	
	
	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}


	/**
	 * @return the movieId
	 */
	public String getMovieId() {
		return movieId;
	}


	/**
	 * @return the originalAvailable
	 */
	public String getOriginalAvailable() {
		return originalAvailable;
	}


	/**
	 * @return the sortTitle
	 */
	public String getSortTitle() {
		return sortTitle;
	}


	/**
	 * @return the tagline
	 */
	public String getTagline() {
		return tagline;
	}


	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}


	/**
	 * @return the year
	 */
	public String getYear() {
		return year;
	}


	/**
	 * @return the summary
	 */
	public String getSummary() {
		return summary;
	}


	/**
	 * @return the poster
	 */
	public String getPoster() {
		return poster;
	}


	/**
	 * @return the rating
	 */
	public String getRating() {
		return rating;
	}


	/**
	 * @return the reference
	 */
	public String getReference() {
		return reference;
	}


	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}


	/**
	 * @return the genre
	 */
	public VideoInfoObj[] getGenre() {
		return genre;
	}


	/**
	 * @return the author
	 */
	public VideoInfoObj[] getAuthor() {
		return author;
	}

	
	/**
	 * @return the director
	 */
	public VideoInfoObj[] getDirector() {
		return director;
	}


	/**
`	 * @return the performer
	 */
	public VideoInfoObj[] getPerformer() {
		return performer;
	}
	
	
	/**
	 * @return the filename
	 */
	public String getFilename() {
		return filename;
	}


	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @return the path
	 */
	public void setPath(String path) {
		this.path=path;
	}
	
	public MovieBean(String mid, String title){
		this.movieId = mid;
		this.title = title;
	}
	
}
