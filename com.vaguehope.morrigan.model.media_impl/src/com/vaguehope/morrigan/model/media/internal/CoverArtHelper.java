package com.vaguehope.morrigan.model.media.internal;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Locale;

import com.vaguehope.morrigan.model.media.IMediaItem;

public class CoverArtHelper {

	private static final String[] DIR_FILE_NAMES = new String[] { "folder", "cover", "album" };

	private CoverArtHelper () {
		throw new AssertionError();
	}

	public static File findCoverArt (final IMediaItem item) {
		return findCoverArt(new File(item.getFilepath()));
	}

	public static File findCoverArt (final File file) {
		final File dir = file.isDirectory() ? file : file.getParentFile();

		final String baseName = file.isFile() ? fileBaseName(file) : null;
		final String[] imgNames = dir.list(ImgFilenameFilter.INSTANCE);
		Arrays.sort(imgNames);

		if (imgNames.length < 1) return null;

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
