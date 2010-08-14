package net.sparktank.morrigan.model.tracks.playlist;

import net.sparktank.morrigan.helpers.EqualHelper;
import net.sparktank.morrigan.model.media.interfaces.IMediaTrack;
import net.sparktank.morrigan.model.media.interfaces.IMediaTrackList;

public class PlayItem {
	
	public IMediaTrackList<? extends IMediaTrack> list;
	public IMediaTrack item;
	
	public PlayItem (IMediaTrackList<? extends IMediaTrack> list, IMediaTrack item) {
		this.list = list;
		this.item = item;
	}
	
	@Override
	public boolean equals (Object obj) {
		if ( obj == null ) return false;
		if ( this == obj ) return true;
		if ( !(obj instanceof PlayItem) ) return false;
		PlayItem that = (PlayItem)obj;
		
		return EqualHelper.areEqual(this.list, that.list)
			&& EqualHelper.areEqual(this.item, that.item);
	}
	
	@Override
	public int hashCode() {
		int hash = 1;
	    hash = hash * 31 + (this.list == null ? 0 : this.list.hashCode());
	    hash = hash * 31 + (this.item == null ? 0 : this.item.hashCode());
	    return hash;
	}
	
}
