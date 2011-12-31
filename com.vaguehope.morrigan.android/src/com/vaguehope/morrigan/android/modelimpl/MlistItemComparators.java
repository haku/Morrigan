package com.vaguehope.morrigan.android.modelimpl;

import java.util.Comparator;

import com.vaguehope.morrigan.android.model.MlistItem;

public enum MlistItemComparators implements Comparator<MlistItem> {
	
	FILENAME () {
		@Override
		public int compare (MlistItem o1, MlistItem o2) {
			return o1.getFileName().compareTo(o2.getFileName());
		}
	}
	;
	
	@Override public abstract int compare (MlistItem o1, MlistItem o2);
	
}
