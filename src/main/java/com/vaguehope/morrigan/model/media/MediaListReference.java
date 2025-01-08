package com.vaguehope.morrigan.model.media;

public interface MediaListReference extends Comparable<MediaListReference> {

	public enum MediaListType {
		LOCALMMDB("Local"),
		REMOTEMMDB("Remote"),
		EXTMMDB("External");

		private final String uiString;

		private MediaListType(final String uiString) {
			this.uiString = uiString;
		}

		public String uiString() {
			return this.uiString;
		}
	}

	MediaListType getType ();

	/**
	 * same as IMediaItemList.getListId()
	 */
	String getIdentifier ();
	String getTitle ();
	String getMid();
	boolean isHasRootNodes();

}
