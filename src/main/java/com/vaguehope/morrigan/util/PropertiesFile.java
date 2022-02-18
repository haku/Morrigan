package com.vaguehope.morrigan.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

public class PropertiesFile {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final File file;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public PropertiesFile (final File file) {
		if (file == null) throw new IllegalArgumentException("Filepath must be set.");
		this.file = file;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public String getString (final String key, final String defaultValue) throws IOException {
		final String string = getProperties().getProperty(key);
		if (string == null) return defaultValue;
		return string;
	}

	public int getInt (final String key, final int defaultValue) throws IOException {
		final String string = getProperties().getProperty(key);
		if (string == null) return defaultValue;
		try {
			return Integer.parseInt(string);
		}
		catch (final NumberFormatException e) {
			return defaultValue;
		}
	}

	public Set<Entry<Object, Object>> getAll () throws IOException {
		return getProperties().entrySet();
	}

	public void writeString (final String key, final String value) throws IOException {
		getProperties().put(key, value);
		writeProperties();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private Properties propCache = null;
	private final Object[] propCacheLock = new Object[0];

	private Properties getProperties () throws IOException {
		synchronized (this.propCacheLock) {
			if (this.propCache == null) {
				final Properties props = new Properties();
				InputStream fis = null;
				try {
					fis = new FileInputStream(this.file);
					props.load(fis);
				}
				catch (final FileNotFoundException e) {
					// Do nothing.
				}
				finally {
					IoHelper.closeQuietly(fis);
				}
				this.propCache = props;
			}
		}
		return this.propCache;
	}

	private void writeProperties () throws IOException {
		synchronized (this.propCacheLock) {
			if (this.propCache == null) {
				throw new IllegalStateException("Must read before writing.");
			}

			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			this.propCache.store(baos, null);

			final File ftmp = File.createTempFile(this.file.getName(), ".tmp", this.file.getParentFile());
			try {
				IoHelper.write(new ByteArrayInputStream(baos.toByteArray()), ftmp);
				FileHelper.rename(ftmp, this.file);
			}
			finally {
				if (ftmp.exists()) ftmp.delete();
			}
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
