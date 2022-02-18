package com.vaguehope.morrigan.model.media;


public interface IMixedMediaItem extends IMediaTrack, IMediaPicture {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Fixed enums - changing these requires writing more code.

	static enum MediaType {
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

		public static MediaType parseInt (int n) {
			switch (n) {
				case 0: return UNKNOWN;
				case 1: return TRACK;
				case 2: return PICTURE;
				default: throw new IllegalArgumentException();
			}
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	MediaType getMediaType ();
	boolean setMediaType (MediaType newType);

	boolean setFromMediaMixedItem (IMixedMediaItem mmi);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
