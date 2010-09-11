package net.sparktank.morrigan.model.media.impl;

public class DurationData {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final long duration;
	private final boolean complete;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public DurationData (long duration, boolean complete) {
		this.duration = duration;
		this.complete = complete;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public long getDuration() {
		return this.duration;
	}
	
	public boolean isComplete() {
		return this.complete;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
