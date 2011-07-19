package net.sparktank.morrigan.model.media.internal.db.mmdb;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sparktank.morrigan.config.Config;
import net.sparktank.morrigan.model.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.ILocalMixedMediaDb;
import net.sparktank.morrigan.model.media.MediaListReference;
import net.sparktank.morrigan.model.media.MediaListReference.MediaListType;
import net.sparktank.morrigan.model.media.internal.MediaListReferenceImpl;
import net.sparktank.sqlitewrapper.DbException;

public class LocalMixedMediaDbHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static String getFullPathToMmdb (String fileName) {
		File dir = Config.getMmdbDir();
		String file = dir.getPath() + File.separator + fileName;
		
		if (!file.toLowerCase().endsWith(Config.MMDB_LOCAL_FILE_EXT)) {
			file = file.concat(Config.MMDB_LOCAL_FILE_EXT);
		}
		
		return file;
	}
	
	public static ILocalMixedMediaDb createMmdb (String name) throws MorriganException {
		String file = getFullPathToMmdb(name);
		ILocalMixedMediaDb l;
		try {
			l = LocalMixedMediaDbFactory.getMain(file);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
		return l;
	}
	
	public static boolean isMmdbFile (String filePath) {
		if (filePath.toLowerCase().endsWith(Config.MMDB_LOCAL_FILE_EXT)) {
			File file = new File(filePath);
			if (file.exists()) return true;
			file = new File(Config.getMmdbDir(), filePath);
			if (file.exists())  return true;
		}
		return false;
	}
	
	public static List<MediaListReference> getAllMmdb () {
		List<MediaListReference> ret = new ArrayList<MediaListReference>();
		
		File dir = Config.getMmdbDir();
		File [] files = dir.listFiles();
		
		// empty dir?
		if (files == null || files.length < 1 ) return ret;
		
		for (File file : files) {
			String absolutePath = file.getAbsolutePath();
			if (isMmdbFile(absolutePath)) {
				MediaListReference newItem = new MediaListReferenceImpl(MediaListType.LOCALMMDB, absolutePath, getMmdbTitle(absolutePath));
				ret.add(newItem);
			}
		}
		
		Collections.sort(ret);
		
		return ret;
	}
	
	public static String getMmdbTitle (String filePath) {
		String ret = filePath;
		int x;
		
		x = ret.lastIndexOf(File.separator);
		if (x > 0) {
			ret = ret.substring(x+1);
		}
		
		x = ret.lastIndexOf(Config.MMDB_LOCAL_FILE_EXT);
		if (x > 0) {
			ret = ret.substring(0, x);
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
