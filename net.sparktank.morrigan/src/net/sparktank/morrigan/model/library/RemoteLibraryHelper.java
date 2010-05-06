package net.sparktank.morrigan.model.library;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import net.sparktank.morrigan.config.Config;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.MediaListFactory;
import net.sparktank.morrigan.model.explorer.MediaExplorerItem;

public class RemoteLibraryHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static String getFullPathToLib (String fileName) {
		File libDir = Config.getLibDir();
		String libFile = libDir.getPath() + File.separator + fileName;
		
		if (!libFile.toLowerCase().endsWith(Config.LIB_REMOTE_FILE_EXT)) {
			libFile = libFile.concat(Config.LIB_REMOTE_FILE_EXT);
		}
		
		return libFile;
	}
	
	public static RemoteMediaLibrary createRemoteLib (String libUrl) throws MorriganException, MalformedURLException {
		URL url = new URL(libUrl);
		// FIXME better naming?
		String name = libUrl.substring(libUrl.lastIndexOf("/")+1).replace(Config.LIB_REMOTE_FILE_EXT, "").replace(Config.LIB_LOCAL_FILE_EXT, "");
		String file = getFullPathToLib(url.getHost() + "_" + url.getPort() + "_" + name);
		RemoteMediaLibrary lib = MediaListFactory.makeRemoteMediaLibrary(getLibraryTitle(file), url, file);
		return lib;
	}
	
	public static boolean isLibFile (String filePath) {
		return (filePath.toLowerCase().endsWith(Config.LIB_REMOTE_FILE_EXT));
	}
	
	public static ArrayList<MediaExplorerItem> getAllRemoteLibraries () {
		ArrayList<MediaExplorerItem> ret = new ArrayList<MediaExplorerItem>();
		
		File libDir = Config.getLibDir();
		File [] libFiles = libDir.listFiles();
		
		// empty dir?
		if (libFiles == null || libFiles.length < 1 ) return ret;
		
		for (File file : libFiles) {
			if (isLibFile(file.getAbsolutePath())) {
				MediaExplorerItem newItem = new MediaExplorerItem(MediaExplorerItem.ItemType.REMOTELIBRARY);
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
		
		x = ret.lastIndexOf(Config.LIB_REMOTE_FILE_EXT);
		if (x > 0) {
			ret = ret.substring(0, x);
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
