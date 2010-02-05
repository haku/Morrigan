package net.sparktank.morrigan.library;

import java.io.File;
import java.util.ArrayList;

import net.sparktank.morrigan.config.Config;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.MediaListFactory;
import net.sparktank.morrigan.model.media.MediaLibrary;
import net.sparktank.morrigan.model.ui.MediaExplorerItem;

public class LibraryHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static final LibraryHelper INSTANCE = new LibraryHelper();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private String getPathForNewLibrary (String libName) {
		File libDir = Config.getLibDir();
		String libFile = libDir.getPath() + File.separator + libName + Config.LIB_FILE_EXT;
		return libFile;
	}
	
	public MediaLibrary createLib (String libName) throws MorriganException {
		String plFile = getPathForNewLibrary(libName);
		MediaLibrary lib = MediaListFactory.makeMediaLibrary(getLibraryTitle(plFile), plFile);
//		lib.read();
		return lib;
	}
	
	private boolean isLibFile (String filePath) {
		return (filePath.toLowerCase().endsWith(Config.LIB_FILE_EXT));
	}
	
	public ArrayList<MediaExplorerItem> getAllLibraries () {
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
	
	public static String getLibraryTitle (String libFilePath) {
		int x = libFilePath.lastIndexOf(File.separator);
		if (x > 0) {
			return libFilePath.substring(x+1);
		} else {
			return libFilePath;
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
