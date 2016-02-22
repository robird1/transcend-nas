package com.realtek.nasfun.api;

// modified by silver
/*
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.realtek.nasfun.R;
*/
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.ImageView;

public class MediaItemPhoto extends MediaItem implements Parcelable{
	private String album;
	// modified by silver
	/*
	private static DisplayImageOptions options;
	static{
		options = new DisplayImageOptions.Builder()
        .showStubImage(R.drawable.icon_thumbnails_album_noalbumphoto) // resource or drawable
        .cacheInMemory(true)
		.cacheOnDisc(true)
        .build();
	};
	*/
	public MediaItemPhoto(int photoRes, String title, String subTitle,
			boolean draggable) {
		super(photoRes, title, subTitle, draggable);
	}
	
	public MediaItemPhoto(Parcel parcel) {
		super(parcel.readInt(), parcel.readString(), parcel.readString(), parcel.readByte() != 0);
		this.setPath(parcel.readString());
	}
	
	@Override
	public void setThumbnailView(Context c, ImageView i) {
		// modified by silver
		/*
		String path = this.getPath();
		if (path != null) {
			ImageLoader imageLoader = ImageLoader.getInstance();
			String url = path + "&thumbnail";
			imageLoader.displayImage(url, i);
		} else {
			i.setImageResource(getPhotoRes());
		}
		*/
	}

	@Override
	public void setPhotoView(Context c, ImageView i) {
		// modified by silver
		/*
		String path = this.getPath();
		if (path != null) {
			ImageLoader imageLoader = ImageLoader.getInstance();
			String url = path + "&webview";
			imageLoader.displayImage(url, i, options);
		} else {
			i.setImageResource(R.drawable.icon_thumbnails_album_noalbumphoto);
		}
		*/
	}

	/**
	 * @return the album
	 */
	public String getAlbum() {
		return album;
	}

	/**
	 * @param album the album to set
	 */
	public void setAlbum(String album) {
		this.album = album;
	}

	@Override
	public int describeContents() {		
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(getPhotoRes());
		dest.writeString(getTitle());
		dest.writeString(getSubTitle());
		dest.writeByte((byte) (isDraggable() ? 1:0));
		dest.writeString(getPath());
	}
	
	@SuppressWarnings("rawtypes")
	public static final Parcelable.Creator CREATOR
    = new Parcelable.Creator() {
		  public MediaItem createFromParcel(Parcel in) {
	             return new MediaItemPhoto(in);
	      }

	      public MediaItem[] newArray(int size) {
	             return new MediaItemPhoto[size];
	      }
	};
}
