package net.sparktank.morrigan.model.media.interfaces;

public interface IMediaTrackDb<H extends IMediaTrackDb<H,S,T>, S extends IMediaItemStorageLayer<T>, T extends IMediaTrack>
		extends IMediaItemDb<H,S,T>, IMediaTrackList<T> {
	
	/**/
	
}
