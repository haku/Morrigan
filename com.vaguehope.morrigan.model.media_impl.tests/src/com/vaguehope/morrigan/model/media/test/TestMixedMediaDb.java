package com.vaguehope.morrigan.model.media.test;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.internal.db.MediaItemDbConfig;
import com.vaguehope.morrigan.model.media.internal.db.mmdb.LocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.internal.db.mmdb.MixedMediaItemFactory;
import com.vaguehope.morrigan.model.media.internal.db.mmdb.MixedMediaSqliteLayerOuter;
import com.vaguehope.sqlitewrapper.DbException;

public class TestMixedMediaDb extends LocalMixedMediaDb {

	private final AtomicInteger newTrackCounter = new AtomicInteger(0);

	public TestMixedMediaDb (final File dbFile) throws DbException, MorriganException {
		super(dbFile.getName(),
				new MediaItemDbConfig(dbFile.getAbsolutePath(), null),
				new MixedMediaSqliteLayerOuter(dbFile.getAbsolutePath(), true, new MixedMediaItemFactory()));
		read();
	}

	public IMixedMediaItem addTestTrack() throws MorriganException, DbException {
		final IMixedMediaItem track = addFile(new File(String.format("some_media_file_%s.ext", this.newTrackCounter.getAndIncrement())));
		setItemMediaType(track, MediaType.TRACK);
		return track;
	}

}
