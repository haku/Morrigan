package net.sparktank.morrigan.server.model;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import net.sparktank.morrigan.config.Config;
import net.sparktank.morrigan.model.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.IRemoteMixedMediaDb;
import net.sparktank.morrigan.model.media.MediaListReference;
import net.sparktank.morrigan.model.media.internal.MediaListReferenceImpl;

public class RemoteMixedMediaDbHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static String getFullPathToMmdb (String fileName) {
		File dir = Config.getMmdbDir();
		String file = dir.getPath() + File.separator + fileName;
		
		if (!file.toLowerCase().endsWith(Config.MMDB_REMOTE_FILE_EXT)) {
			file = file.concat(Config.MMDB_REMOTE_FILE_EXT);
		}
		
		return file;
	}
	
	public static IRemoteMixedMediaDb createRemoteMmdb (String mmdbUrl) throws MorriganException, MalformedURLException {
		URL url = new URL(mmdbUrl);
		// FIXME better naming?
		String name = mmdbUrl.substring(mmdbUrl.lastIndexOf("/")+1).replace(Config.MMDB_REMOTE_FILE_EXT, "").replace(Config.MMDB_LOCAL_FILE_EXT, "");
		String file = getFullPathToMmdb(url.getHost() + "_" + url.getPort() + "_" + name);
		IRemoteMixedMediaDb db = RemoteMixedMediaDb.FACTORY.manufacture(file, url);
		return db;
	}
	
	public static boolean isRemoteMmdbFile (String filePath) {
		return (filePath.toLowerCase().endsWith(Config.MMDB_REMOTE_FILE_EXT));
	}
	
	public static ArrayList<MediaListReference> getAllRemoteMmdb () {
		ArrayList<MediaListReference> ret = new ArrayList<MediaListReference>();
		
		File dir = Config.getMmdbDir();
		File [] files = dir.listFiles();
		
		// empty dir?
		if (files == null || files.length < 1 ) return ret;
		
		for (File file : files) {
			String absolutePath = file.getAbsolutePath();
			if (isRemoteMmdbFile(absolutePath)) {
				MediaListReference newItem = new MediaListReferenceImpl(MediaListReference.MediaListType.REMOTEMMDB, absolutePath, getRemoteMmdbTitle(absolutePath));
				ret.add(newItem);
			}
		}
		
		Collections.sort(ret);
		
		return ret;
	}
	
	public static String getRemoteMmdbTitle (String filePath) {
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
