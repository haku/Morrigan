package com.vaguehope.morrigan.player;

import com.vaguehope.morrigan.model.helper.EqualHelper;

import net.sparktank.morrigan.model.media.IMediaTrack;
import net.sparktank.morrigan.model.media.IMediaTrackList;

// TODO add interface to refer to this class with.
public class PlayItem {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	// TODO make these final.
	public final IMediaTrackList<? extends IMediaTrack> list;
	public IMediaTrack item;
	public int id;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public PlayItem (IMediaTrackList<? extends IMediaTrack> list, IMediaTrack item) {
		this(list, item, -1);
	}
	
	public PlayItem (IMediaTrackList<? extends IMediaTrack> list, IMediaTrack item, int id) {
		this.list = list;
		this.item = item;
		this.id = id;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String toString() {
		if (this.item == null) {
			return this.list.getListName();
		}
		
		return this.list.getListName() + "/" + this.item.getTitle();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public boolean equals (Object obj) {
		if ( obj == null ) return false;
		if ( this == obj ) return true;
		if ( !(obj instanceof PlayItem) ) return false;
		PlayItem that = (PlayItem)obj;
		
		return EqualHelper.areEqual(this.list, that.list)
			&& EqualHelper.areEqual(this.item, that.item)
			&& this.id == that.id;
	}
	
	@Override
	public int hashCode() {
		int hash = 1;
		hash = hash * 31 + (this.list == null ? 0 : this.list.hashCode());
		hash = hash * 31 + (this.item == null ? 0 : this.item.hashCode());
		hash = hash * 31 + this.id;
		return hash;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
