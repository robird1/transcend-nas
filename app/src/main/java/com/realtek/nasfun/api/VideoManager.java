package com.realtek.nasfun.api;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
// modified by silver
//import com.realtek.nasfun.R;

import com.realtek.nasfun.api.MovieBean.VideoInfoObj;
import com.realtek.nasfun.api.VideoResponse.Index;
import com.realtek.nasfun.api.VideoResponse.ListResponse;
import com.realtek.nasfun.api.VideoResponse.MovieDetailResponse;
import com.realtek.nasfun.api.VideoResponse.MovieDetailResponse.MovieDetailData.MovieWrap.MovieDetailWithFile;
import com.realtek.nasfun.api.VideoResponse.MovieListResponse;
import com.realtek.nasfun.api.VideoResponse.MovieResponse;
import com.realtek.nasfun.api.VideoResponse.MovieListResponse.MovieListData.MovieDetail;
import com.realtek.nasfun.api.VideoResponse.MovieResponse.MovieData.Movie;
import com.realtek.nasfun.api.VideoResponse.UnknownVideoResponse;
import com.realtek.nasfun.api.VideoResponse.UnknownVideoResponse.MovieData.Video;
import com.transcend.nas.R;

/**
 * VideoManager implements video API provided by NAS BOX
 * 
 * Get the VideoManager instance from server API.
 * Example:
 * 	Server server = ServerManager.INSTANCE.getCurrentServer();
 * 	VideoManager manager = server.getVideoManager();
 * @author mark.yang
 *
 */
public class VideoManager {
	private static final String TAG = "VideoManager";
	
	//categories are from "all" to "genre"
	public static final int VIEW_TYPE_VIDEO_LIST_ALL = 304;
	public static final int VIEW_TYPE_VIDEO_LIST_YEAR = 305;
	public static final int VIEW_TYPE_VIDEO_LIST_DIRECTOR = 306;
	public static final int VIEW_TYPE_VIDEO_LIST_PERFORMER = 307;
	public static final int VIEW_TYPE_VIDEO_LIST_SCENARIST = 308;
	public static final int VIEW_TYPE_VIDEO_LIST_UNKNOWN = 309;
	public static final int VIEW_TYPE_VIDEO_LIST_GENRE = 310;

	public static int VIEW_TYPE_VIDEO_LIST_BASE = VIEW_TYPE_VIDEO_LIST_ALL;
	
	private Server server;
	private boolean isUnKnownSupported = false;
	private static final String UNKNOWN_VIDEO_ID = "-1";
	// TODO: move android related code out of API
	/*
	static final int[] icons = { R.drawable.icon_list_video,
			R.drawable.icon_list_video_year,
			R.drawable.icon_list_video_director,
			R.drawable.icon_list_video_protagonist,
			R.drawable.icon_list_video_scenarist,
			R.drawable.icon_list_video_unknown
	};
	*/
	// modified by silver
	static final int[] icons = { R.drawable.ic_logo_transcend,
			R.drawable.ic_logo_transcend,
			R.drawable.ic_logo_transcend,
			R.drawable.ic_logo_transcend,
			R.drawable.ic_logo_transcend,
			R.drawable.ic_logo_transcend
	};

	/**
	 * Get the default icon resource id for specific video list type
	 * @param type
	 * @return icon drawable resource id
	 */
	public static int getIconRes(int type) {
		if (type >= VIEW_TYPE_VIDEO_LIST_ALL && type <= VIEW_TYPE_VIDEO_LIST_UNKNOWN)
			return icons[type-VIEW_TYPE_VIDEO_LIST_BASE];
		else
			return icons[VIEW_TYPE_VIDEO_LIST_ALL-VIEW_TYPE_VIDEO_LIST_BASE];
	}


	VideoManager(Server server, boolean isUnknown) {
		this.server = server;
		this.isUnKnownSupported = isUnknown;
	}
	
	public int getListStart() {
		return VIEW_TYPE_VIDEO_LIST_YEAR;
	}
	
	public int getListEnd() {
		if(isUnKnownSupported)
			return VIEW_TYPE_VIDEO_LIST_UNKNOWN;
		else
			return VIEW_TYPE_VIDEO_LIST_SCENARIST;
	}
	
	public ArrayList<MediaItemVideo> getYearList() {
		String commandURL = "http://" + server.hostname
				+ "/movie-api/movie_qry.php?op=year_idx";
		ListResponse resp = (ListResponse) getResponse(commandURL,
				ListResponse.class);
		return parseListResponse(resp, VIEW_TYPE_VIDEO_LIST_YEAR);
	}

	public ArrayList<MediaItemVideo> getDirectorList() {
		String commandURL = "http://" + server.hostname
				+ "/movie-api/movie_qry.php?op=drct_idx";
		ListResponse resp = (ListResponse) getResponse(commandURL,
				ListResponse.class);
		return parseListResponse(resp, VIEW_TYPE_VIDEO_LIST_DIRECTOR);
	}

	public ArrayList<MediaItemVideo> getPerformerList() {
		String commandURL = "http://" + server.hostname
				+ "/movie-api/movie_qry.php?op=pfmr_idx";
		ListResponse resp = (ListResponse) getResponse(commandURL,
				ListResponse.class);
		return parseListResponse(resp, VIEW_TYPE_VIDEO_LIST_PERFORMER);
	}

	public ArrayList<MediaItemVideo> getScenaristList() {
		String commandURL = "http://" + server.hostname
				+ "/movie-api/movie_qry.php?op=snrt_idx";
		ListResponse resp = (ListResponse) getResponse(commandURL,
				ListResponse.class);
		return parseListResponse(resp, VIEW_TYPE_VIDEO_LIST_SCENARIST);
	}

	public ArrayList<MediaItemVideo> getGenreList() {
		String commandURL = "http://" + server.hostname
				+ "/movie-api/movie_qry.php?op=genr_idx";
		ListResponse resp = (ListResponse) getResponse(commandURL,
				ListResponse.class);
		return parseListResponse(resp, VIEW_TYPE_VIDEO_LIST_GENRE);
	}
	
	public ArrayList<MediaItemVideo> getUnknownList(){
		String commandURL = "http://" + server.hostname
				+ "/movie-api/movie_qry.php?op=unknown";
		
		UnknownVideoResponse resp = (UnknownVideoResponse) getResponse(commandURL,
				UnknownVideoResponse.class);
		return parseUnknownVideoResponse(resp, VIEW_TYPE_VIDEO_LIST_ALL);
	}
	
	public ArrayList<MediaItemVideo> findByYear(String year) {
		String commandURL = "http://" + server.hostname
				+ "/movie-api/movie_qry.php?op=year&value=" + year;
		MovieResponse resp = (MovieResponse) getResponse(commandURL,
				MovieResponse.class);
		return parseMovieResponse(resp, VIEW_TYPE_VIDEO_LIST_ALL);
	}

	public ArrayList<MediaItemVideo> findByDirector(String name) {
		String commandURL = "http://" + server.hostname
				+ "/movie-api/movie_qry.php?op=drct&value=" + name;
		MovieResponse resp = (MovieResponse) getResponse(commandURL,
				MovieResponse.class);
		return parseMovieResponse(resp, VIEW_TYPE_VIDEO_LIST_ALL);
	}

	public ArrayList<MediaItemVideo> findByScenarist(String name) {
		String commandURL = "http://" + server.hostname
				+ "/movie-api/movie_qry.php?op=snrt&value=" + name;
		MovieResponse resp = (MovieResponse) getResponse(commandURL,
				MovieResponse.class);
		return parseMovieResponse(resp, VIEW_TYPE_VIDEO_LIST_ALL);
	}

	public ArrayList<MediaItemVideo> findByPerformer(String name) {
		String commandURL = "http://" + server.hostname
				+ "/movie-api/movie_qry.php?op=pfmr&value=" + name;
		MovieResponse resp = (MovieResponse) getResponse(commandURL,
				MovieResponse.class);
		return parseMovieResponse(resp, VIEW_TYPE_VIDEO_LIST_ALL);
	}

	public ArrayList<MediaItemVideo> getRecentAdded() {
		String commandURL = "http://" + server.hostname
				+ "/movie-api/movie_qry.php?op=rcnt";
		MovieListResponse resp = (MovieListResponse) getResponse(commandURL,
				MovieListResponse.class);
		return parseMovieListResponse(resp, VIEW_TYPE_VIDEO_LIST_ALL);
	}

	public MovieBean getMovie(String id) {
		String commandURL = "http://" + server.hostname
				+ "/movie-api/movie_qry.php?op=movie_id&value=" + id;
		MovieDetailResponse resp = (MovieDetailResponse) getResponse(
				commandURL, MovieDetailResponse.class);
		return parseMovieDetailResponse(resp, VIEW_TYPE_VIDEO_LIST_ALL);
	}

	private Object getResponse(String command, Class<?> t) {
		Log.d(TAG, "getResponse:"+command);
		String reqString = command;
		String rspString = null;
		try {
			// prepare http post requet
			HttpGet httpGet = new HttpGet(command);
			DefaultHttpClient httpClient = HttpClientManager.getClient();
			HttpResponse httpResponse = httpClient.execute(httpGet);
			HttpEntity rspEntity = httpResponse.getEntity();
			String inputEncoding = EntityUtils.getContentCharSet(rspEntity);
			if (inputEncoding == null)
				inputEncoding = HTTP.UTF_8;
			rspString = EntityUtils.toString(rspEntity, inputEncoding);
			return server.mapper.readValue(rspString, t);

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			Log.d(TAG, "Request = " + reqString);
			Log.d(TAG, "Response = " + rspString);
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch(IllegalArgumentException e){
			e.printStackTrace();
		}
		return null;
	}

	private ArrayList<MediaItemVideo> parseListResponse(ListResponse resp, int type) {
		ArrayList<MediaItemVideo> items = null;
		if (resp != null && "true".equals(resp.success)) {
			items = new ArrayList<MediaItemVideo>(resp.data.total);
			for (Index index : resp.data.index) {
				MediaItemVideo mi = new MediaItemVideo(VideoManager.getIconRes(type),
						index.name, null, false);
				mi.setId(index.id);
				items.add(mi);
			}
		}
		return items;
	}

	private ArrayList<MediaItemVideo> parseMovieResponse(MovieResponse resp, int type) {
		ArrayList<MediaItemVideo> items = null;
		if (resp != null && "true".equals(resp.success)) {
			items = new ArrayList<MediaItemVideo>(resp.data.total);
			for (Movie movie : resp.data.movies) {
				MediaItemVideo mi = new MediaItemVideo(VideoManager.getIconRes(type),
						movie.title, movie.year, false);
				mi.setId(movie.movie_id);
				items.add(mi);
			}
		}
		return items;
	}

	private ArrayList<MediaItemVideo> parseMovieListResponse(MovieListResponse resp,
			int type) {
		ArrayList<MediaItemVideo> items = null;
		if (resp != null && "true".equals(resp.success)) {
			items = new ArrayList<MediaItemVideo>(resp.data.total);
			for (MovieDetail md : resp.data.movies) {
				MediaItemVideo mi = new MediaItemVideo(VideoManager.getIconRes(type),
						md.title, md.year, false);
				mi.setId(md.movie_id);
				items.add(mi);
			}
		}
		return items;
	}
	
	private ArrayList<MediaItemVideo> parseUnknownVideoResponse(UnknownVideoResponse resp, 
			int type) {
		ArrayList<MediaItemVideo> items = null;
		if (resp != null && "true".equals(resp.success)) {
			items = new ArrayList<MediaItemVideo>(resp.data.total);
			for (Video video : resp.data.movies) {
				String filename = video.filename;
				int index = filename.lastIndexOf('.');
				String title = filename.substring(0,index);
				MediaItemVideo mi = new MediaItemVideo(VideoManager.getIconRes(type),
						title, null, false);
				mi.setId(video.movie_id);
				mi.setFileName(filename);
				mi.setPath(video.path);
				items.add(mi);
			}
		}
		return items;
	}
	
	private MovieBean parseMovieDetailResponse(MovieDetailResponse resp,
			int type) {
		MovieBean movie = null;
		if (resp != null && "true".equals(resp.success)) {
			if(resp.data.movies.zero == null) {
				Log.w(TAG, "resp.data.movies.zero==null, "+resp.toString());
				return null;
			}
			MovieDetailWithFile md = resp.data.movies.zero;
			if(md != null){
				movie = new MovieBean(md.movie_id, md.title);
				movie.author = getTheMovieInfo(resp.data.movies.author);
				movie.genre = getTheMovieInfo(resp.data.movies.genre);
				movie.director = getTheMovieInfo(resp.data.movies.director);
				movie.performer = getTheMovieInfo(resp.data.movies.performer);
				movie.description = md.description;
				movie.filename = md.file.filename;
				movie.path = md.file.path;
				movie.id = md.id;
				movie.originalAvailable = md.original_available;
				movie.poster = md.poster;
				movie.rating = md.rating;
				movie.reference = md.reference;
				movie.sortTitle = md.sort_title;
				movie.summary = md.summary;
				movie.tagline = md.tagline;
				movie.year = md.year;
			}
		}
		return movie;
	}
	
	private VideoInfoObj[] getTheMovieInfo(ArrayList<Index> list) {
		if(list != null && list.size()>0) {
			int count = list.size();
			VideoInfoObj[] infos= new VideoInfoObj[count];
			for (int i = 0; i < count; i++) {
				Index idx = list.get(i);
				infos[i] = new VideoInfoObj(idx.id, idx.name);
			}
			return infos;
		}
		return null;
	}
	
	public static boolean isMovie(String movie_id) {
		//if movie_id = -1 => this is an unknown video file, and it's not a movie
		if(movie_id.equals(UNKNOWN_VIDEO_ID))
			return false;
		else
			return true;
	}
	
}