package com.vaguehope.morrigan.model.media.test;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.MediaNode;
import com.vaguehope.morrigan.model.media.internal.db.MediaItemDbConfig;
import com.vaguehope.morrigan.model.media.internal.db.mmdb.LocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.internal.db.mmdb.MixedMediaItemFactory;
import com.vaguehope.morrigan.model.media.internal.db.mmdb.MixedMediaSqliteLayerOuter;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.util.MimeType;

public class TestMixedMediaDb extends LocalMixedMediaDb {

	private static final String NAME = "testDb";

	private static final Random RND = new Random(System.currentTimeMillis());
	private static final AtomicInteger newDbCounter = new AtomicInteger(0);
	private static final AtomicInteger newTrackCounter = new AtomicInteger(0);

	private final List<MediaNode> nodes = new ArrayList<>();

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
		super(name, new MediaItemDbConfig(name, null));
		final MixedMediaItemFactory itemFactory = new MixedMediaItemFactory(this);
		setDbLayer(new MixedMediaSqliteLayerOuter("file:" + name + "?mode=memory&cache=shared", autoCommit, itemFactory));
		read();
	}

	@Override
	public String getListId() {
		return getListName();
	}

	public void addNode(final MediaNode node) {
		this.nodes.add(node);
	}
	@Override
	public boolean hasNodes() {
		return this.nodes.size() > 0;
	}
	@Override
	public List<MediaNode> getSubNodes() throws MorriganException {
		return this.nodes;
	}

	public IMediaItem addTestTrack() throws MorriganException, DbException {
		return addTestTrack(MimeType.MP3);
	}

	public IMediaItem addTestTrack(final MimeType mimeType) throws MorriganException, DbException {
		final int n = getTrackNumber();
		return addTestTrack(new File(String.format("some_media_file_%s." + mimeType.getExt(), n)),
				BigInteger.TEN.add(BigInteger.valueOf(2 * n)),
				BigInteger.TEN.add(BigInteger.valueOf((2 * n) + 1)));
	}

	public IMediaItem addTestTrack (final File file) throws MorriganException, DbException {
		return addTestTrack(file, new BigInteger(128, RND), new BigInteger(128, RND));
	}

	public IMediaItem addTestTrack (final BigInteger md5, final BigInteger sha1) throws MorriganException, DbException {
		return addTestTrack(new File(String.format("some_media_file_%s.ext", getTrackNumber())), md5, sha1);
	}

	public IMediaItem addTestTrack (final File file, final BigInteger md5, final BigInteger sha1) throws MorriganException, DbException {
		final long lastPlayed = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(RND.nextInt(144000));
		return addTestTrack(file, md5, sha1, lastPlayed, System.currentTimeMillis());
	}

	public IMediaItem addTestTrack (final File file, final BigInteger md5, final BigInteger sha1, final long lastPlayed, final long dateAdded) throws MorriganException, DbException {
		addFile(MediaType.TRACK, file);
		final IMediaItem track = getByFile(file); // Workaround so dbRowId is filled in.
		setItemMd5(track, md5);
		setItemSha1(track, sha1);
		setTrackDateLastPlayed(track, new Date(lastPlayed));
		setItemDateAdded(track, new Date(dateAdded));
		return track;
	}

	public void printContent(final String prefix) {
		System.out.println(prefix + ": TestDb " + getListName() + " has " + size() + " items:");
		final List<IMediaItem> items = getMediaItems();
		for (final IMediaItem i :  items) {
			System.out.print(i.isMissing() ? "M" : "-");
			System.out.print(i.isEnabled() ? "-" : "D");
			System.out.print(" ");
			System.out.println(i.getFilepath());
		}
	}

}
