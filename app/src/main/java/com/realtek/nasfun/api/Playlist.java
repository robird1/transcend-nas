package com.realtek.nasfun.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class Playlist implements List<MediaItemSong>{

	private List<MediaItemSong> list;
	private int focusIdx = -1;
	private ArrayList<OnPlaylistChangeListener> listener;
	
	Playlist(){
		list = Collections.synchronizedList(new ArrayList<MediaItemSong>());
		listener = new ArrayList<OnPlaylistChangeListener>();
	}
	
	public void registeListener(OnPlaylistChangeListener l){
		listener.add(l);
	}
	
	public void unregisteListener(OnPlaylistChangeListener l){
		listener.remove(l);
	}
	
	public void setFocus(int which){
		if(which < list.size())
			this.focusIdx = which;
	}
	
	public int getFocus(){
		return focusIdx;
	}
	
	public void move(int from, int to){
		MediaItemSong song = list.remove(from);
		list.add(to, song);
	}

	@Override
	public MediaItemSong get(int index){
		return list.get(index);
	}

	@Override
	public int size(){
		return list.size();
	}
	
	@Override
	public MediaItemSong remove(int which){
		MediaItemSong song = list.remove(which);
		if(list.size() == 0){
			focusIdx = -1;
			for(OnPlaylistChangeListener l: listener){
				l.onListEmpty();
			}
		}
		return song; 
	}
	
	@Override
	public void clear(){
		list.clear();
		focusIdx = -1;
		for(OnPlaylistChangeListener l: listener){
			l.onListEmpty();
		}
	}
	

	@Override
	public boolean add(MediaItemSong object) {
		int origSize = list.size();
		boolean result = list.add(object);
		if(origSize == 0){
			focusIdx = 0;
			for(OnPlaylistChangeListener l: listener){
				l.onFistSongAdded();
			}
		}
		return result;
	}

	@Override
	public boolean addAll(Collection<? extends MediaItemSong> arg0) {
		int origSize = list.size();
		boolean result = list.addAll(arg0);
		if(origSize == 0){
			focusIdx = 0;
			for(OnPlaylistChangeListener l: listener){
				l.onFistSongAdded();
			}
		}
		return result;
	}

	@Override
	public boolean addAll(int arg0, Collection<? extends MediaItemSong> arg1) {
		int origSize = list.size();
		boolean result = list.addAll(arg0, arg1);
		if(origSize == 0){
			focusIdx = 0;
			for(OnPlaylistChangeListener l: listener){
				l.onFistSongAdded();
			}
		}
		return result;
	}

	@Override
	public boolean contains(Object object) {
		return list.contains(object);
	}

	@Override
	public boolean containsAll(Collection<?> arg0) {
		return list.containsAll(arg0);
	}

	@Override
	public int indexOf(Object object) 
	{
		return list.indexOf(object);
	}

	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}

	@Override
	public Iterator<MediaItemSong> iterator() {
		return list.iterator();
	}

	@Override
	public int lastIndexOf(Object object) {
		return list.lastIndexOf(object);
	}

	@Override
	public ListIterator<MediaItemSong> listIterator() {
		return list.listIterator();
	}

	@Override
	public ListIterator<MediaItemSong> listIterator(int location) {
		return list.listIterator(location);
	}

	@Override
	public boolean remove(Object object) {
		boolean result = list.remove(object);
		if(list.size() == 0) {
			focusIdx = -1;
			for(OnPlaylistChangeListener l: listener){
				l.onListEmpty();
			}
		}
		return result;
	}

	@Override
	public boolean removeAll(Collection<?> arg0) {
		boolean result = list.removeAll(arg0);
		focusIdx = -1;
		for(OnPlaylistChangeListener l: listener){
			l.onListEmpty();
		}
		return result;
	}

	@Override
	public boolean retainAll(Collection<?> arg0) {
		return list.retainAll(arg0);
	}

	@Override
	public MediaItemSong set(int location, MediaItemSong object) {
		return list.set(location,  object);
	}

	@Override
	public List<MediaItemSong> subList(int start, int end) {
		return list.subList(start,  end);
	}

	@Override
	public Object[] toArray() {
		return list.toArray();
	}

	@Override
	public <T> T[] toArray(T[] array) {
		return list.toArray(array);
	}

	@Override
	public void add(int location, MediaItemSong object) {
		list.add(location, object);
		if(list.size() == 1){
			focusIdx = 0;
			for(OnPlaylistChangeListener l: listener){
				l.onFistSongAdded();
			}			
		}
	}

	public interface OnPlaylistChangeListener{
		public void onListEmpty();
		public void onFistSongAdded();
	}
}
