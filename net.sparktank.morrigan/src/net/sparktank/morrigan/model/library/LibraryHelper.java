package net.sparktank.morrigan.model.library;

import java.io.File;
import java.util.ArrayList;

import net.sparktank.morrigan.config.Config;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.gui.model.MediaExplorerItem;
import net.sparktank.morrigan.model.MediaListFactory;

public class LibraryHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static String getFullPathToLib (String fileName) {
		File libDir = Config.getLibDir();
		String libFile = libDir.getPath() + File.separator + fileName;
		
		if (!libFile.toLowerCase().endsWith(Config.LIB_FILE_EXT)) {
			libFile = libFile.concat(Config.LIB_FILE_EXT);
		}
		
		return libFile;
	}
	
	public static MediaLibrary createLib (String libName) throws MorriganException {
		String plFile = getFullPathToLib(libName);
		MediaLibrary lib = MediaListFactory.makeMediaLibrary(getLibraryTitle(plFile), plFile);
		return lib;
	}
	
	public static boolean isLibFile (String filePath) {
		return (filePath.toLowerCase().endsWith(Config.LIB_FILE_EXT));
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
		
		return ret;
		
	}
	
	public static String getLibraryTitle (String filePath) {
		String ret = filePath;
		int x;
		
		x = ret.lastIndexOf(File.separator);
		if (x > 0) {
			ret = ret.substring(x+1);
		}
		
		x = ret.lastIndexOf(Config.LIB_FILE_EXT);
		if (x > 0) {
			ret = ret.substring(0, x);
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
