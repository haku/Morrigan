package com.vaguehope.morrigan.osgiconsole;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.ILocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackDb;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.model.media.IRemoteMixedMediaDb;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.MediaListReference;
import com.vaguehope.morrigan.model.media.MediaListReference.MediaListType;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.server.model.RemoteMixedMediaDbFactory;
import com.vaguehope.morrigan.server.model.RemoteMixedMediaDbHelper;
import com.vaguehope.morrigan.util.StringHelper;
import com.vaguehope.sqlitewrapper.DbException;

public class CliHelper {

	private final MediaFactory mediaFactory;

	public CliHelper (final MediaFactory mediaFactory) {
		this.mediaFactory = mediaFactory;
	}

	public void checkArgs (final List<String> args, final int expectedCount) throws ArgException {
		if (args.size() < expectedCount) {
			throw new ArgException("Not enough arguments.");
		}
	}

	public String argNotBlank (final String arg) throws ArgException {
		if (StringHelper.blank(arg)) {
			throw new ArgException("arg was blank.");
		}
		return arg;
	}

	public int argPositiveInt (final String arg) throws ArgException {
		try {
			final int n = Integer.parseInt(arg);
			if (n < 0) throw new ArgException("Less than 0: " + arg);
			return n;
		}
		catch (final NumberFormatException e) {
			throw new ArgException("Not a number: " + arg);
		}
	}

	public URI argUri (final String arg) throws ArgException {
		try {
			return new URI(argNotBlank(arg));
		}
		catch (URISyntaxException e) {
			throw new ArgException("Invalid URI: " + arg);
		}
	}

	public File argLocalDir (final String arg) throws ArgException {
		final File dir = new File(arg);
		if (!dir.exists()) {
			throw new ArgException("Directory '" + dir.getAbsolutePath() + "' not found.");
		}
		return dir;
	}

	public IMediaTrackList<? extends IMediaTrack> argQ1 (final String arg) throws ArgException, MorriganException {
		final List<PlayItem> lq1Pi = queryForPlayableItems(arg, null, 2);
		if (lq1Pi == null || lq1Pi.size() != 1) {
			throw new ArgException("Query '" + arg + "' did not return only one result.");
		}
		return lq1Pi.get(0).getList();
	}

	public ILocalMixedMediaDb argLocalQ1 (final String arg) throws ArgException, MorriganException {
		final IMediaTrackList<? extends IMediaTrack> ll = argQ1(arg);
		if (!(ll instanceof ILocalMixedMediaDb)) {
			throw new ArgException("DB '" + arg + "' is not a local DB.");
		}
		return (ILocalMixedMediaDb) ll;
	}

	public IRemoteMixedMediaDb argRemoteQ1 (final String arg) throws ArgException, MorriganException {
		final IMediaTrackList<? extends IMediaTrack> ll = argQ1(arg);
		if (!(ll instanceof IRemoteMixedMediaDb)) {
			throw new ArgException("DB '" + arg + "' is not a remote DB.");
		}
		return (IRemoteMixedMediaDb) ll;
	}

	/**
	 * TODO Make this method able to take all sorts if user input.
	 */
	public List<PlayItem> queryForPlayableItems (final String query1, final String query2, final int maxResults) throws MorriganException {
		List<PlayItem> ret = new LinkedList<PlayItem>();

		List<MediaListReference> items = new LinkedList<MediaListReference>();
		List<MediaListReference> matches = new LinkedList<MediaListReference>();

		items.addAll(this.mediaFactory.getAllLocalMixedMediaDbs());
		items.addAll(RemoteMixedMediaDbHelper.getAllRemoteMmdb());

		// First search exact.
		for (MediaListReference i : items) {
			if (i.getTitle().equals(query1)) matches.add(i);
		}

		// Second search case-insensitive, but still exact.
		if (matches.size() < 1) {
			for (MediaListReference i : items) {
				if (i.getTitle().equalsIgnoreCase(query1)) matches.add(i);
			}
		}

		// Third search sub-string.
		if (matches.size() < 1) {
			for (MediaListReference i : items) {
				if (i.getTitle().contains(query1)) matches.add(i);
			}
		}

		// Fourth search sub-string and case-insensitive.
		if (matches.size() < 1) {
			for (MediaListReference i : items) {
				if (i.getTitle().toLowerCase().contains(query1.toLowerCase())) matches.add(i);
			}
		}

		for (MediaListReference explorerItem : matches) {
			if (ret.size() >= maxResults) break;

			/*
			 * FIXME this will load the DB (if its not already loaded), which is excessive if we are
			 * just going to show some search results.
			 */
			IMediaTrackDb<?, ? extends IMediaTrack> db = mediaListReferenceToReadTrackDb(explorerItem);

			if (query2 == null) {
				ret.add(new PlayItem(db, null));
			}
			else {
				List<? extends IMediaTrack> results;
				try {
					results = db.simpleSearch(query2, maxResults);
				}
				catch (DbException e) {
					throw new MorriganException(e);
				}

				for (IMediaTrack result : results) {
					if (ret.size() >= maxResults) break;
					ret.add(new PlayItem(db, result));
				}
			}

		}

		return ret;
	}

	public IMediaTrackDb<?, ? extends IMediaTrack> mediaListReferenceToReadTrackDb (final MediaListReference item) throws MorriganException {
		IMediaTrackDb<?, ? extends IMediaTrack> ret = null;

		if (item.getType() == MediaListReference.MediaListType.LOCALMMDB) {
			ILocalMixedMediaDb mmdb;
			try {
				mmdb = this.mediaFactory.getLocalMixedMediaDb(item.getIdentifier());
			}
			catch (DbException e) {
				throw new MorriganException(e);
			}
			mmdb.read();
			ret = mmdb;
		}
		else if (item.getType() == MediaListType.REMOTEMMDB) {
			IRemoteMixedMediaDb db = RemoteMixedMediaDbFactory.getExisting(item.getIdentifier());
			db.read();
			ret = db;
		}
		else {
			throw new MorriganException("TODO: show " + item.getIdentifier());
		}

		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
