package com.vaguehope.morrigan.model.media.internal.db;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.ListRef;
import com.vaguehope.morrigan.model.media.ListRefWithTitle;
import com.vaguehope.morrigan.model.media.MediaDb;

public final class LocalMediaDbHelper {

	private LocalMediaDbHelper () {}

	public static String listIdForFilepath(final String filepath) {
		return StringUtils.removeEnd(FilenameUtils.getName(filepath), Config.MMDB_LOCAL_FILE_EXT);
	}

	public static String getFullPathToLocalDb (final Config config, final String fileName) {
		String file = new File(config.getMmdbDir(), fileName).getAbsolutePath();
		if (!file.toLowerCase().endsWith(Config.MMDB_LOCAL_FILE_EXT)) {
			file = file.concat(Config.MMDB_LOCAL_FILE_EXT);
		}
		return file;
	}

	public static MediaDb createLocalDb (final Config config, final String name) throws MorriganException {
		final String file = getFullPathToLocalDb(config, name);
		return LocalMediaDbFactory.getMain(file);
	}

	public static boolean isLocalDbFile (final Config config, final String filePath) {
		if (filePath.toLowerCase().endsWith(Config.MMDB_LOCAL_FILE_EXT)) {
			File file = new File(filePath);
			if (file.exists()) return true;
			file = new File(config.getMmdbDir(), filePath);
			if (file.exists()) return true;
		}
		return false;
	}

	public static List<ListRefWithTitle> getAllLocalDbs (final Config config) {
		final File dir = config.getMmdbDir();
		final File[] files = dir.listFiles();
		if (files == null || files.length < 1) return Collections.emptyList();

		final List<ListRefWithTitle> ret = new ArrayList<>();
		for (final File file : files) {
			if (isLocalDbFile(config, file.getAbsolutePath())) {
				ret.add(new ListRefWithTitle(
						ListRef.forLocal(listIdForFilepath(file.getName())),
						getDbFileTitle(file.getName())));
			}
		}

		Collections.sort(ret);
		return ret;
	}

	public static String getDbTitle (final MediaDbConfig config) {
		String ret = getDbFileTitle(config.getFilePath());

		if (config.getFilter() != null) {
			ret = ret + "{" + config.getFilter() + "}";
		}

		return ret;
	}

	private static String getDbFileTitle (final String filePath) {
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
