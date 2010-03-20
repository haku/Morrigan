package net.sparktank.morrigan.model.media;

import net.sparktank.morrigan.helpers.EqualHelper;

public class PlayItem {
	
	public MediaList list;
	public MediaItem item;
	
	public PlayItem (MediaList list, MediaItem item) {
		this.list = list;
		this.item = item;
	}
	
	@Override
	public boolean equals (Object obj) {
		if ( obj == null ) return false;
		if ( this == obj ) return true;
		if ( !(obj instanceof PlayItem) ) return false;
		PlayItem that = (PlayItem)obj;
		
		return EqualHelper.areEqual(list, that.list)
			&& EqualHelper.areEqual(item, that.item);
	}
	
	@Override
	public int hashCode() {
		int hash = 1;
	    hash = hash * 31 + (list == null ? 0 : list.hashCode());
	    hash = hash * 31 + (item == null ? 0 : item.hashCode());
	    return hash;
	}
	
}
