package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.ILocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.MediaListReference;
import com.vaguehope.morrigan.model.media.MediaListReference.MediaListType;
import com.vaguehope.morrigan.model.media.internal.MediaListReferenceImpl;
import com.vaguehope.morrigan.model.media.internal.db.MediaItemDbConfig;
import com.vaguehope.sqlitewrapper.DbException;

public final class LocalMixedMediaDbHelper {

	private LocalMixedMediaDbHelper () {}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static String getFullPathToMmdb (final Config config, final String fileName) {
		String file = new File(config.getMmdbDir(), fileName).getAbsolutePath();

		if (!file.toLowerCase().endsWith(Config.MMDB_LOCAL_FILE_EXT)) {
			file = file.concat(Config.MMDB_LOCAL_FILE_EXT);
		}

		return file;
	}

	public static ILocalMixedMediaDb createMmdb (final Config config, final String name) throws MorriganException {
		final String file = getFullPathToMmdb(config, name);
		try {
			return LocalMixedMediaDbFactory.getMain(file);
		}
		catch (final DbException e) {
			throw new MorriganException(e);
		}
	}

	public static boolean isMmdbFile (final Config config, final String filePath) {
		if (filePath.toLowerCase().endsWith(Config.MMDB_LOCAL_FILE_EXT)) {
			File file = new File(filePath);
			if (file.exists()) return true;
			file = new File(config.getMmdbDir(), filePath);
			if (file.exists()) return true;
		}
		return false;
	}

	public static List<MediaListReference> getAllMmdb (final Config config) {
		final List<MediaListReference> ret = new ArrayList<MediaListReference>();

		final File dir = config.getMmdbDir();
		final File[] files = dir.listFiles();

		// empty dir?
		if (files == null || files.length < 1) return ret;

		for (final File file : files) {
			final String absolutePath = file.getAbsolutePath();
			if (isMmdbFile(config, absolutePath)) {
				final MediaListReference newItem = new MediaListReferenceImpl(MediaListType.LOCALMMDB, absolutePath, getMmdbFileTitle(absolutePath));
				ret.add(newItem);
			}
		}

		Collections.sort(ret);

		return ret;
	}

	public static String getMmdbTitle (final MediaItemDbConfig config) {
		String ret = getMmdbFileTitle(config.getFilePath());

		if (config.getFilter() != null) {
			ret = ret + "{" + config.getFilter() + "}";
		}

		return ret;
	}

	private static String getMmdbFileTitle (final String filePath) {
		String ret = filePath;
		int x;

		x = ret.lastIndexOf(File.separator);
		if (x > 0) {
			ret = ret.substring(x + 1);
		}

		x = ret.lastIndexOf(Config.MMDB_LOCAL_FILE_EXT);
		if (x > 0) {
			ret = ret.substring(0, x);
		}

		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
