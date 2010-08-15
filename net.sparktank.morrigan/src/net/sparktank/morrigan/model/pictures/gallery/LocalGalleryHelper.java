package net.sparktank.morrigan.model.pictures.gallery;

import java.io.File;
import java.util.ArrayList;

import net.sparktank.morrigan.config.Config;
import net.sparktank.morrigan.model.explorer.MediaExplorerItem;
import net.sparktank.sqlitewrapper.DbException;

public class LocalGalleryHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static String getFullPathToGallery (String fileName) {
		File dir = Config.getGalleryDir();
		String file = dir.getPath() + File.separator + fileName;
		
		if (!file.toLowerCase().endsWith(Config.GALLERY_LOCAL_FILE_EXT)) {
			file = file.concat(Config.GALLERY_LOCAL_FILE_EXT);
		}
		
		return file;
	}
	
	public static LocalGallery createGallery (String name) throws DbException {
		String file = getFullPathToGallery(name);
		LocalGallery l = LocalGallery.FACTORY.manufacture(file);
		return l;
	}
	
	public static boolean isGalleryFile (String filePath) {
		return (filePath.toLowerCase().endsWith(Config.GALLERY_LOCAL_FILE_EXT));
	}
	
	public static ArrayList<MediaExplorerItem> getAllGalleries () {
		ArrayList<MediaExplorerItem> ret = new ArrayList<MediaExplorerItem>();
		
		File dir = Config.getGalleryDir();
		File [] files = dir.listFiles();
		
		// empty dir?
		if (files == null || files.length < 1 ) return ret;
		
		for (File file : files) {
			if (isGalleryFile(file.getAbsolutePath())) {
				MediaExplorerItem newItem = new MediaExplorerItem(MediaExplorerItem.ItemType.LOCALGALLERY);
				newItem.identifier = file.getAbsolutePath();
				newItem.title = getGalleryTitle(newItem.identifier);
				ret.add(newItem);
			}
		}
		
		return ret;
	}
	
	public static String getGalleryTitle (String filePath) {
		String ret = filePath;
		int x;
		
		x = ret.lastIndexOf(File.separator);
		if (x > 0) {
			ret = ret.substring(x+1);
		}
		
		x = ret.lastIndexOf(Config.GALLERY_LOCAL_FILE_EXT);
		if (x > 0) {
			ret = ret.substring(0, x);
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
