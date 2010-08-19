package net.sparktank.morrigan.model.tracks.library.local;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import net.sparktank.morrigan.config.Config;
import net.sparktank.morrigan.model.explorer.MediaExplorerItem;
import net.sparktank.sqlitewrapper.DbException;

public class LocalLibraryHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static String getFullPathToLib (String fileName) {
		File libDir = Config.getLibDir();
		String libFile = libDir.getPath() + File.separator + fileName;
		
		if (!libFile.toLowerCase().endsWith(Config.LIB_LOCAL_FILE_EXT)) {
			libFile = libFile.concat(Config.LIB_LOCAL_FILE_EXT);
		}
		
		return libFile;
	}
	
	public static LocalMediaLibrary createLib (String libName) throws DbException {
		String plFile = getFullPathToLib(libName);
		LocalMediaLibrary lib = LocalMediaLibrary.FACTORY.manufacture(plFile);
		return lib;
	}
	
	public static boolean isLibFile (String filePath) {
		return (filePath.toLowerCase().endsWith(Config.LIB_LOCAL_FILE_EXT));
	}
	
	public static ArrayList<MediaExplorerItem> getAllLibraries () {
		ArrayList<MediaExplorerItem> ret = new ArrayList<MediaExplorerItem>();
		
		File libDir = Config.getLibDir();
		File [] libFiles = libDir.listFiles();
		
		// empty dir?
		if (libFiles == null || libFiles.length < 1 ) return ret;
		
		for (File file : libFiles) {
			if (isLibFile(file.getAbsolutePath())) {
				MediaExplorerItem newItem = new MediaExplorerItem(MediaExplorerItem.ItemType.LIBRARY);
				newItem.identifier = file.getAbsolutePath();
				newItem.title = getLibraryTitle(newItem.identifier);
				ret.add(newItem);
			}
		}
		
		Collections.sort(ret);
		
		return ret;
		
	}
	
	public static String getLibraryTitle (String filePath) {
		String ret = filePath;
		int x;
		
		x = ret.lastIndexOf(File.separator);
		if (x > 0) {
			ret = ret.substring(x+1);
		}
		
		x = ret.lastIndexOf(Config.LIB_LOCAL_FILE_EXT);
		if (x > 0) {
			ret = ret.substring(0, x);
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
