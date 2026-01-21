package morrigan.model.media;

public class DurationData {

	private final long duration;
	private final boolean complete;

	public DurationData(final long duration, final boolean complete) {
		this.duration = duration;
		this.complete = complete;
	}

	public long getDuration() {
		return this.duration;
	}

	public boolean isComplete() {
		return this.complete;
	}

}
