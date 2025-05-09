package com.vaguehope.morrigan.model.media.internal;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import com.vaguehope.morrigan.model.media.MediaItem;

public class CoverArtHelper {

	private static final String[] DIR_FILE_NAMES = new String[] { "folder", "cover", "album" };

	private CoverArtHelper () {
		throw new AssertionError();
	}

	public static File findCoverArt (final MediaItem item) {
		return findCoverArt(new File(item.getFilepath()));
	}

	public static File findCoverArt (final File file) {
		final File dir = file.isDirectory() ? file : file.getParentFile();

		final String[] imgNames = dir.list(ImgFilenameFilter.INSTANCE);
		if (imgNames == null || imgNames.length < 1) return null;
		Arrays.sort(imgNames);

		final String baseName = file.isFile() ? fileBaseName(file) : null;

		// Same name but with different extension.
		if (baseName != null) {
			for (final String imgName : imgNames) {
				if (fileBaseName(imgName).equals(baseName)) return new File(dir, imgName);
			}
		}

		// Make lower case names without extensions of all the images.
		final String[] lcaseImgBaseNames = new String[imgNames.length];
		for (int i = 0; i < imgNames.length; i++) {
			lcaseImgBaseNames[i] = fileBaseName(imgNames[i].toLowerCase(Locale.UK));
		}

		if (baseName != null) {
			final String lcaseBaseName = baseName.toLowerCase(Locale.UK);
			// Same name but with different case and extension.
			for (int i = 0; i < imgNames.length; i++) {
				if (lcaseImgBaseNames[i].equals(lcaseBaseName)) return new File(dir, imgNames[i]);
			}
			// Image starts with the same name but with different case.
			for (int i = 0; i < imgNames.length; i++) {
				if (lcaseImgBaseNames[i].startsWith(lcaseBaseName)) return new File(dir, imgNames[i]);
			}
		}

		// Conventional name for entire directory.
		for (final String name : DIR_FILE_NAMES) {
			for (int i = 0; i < imgNames.length; i++) {
				if (lcaseImgBaseNames[i].startsWith(name)) return new File(dir, imgNames[i]);
			}
		}

		return null;
	}

	public static File findCoverArt (final Collection<? extends MediaItem> possiblePics) {
		if (possiblePics instanceof List) return findCoverArt((List<? extends MediaItem>) possiblePics);
		return findCoverArt(new ArrayList<>(possiblePics));
	}

	public static File findCoverArt (final List<? extends MediaItem> possiblePics) {
		if (possiblePics == null || possiblePics.size() < 1) return null;

		final List<String> paths = new ArrayList<>(possiblePics.size());
		for (final MediaItem possiblePic : possiblePics) {
			paths.add(possiblePic.getFilepath());
		}

		final List<String> lcaseBaseNames = new ArrayList<>(possiblePics.size());
		for (final String path : paths) {
			lcaseBaseNames.add(fileBaseName(new File(path).getName()).toLowerCase(Locale.UK));
		}

		// Conventional name for entire directory.
		for (final String name : DIR_FILE_NAMES) {
			for (int i = 0; i < paths.size(); i++) {
				if (lcaseBaseNames.get(i).startsWith(name)) return new File(paths.get(i));
			}
		}

		return new File(paths.get(0));
	}

	private static String fileBaseName (final File file) {
		return fileBaseName(file.getName());
	}

	private static String fileBaseName (final String name) {
		final int extStart = name.lastIndexOf('.');
		if (extStart < 1) return name;
		return name.substring(0, extStart);
	}

	protected static String fileExt (final String name) {
		final int extStart = name.lastIndexOf('.');
		if (extStart < 1) return "";
		return name.substring(extStart + 1);
	}

	private static enum ImgFilenameFilter implements FilenameFilter {
		INSTANCE;

		private static final String[] IMG_EXT = new String[] { "jpg", "jpeg", "gif", "png" };

		@Override
		public boolean accept (final File dir, final String name) {
			if (name == null) return false;
			final String lcaseExt = fileExt(name).toLowerCase(Locale.UK);
			for (final String ext : IMG_EXT) {
				if (lcaseExt.equals(ext)) return true;
			}
			return false;
		}

	}

}
