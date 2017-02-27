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

	public TestMixedMediaDb () throws DbException, MorriganException {
		super("testdb",
				new MediaItemDbConfig("testdb", null),
				new MixedMediaSqliteLayerOuter(":memory:", true, new MixedMediaItemFactory()));
		read();
	}

	public IMixedMediaItem addTestTrack() throws MorriganException, DbException {
		return addTestTrack(new File(String.format("some_media_file_%s.ext", this.newTrackCounter.getAndIncrement())));
	}

	public IMixedMediaItem addTestTrack (final File file) throws MorriganException, DbException {
		addFile(file);
		final IMixedMediaItem track = getByFile(file); // Workaround so dbRowId is filled in.
		setItemMediaType(track, MediaType.TRACK);
		return track;
	}

}
