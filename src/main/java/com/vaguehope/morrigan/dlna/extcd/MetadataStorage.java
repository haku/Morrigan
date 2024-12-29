package com.vaguehope.morrigan.dlna.extcd;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

public class MetadataStorage {

	private final IMediaItemStorageLayer storage;

	public MetadataStorage (final IMediaItemStorageLayer storage) {
		this.storage = storage;
	}

	public void dispose () {
		this.storage.dispose();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Writes.

	public void incTrackStartCnt (final IMediaItem item) throws MorriganException {
		try {
			try {
				this.storage.addFile(MediaType.TRACK, item.getRemoteId(), 0);
				this.storage.incTrackPlayed(new ItemWithFilepath(item, item.getRemoteId()));
			}
			finally {
				this.storage.commitOrRollBack();
				uncache(item);
			}
		}
		catch (final DbException e) {
			throw new MorriganException(e);
		}
	}

	public void incTrackEndCnt (final IMediaItem item) throws MorriganException {
		try {
			try {
				this.storage.incTrackFinished(new ItemWithFilepath(item, item.getRemoteId()));
			}
			finally {
				this.storage.commitOrRollBack();
				uncache(item);
			}
		}
		catch (final DbException e) {
			throw new MorriganException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Reads.

	private final ConcurrentMap<String, Metadata> cache = new ConcurrentHashMap<>();

	private void uncache (final IMediaItem track) {
		this.cache.remove(track.getRemoteId());
	}

	public Metadata getMetadataProxy (final String remoteId) throws DbException {
		final Metadata cached = this.cache.get(remoteId);
		if (cached != null) return cached;

		if (!this.storage.hasFile(remoteId).isKnown()) {
			this.cache.putIfAbsent(remoteId, Metadata.EMPTY);
			return Metadata.EMPTY;
		}

		final Metadata item = new Metadata(this.storage.getByFile(remoteId));
		while (true) {
			final Metadata previous = this.cache.get(remoteId);
			if (previous == null) {
				if (this.cache.putIfAbsent(remoteId, item) == null) return item;
			}
			else if (previous.isLessThan(item)) {
				if (this.cache.replace(remoteId, previous, item)) return item;
			}
			else {
				return previous;
			}
		}
	}
}
