package com.vaguehope.morrigan.model.media.test;

import java.io.File;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.internal.db.MediaItemDbConfig;
import com.vaguehope.morrigan.model.media.internal.db.mmdb.LocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.internal.db.mmdb.MixedMediaItemFactory;
import com.vaguehope.morrigan.model.media.internal.db.mmdb.MixedMediaSqliteLayerOuter;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

public class TestMixedMediaDb extends LocalMixedMediaDb {

	private static final String NAME = "testDb";

	private static final Random RND = new Random(System.currentTimeMillis());
	private static final AtomicInteger newDbCounter = new AtomicInteger(0);
	private static final AtomicInteger newTrackCounter = new AtomicInteger(0);

	public static int getTrackNumber() {
		return newTrackCounter.getAndIncrement();
	}

	public TestMixedMediaDb () throws DbException, MorriganException {
		this(NAME + newDbCounter.getAndIncrement());
	}

	public TestMixedMediaDb (final String name) throws DbException, MorriganException {
		this(name, true);
	}

	public TestMixedMediaDb (final String name, boolean autoCommit) throws DbException, MorriganException {
		super(name,
				new MediaItemDbConfig(name, null),
				new MixedMediaSqliteLayerOuter(
						"file:" + name + "?mode=memory&cache=shared",
						autoCommit, new MixedMediaItemFactory()));
		read();
	}

	public IMixedMediaItem addTestTrack() throws MorriganException, DbException {
		final int n = getTrackNumber();
		return addTestTrack(new File(String.format("some_media_file_%s.ext", n)),
				BigInteger.TEN.add(BigInteger.valueOf(n)));
	}

	public IMixedMediaItem addTestTrack (final File file) throws MorriganException, DbException {
		return addTestTrack(file, new BigInteger(128, RND));
	}

	public IMixedMediaItem addTestTrack (final BigInteger hashCode) throws MorriganException, DbException {
		return addTestTrack(new File(String.format("some_media_file_%s.ext", getTrackNumber())), hashCode);
	}

	public IMixedMediaItem addTestTrack (final File file, final BigInteger hashCode) throws MorriganException, DbException {
		addFile(file);
		final IMixedMediaItem track = getByFile(file); // Workaround so dbRowId is filled in.
		setItemMediaType(track, MediaType.TRACK);
		setItemHashCode(track, hashCode);

		final Date lastPlayed = new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(RND.nextInt(144000)));
		setTrackDateLastPlayed(track, lastPlayed);

		return track;
	}

	public void printContent(final String prefix) {
		System.out.println(prefix + ": TestDb " + getListName() + " has " + getCount() + " items:");
		final List<IMixedMediaItem> items = getMediaItems();
		for (final IMixedMediaItem i :  items) {
			System.out.print(i.isMissing() ? "M" : "-");
			System.out.print(i.isEnabled() ? "-" : "D");
			System.out.print(" ");
			System.out.println(i.getFilepath());
		}
	}

}
