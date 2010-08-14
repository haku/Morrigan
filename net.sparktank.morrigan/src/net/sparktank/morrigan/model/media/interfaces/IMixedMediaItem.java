package net.sparktank.morrigan.model.media.interfaces;


public interface IMixedMediaItem extends IMediaTrack, IMediaPicture {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Fixed enums - changing these requires writing more code.
	
	public static enum MediaType {
		UNKNOWN(0), TRACK(1), PICTURE(2);
		
		private final int n;
		
		MediaType(int n) {
			this.n = n;
		}
		
		public int getN() {
			return this.n;
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
