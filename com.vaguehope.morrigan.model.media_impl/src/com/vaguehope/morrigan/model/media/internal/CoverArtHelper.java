package com.vaguehope.morrigan.model.media.internal;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Locale;

import com.vaguehope.morrigan.model.media.IMediaItem;

public class CoverArtHelper {

	private static final String[] DIR_FILE_NAMES = new String[] { "folder", "cover", "album" };

	private CoverArtHelper () {
		throw new AssertionError();
	}

	public static File findCoverArt (final IMediaItem item) {
		final File file = new File(item.getFilepath());
		final File dir = file.getParentFile();
		final String baseName = fileBaseName(file);
		final String[] imgNames = dir.list(ImgFilenameFilter.INSTANCE);

		for (final String imgName : imgNames) {
			if (imgName.startsWith(baseName)) return new File(dir, imgName);
		}

		final String lcaseBaseName = baseName.toLowerCase(Locale.UK);
		final String [] lcaseImgNames = new String[imgNames.length];
		for (int i = 0; i < imgNames.length; i++) {
			lcaseImgNames[i] = imgNames[i].toLowerCase(Locale.UK);
		}

		for (int i = 0; i < imgNames.length; i++) {
			if (lcaseImgNames[i].startsWith(lcaseBaseName)) return new File(dir, imgNames[i]);
		}

		for (int i = 0; i < imgNames.length; i++) {
			for (final String name : DIR_FILE_NAMES) {
				if (lcaseImgNames[i].startsWith(name)) return new File(dir, imgNames[i]);
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
		return name.substring(0, extStart - 1);
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
