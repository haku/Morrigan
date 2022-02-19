package com.vaguehope.morrigan.dlna.content;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.vaguehope.morrigan.dlna.httpserver.FileLocator;
import com.vaguehope.morrigan.dlna.util.Cache;
import com.vaguehope.morrigan.dlna.util.HashHelper;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.MediaAlbum;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.MediaListReference;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

public class MediaFileLocator implements FileLocator {

	private final MediaFactory mediaFactory;
	private final DbHelper dbHelper;

	private final Map<String, File> files = new ConcurrentHashMap<String, File>();

	public MediaFileLocator (final MediaFactory mediaFactory) {
		this.mediaFactory = mediaFactory;
		this.dbHelper = new DbHelper(mediaFactory);
	}

	/**
	 * For fall-back.
	 */
	public String fileId (final File file) {
		final String id = HashHelper.sha1(file.getAbsolutePath()) + "-" + file.getName().replaceAll("[^a-zA-Z0-9]", "_");
		this.files.put(id, file);
		return id;
	}

	public String mediaItemId (final MediaListReference mlr, final IMixedMediaItem mi) {
		return String.format("%s/item/%s/%s", mlrRef(mlr), encodeFilepath(mi), mi.getHashcode().toString(16));
	}

	public String mediaItemArtId (final MediaListReference mlr, final IMixedMediaItem mi) {
		return String.format("%s/item/%s/%s/art", mlrRef(mlr), encodeFilepath(mi), mi.getHashcode().toString(16));
	}

	public String albumArtId (final MediaListReference mlr, final MediaAlbum album) {
		return String.format("%s/album/%s/art", mlrRef(mlr), encodeName(album));
	}

	@Override
	public File idToFile (final String id) throws IOException {
		final File mapFile = this.files.get(id);
		if (mapFile != null) return mapFile;

		try {
			final File file = idToFileUnsafe(id);
			if (file == null || !file.exists()) return null;
			return file;
		}
		catch (final IllegalArgumentException e) {
			throw new IllegalArgumentException("Failed to parse ID: " + id, e);
		}
		catch (final MorriganException e) {
			throw new IOException("Failed to resolve ID: " + id, e);
		}
		catch (final DbException e) {
			throw new IOException("Failed to resolve ID: " + id, e);
		}
	}

	public File idToFileUnsafe (final String id) throws DbException, MorriganException {
		final String[] parts = id.split("/");
		if (parts.length < 3) throw new IllegalArgumentException("Need at least 3 parts: " + id);

		final MediaListReference mlr = derefMlr(parts[0]);
		if (mlr == null) throw new IllegalArgumentException("Invalid ref in ID: " + id);
		final IMixedMediaDb db = this.dbHelper.mediaListReferenceToDb(mlr);

		if ("item".equals(parts[1])) {
			if (parts.length < 4) throw new IllegalArgumentException("Need at least 4 parts: " + id);

			final String filepath = decodeString(parts[2]);
			final IMixedMediaItem item = db.hasFile(filepath).isKnown() ? db.getByFile(filepath) : db.getByHashcode(new BigInteger(parts[3], 16));

			if (parts.length == 4) {
				return new File(item.getFilepath());
			}
			else if ("art".equals(parts[4])) {
				return item.findCoverArt();
			}

			throw new IllegalArgumentException("Invalid item ID: " + id);
		}
		else if ("album".equals(parts[1])) {
			if (parts.length < 4) throw new IllegalArgumentException("Need at least 4 parts: " + id);

			if ("art".equals(parts[3])) {
				return db.findAlbumCoverArt(db.getAlbum(decodeString(parts[2])));
			}

			throw new IllegalArgumentException("Invalid album ID: " + id);
		}
		else {
			throw new IllegalArgumentException("Invalid type in ID: " + id);
		}
	}

	private static String mlrRef (final MediaListReference mlr) {
		try {
			return URLEncoder.encode(filenameFromPath(mlr.getIdentifier()), "UTF-8");
		}
		catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	private final Cache<String, MediaListReference> mlrCache = new Cache<String, MediaListReference>(10);

	private MediaListReference derefMlr (final String encodedMlrIdentifier) {
		final MediaListReference cached = this.mlrCache.getFresh(encodedMlrIdentifier, 60, TimeUnit.SECONDS);
		if (cached != null) return cached;

		for (final MediaListReference mlr : this.mediaFactory.getAllLocalMixedMediaDbs()) {
			if (encodedMlrIdentifier.equals(filenameFromPath(mlr.getIdentifier()))) {
				this.mlrCache.put(encodedMlrIdentifier, mlr);
				return mlr;
			}
		}
		return null;
	}

	private static String encodeFilepath (final IMixedMediaItem mi) {
		try {
			return URLEncoder.encode(mi.getFilepath(), "UTF-8");
		}
		catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String encodeName (final MediaAlbum album) {
		try {
			return URLEncoder.encode(album.getName(), "UTF-8");
		}
		catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String decodeString (final String path) {
		try {
			return URLDecoder.decode(path, "UTF-8");
		}
		catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String filenameFromPath (final String path) {
		return path.substring(path.lastIndexOf(File.separator) + 1);
	}

}
