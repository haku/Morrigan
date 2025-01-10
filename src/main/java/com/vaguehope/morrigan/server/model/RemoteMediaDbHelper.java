package com.vaguehope.morrigan.server.model;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.ListRef;
import com.vaguehope.morrigan.model.media.ListRefWithTitle;
import com.vaguehope.morrigan.model.media.RemoteMediaDb;
import com.vaguehope.morrigan.model.media.internal.db.MediaDbConfig;


public final class RemoteMediaDbHelper {

	private RemoteMediaDbHelper () {}

	public static String listIdForFilepath(final String filepath) {
		return StringUtils.removeEnd(FilenameUtils.getName(filepath), Config.MMDB_REMOTE_FILE_EXT);
	}

	public static String getFullPathToMmdb (final Config config, final String fileName) {
		String file = new File(config.getMmdbDir(), fileName).getAbsolutePath();

		if (!file.toLowerCase().endsWith(Config.MMDB_REMOTE_FILE_EXT)) {
			file = file.concat(Config.MMDB_REMOTE_FILE_EXT);
		}

		return file;
	}

	public static RemoteMediaDb createRemoteMmdb (final Config config, final String mmdbUrl, final String pass) throws MorriganException, URISyntaxException {
		final URI uri = new URI(mmdbUrl);
		// FIXME better naming?
		final String name = mmdbUrl.substring(mmdbUrl.lastIndexOf("/")+1).replace(Config.MMDB_REMOTE_FILE_EXT, "").replace(Config.MMDB_LOCAL_FILE_EXT, "");
		final String file = getFullPathToMmdb(config, uri.getHost() + "_" + name);
		final RemoteHostDetails details = new RemoteHostDetails(uri, pass);
		return RemoteMediaDbFactory.getNew(file, details);
	}

	public static boolean isRemoteMmdbFile (final String filePath) {
		return (filePath.toLowerCase().endsWith(Config.MMDB_REMOTE_FILE_EXT));
	}

	public static List<ListRefWithTitle> getAllRemoteMmdb (final Config config) {
		final ArrayList<ListRefWithTitle> ret = new ArrayList<>();

		final File dir = config.getMmdbDir();
		final File [] files = dir.listFiles();

		// empty dir?
		if (files == null || files.length < 1 ) return ret;

		for (final File file : files) {
			final String absolutePath = file.getAbsolutePath();
			if (isRemoteMmdbFile(absolutePath)) {
				ret.add(new ListRefWithTitle(
						ListRef.forRemote(listIdForFilepath(file.getName())),
						getRemoteMmdbTitle(file.getName())));
			}
		}

		Collections.sort(ret);

		return ret;
	}

	public static String getRemoteMmdbTitle (final MediaDbConfig config) {
		String ret = getRemoteMmdbTitle(config.getFilePath());

		if (config.getFilter() != null) {
			ret = ret + "{" + config.getFilter() + "}";
		}

		return ret;
	}

	public static String getRemoteMmdbTitle (final String filePath) {
		String ret = filePath;
		int x;

		x = ret.lastIndexOf(File.separator);
		if (x > 0) {
			ret = ret.substring(x+1);
		}

		x = ret.lastIndexOf(Config.MMDB_REMOTE_FILE_EXT);
		if (x > 0) {
			ret = ret.substring(0, x);
		}

		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
