package com.vaguehope.morrigan.server.model;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IRemoteMixedMediaDb;
import com.vaguehope.morrigan.model.media.MediaListReference;
import com.vaguehope.morrigan.model.media.internal.MediaListReferenceImpl;
import com.vaguehope.morrigan.model.media.internal.db.MediaItemDbConfig;


public final class RemoteMixedMediaDbHelper {

	private RemoteMixedMediaDbHelper () {}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static String getFullPathToMmdb (final Config config, final String fileName) {
		String file = new File(config.getMmdbDir(), fileName).getAbsolutePath();

		if (!file.toLowerCase().endsWith(Config.MMDB_REMOTE_FILE_EXT)) {
			file = file.concat(Config.MMDB_REMOTE_FILE_EXT);
		}

		return file;
	}

	public static IRemoteMixedMediaDb createRemoteMmdb (final Config config, final String mmdbUrl, final String pass) throws MorriganException, URISyntaxException {
		final URI uri = new URI(mmdbUrl);
		// FIXME better naming?
		final String name = mmdbUrl.substring(mmdbUrl.lastIndexOf("/")+1).replace(Config.MMDB_REMOTE_FILE_EXT, "").replace(Config.MMDB_LOCAL_FILE_EXT, "");
		final String file = getFullPathToMmdb(config, uri.getHost() + "_" + name);
		final RemoteHostDetails details = new RemoteHostDetails(uri, pass);
		return RemoteMixedMediaDbFactory.getNew(file, details);
	}

	public static boolean isRemoteMmdbFile (final String filePath) {
		return (filePath.toLowerCase().endsWith(Config.MMDB_REMOTE_FILE_EXT));
	}

	public static List<MediaListReference> getAllRemoteMmdb (final Config config) {
		final ArrayList<MediaListReference> ret = new ArrayList<MediaListReference>();

		final File dir = config.getMmdbDir();
		final File [] files = dir.listFiles();

		// empty dir?
		if (files == null || files.length < 1 ) return ret;

		for (final File file : files) {
			final String absolutePath = file.getAbsolutePath();
			if (isRemoteMmdbFile(absolutePath)) {
				final MediaListReference newItem = new MediaListReferenceImpl(MediaListReference.MediaListType.REMOTEMMDB, absolutePath, getRemoteMmdbTitle(absolutePath));
				ret.add(newItem);
			}
		}

		Collections.sort(ret);

		return ret;
	}

	public static String getRemoteMmdbTitle (final MediaItemDbConfig config) {
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
