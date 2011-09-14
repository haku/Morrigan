package com.vaguehope.morrigan.model.media;


public interface IMixedMediaItem extends IMediaTrack, IMediaPicture {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Fixed enums - changing these requires writing more code.
	
	public static enum MediaType {
		UNKNOWN(0, "all"), TRACK(1, "tracks"), PICTURE(2, "pictures");
		
		private final String humanName;
		private final int n;
		
		MediaType(int n, String humanName) {
			this.n = n;
			this.humanName = humanName;
		}
		
		public int getN() {
			return this.n;
		}
		
		public String getHumanName () {
			return this.humanName;
		}
		
		@Override
		public String toString() {
			return getHumanName();
		}
		
		static public MediaType parseInt (int n) {
			switch (n) {
				case 0: return UNKNOWN;
				case 1: return TRACK;
				case 2: return PICTURE;
				default: throw new IllegalArgumentException();
			}
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public MediaType getMediaType ();
	public boolean setMediaType (MediaType newType);
	
	public boolean setFromMediaMixedItem (IMixedMediaItem mmi);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
