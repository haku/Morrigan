package net.sparktank.morrigan.library;

import java.io.File;
import java.util.ArrayList;

import net.sparktank.morrigan.config.Config;
import net.sparktank.morrigan.model.ui.MediaExplorerItem;

public class LibraryHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static final LibraryHelper INSTANCE = new LibraryHelper();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
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
				
				int x = newItem.identifier.lastIndexOf(File.separator);
				if (x > 0) {
					newItem.title = newItem.identifier.substring(x+1);
				} else {
					newItem.title = newItem.identifier;
				}
				
				ret.add(newItem);
			}
		}
		
		return ret;
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
