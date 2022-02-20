package com.vaguehope.morrigan.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.Args;


public class Config {

	private static final String DIR_CONFIG = ".morrigan";
	private static final Logger LOG = LoggerFactory.getLogger(Config.class);

	private final File configDir;

	public static Config fromArgs(final Args args) throws IOException {
		final String argsPath = args.getConfigPath();
		final File dir;
		if (argsPath != null) {
			dir = new File(argsPath);
			checkExistsAndIsDirectory(dir, argsPath);
		}
		else {
			final File home = new File(System.getProperty("user.home"));
			checkExistsAndIsDirectory(home, argsPath);
			dir = new File(home, DIR_CONFIG);
			mkdir(dir);
		}
		return new Config(dir);
	}

	private static void checkExistsAndIsDirectory(final File dir, final String pathForErr) throws FileNotFoundException, IOException {
		if (!dir.exists()) {
			throw new FileNotFoundException("Not found: " + pathForErr);
		}
		if (!dir.isDirectory()) {
			throw new IOException("Not a directory: " + pathForErr);
		}
	}

	public Config (final File configDir) {
		this.configDir = configDir;
		LOG.info("Config directory: {}", configDir.getAbsolutePath());
	}

	public File getConfigDir() {
		return this.configDir;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String PL_DIR = "pl";
	public static final String PL_FILE_EXT = ".morrigan_pl";

	public File getPlDir () {
		final File f = new File(this.configDir, PL_DIR);
		mkdir(f);
		return f;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String LIB_DIR = "libs";
	public static final String LIB_LOCAL_FILE_EXT = ".local.db3";
	public static final String LIB_REMOTE_FILE_EXT = ".remote.db3";

	public File getLibDir () {
		final File f = new File(this.configDir, LIB_DIR);
		mkdir(f);
		return f;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String GALLERY_DIR = "gals";
	public static final String GALLERY_LOCAL_FILE_EXT = ".local.db3";

	public File getGalleryDir () {
		final File f = new File(this.configDir, GALLERY_DIR);
		mkdir(f);
		return f;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String MMDB_DIR = "mmdb";
	public static final String MMDB_LOCAL_FILE_EXT = ".local.db3";
	public static final String MMDB_REMOTE_FILE_EXT = ".remote.db3";

	public File getMmdbDir () {
		final File f = new File(this.configDir, MMDB_DIR);
		mkdir(f);
		return f;
	}

	private static final String SAVED_VIEWS_FILE = "savedviews.json";

	public File getSavedViewsFile () {
		return new File(this.configDir, SAVED_VIEWS_FILE);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String PROP_MEDIA_TYPES = "morrigan.media.types";
	private static final String PROP_MEDIA_PICTURE_TYPES = "morrigan.media.pictures.types";

	private static final String DEFAULT_MEDIA_TYPES = "mp3|ogg|wma|wmv|avi|mpg|mpeg|ac3|mp4|wav|ra|mpga|mkv|oga|ogm|mpc|m4a|flv|rmvb|aac|m4v|flac|webm";
	private static final String DEFAULT_MEDIA_PICTURE_TYPES = "jpg|jpeg|png|gif";

	/**
	 * @return Array of lower-case strings without dots.  e.g. "mp3", "ogg".
	 */
	public static String[] getMediaFileTypes () {
		final String types = System.getProperty(PROP_MEDIA_TYPES, DEFAULT_MEDIA_TYPES);
		final String[] arrTypes = types.split("\\|");

		for (int i = 0; i < arrTypes.length; i++) {
			arrTypes[i] = arrTypes[i].toLowerCase();
		}

		return arrTypes;
	}

	/**
	 * @return Array of lower-case strings without dots.  e.g. "mp3", "ogg".
	 */
	public static String[] getPictureFileTypes () {
		final String types = System.getProperty(PROP_MEDIA_PICTURE_TYPES, DEFAULT_MEDIA_PICTURE_TYPES);
		final String[] arrTypes = types.split("\\|");

		for (int i = 0; i < arrTypes.length; i++) {
			arrTypes[i] = arrTypes[i].toLowerCase();
		}

		return arrTypes;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String RESIZED_DIR = "resized";

	public File getResizedDir () {
		final File f = new File(this.configDir, RESIZED_DIR);
		mkdir(f);
		return f;
	}

	private static final String TRANSCODED_DIR = "transcoded";

	public File getTranscodedDir () {
		final File f = new File(this.configDir, TRANSCODED_DIR);
		mkdir(f);
		return f;
	}

	private static final String SESSION_DIR = "sessions";

	public File getSessionDir () {
		final File f = new File(this.configDir, SESSION_DIR);
		mkdir(f);
		return f;
	}

	private static final String WEB_LIBRARY_DIR = "weblibs";

	public File getWebLibraryDir () {
		final File f = new File(this.configDir, WEB_LIBRARY_DIR);
		mkdir(f);
		return f;
	}

	private static void mkdir (final File f) {
		if (!f.exists()) {
			if (!f.mkdirs()) {
				throw new RuntimeException("Failed to create direactory '" + f.getAbsolutePath() + "'.");
			}
		}
	}

}
