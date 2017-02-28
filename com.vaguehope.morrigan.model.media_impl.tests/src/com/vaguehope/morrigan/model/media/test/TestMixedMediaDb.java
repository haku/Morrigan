package com.vaguehope.morrigan.model.media.test;

import java.io.File;
import java.math.BigInteger;
import java.util.Random;
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

	private static final Random RND = new Random(System.currentTimeMillis());
	private static final AtomicInteger newDbCounter = new AtomicInteger(0);

	private final AtomicInteger newTrackCounter = new AtomicInteger(0);

	public TestMixedMediaDb () throws DbException, MorriganException {
		this("testDb");
	}

	public TestMixedMediaDb (final String name) throws DbException, MorriganException {
		super(name,
				new MediaItemDbConfig(name, null),
				new MixedMediaSqliteLayerOuter(
						"file:testdb" + newDbCounter.getAndIncrement() + "?mode=memory&cache=shared",
						true, new MixedMediaItemFactory()));
		read();
	}

	public IMixedMediaItem addTestTrack() throws MorriganException, DbException {
		final int n = this.newTrackCounter.getAndIncrement();
		return addTestTrack(new File(String.format("some_media_file_%s.ext", n)),
				BigInteger.TEN.add(BigInteger.valueOf(n)));
	}

	public IMixedMediaItem addTestTrack (final File file) throws MorriganException, DbException {
		return addTestTrack(file, new BigInteger(8, RND));
	}

	public IMixedMediaItem addTestTrack (final BigInteger hashCode) throws MorriganException, DbException {
		return addTestTrack(new File(String.format("some_media_file_%s.ext", this.newTrackCounter.getAndIncrement())), hashCode);
	}

	public IMixedMediaItem addTestTrack (final File file, final BigInteger hashCode) throws MorriganException, DbException {
		addFile(file);
		final IMixedMediaItem track = getByFile(file); // Workaround so dbRowId is filled in.
		setItemMediaType(track, MediaType.TRACK);
		setItemHashCode(track, hashCode);
		return track;
	}

}
