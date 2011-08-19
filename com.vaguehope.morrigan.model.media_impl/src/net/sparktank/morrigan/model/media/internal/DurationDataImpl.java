package net.sparktank.morrigan.model.media.internal;

import com.vaguehope.morrigan.model.media.DurationData;

public class DurationDataImpl implements DurationData {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final long duration;
	private final boolean complete;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public DurationDataImpl (long duration, boolean complete) {
		this.duration = duration;
		this.complete = complete;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public long getDuration() {
		return this.duration;
	}
	
	@Override
	public boolean isComplete() {
		return this.complete;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
