package com.vaguehope.morrigan.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.concurrent.TimeUnit;

public final class FileHelper {

	private FileHelper () {}

	public static void copyFile (final File sourceFile, final File destFile) throws IOException {
		copyFile(sourceFile, destFile, false);
	}

	public static void copyFile (final File srcFile, final File dstFile, final boolean overWrite) throws IOException {
		if (dstFile.exists()) {
			if (!overWrite) {
				throw new IllegalArgumentException("Target file already exists.");
			}
		}
		else {
			dstFile.createNewFile();
		}

		FileChannel srcChannel = null;
		FileChannel dstChannel = null;

		try {
			boolean transferDone = false;
			int chunkSizeMb = 64;

			while (!transferDone) {
				srcChannel = new FileInputStream(srcFile).getChannel();
				dstChannel = new FileOutputStream(dstFile).getChannel();

				try {
					final int maxCount = (chunkSizeMb * 1024 * 1024) - (32 * 1024);
					final long size = srcChannel.size();
					long position = 0;
					while (position < size) {
						position += srcChannel.transferTo(position, maxCount, dstChannel);
					}
					transferDone = true;
				}
				catch (final IOException e) {
					if (e.getMessage().contains("Insufficient system resources exist to complete the requested service")) {
						chunkSizeMb--;
						if (chunkSizeMb <= 0) {
							throw e; // We have run out of options.
						}
						System.err.println("Reduced chunk size to " + chunkSizeMb + " mb.");
						srcChannel.close();
						if (dstChannel != null) dstChannel.close();
					}
					else {
						throw e;
					}
				}
			}
		}
		finally {
			if (srcChannel != null) {
				srcChannel.close();
			}
			if (dstChannel != null) {
				dstChannel.close();
			}
		}
	}

	public static void rename (final File from, final File to) throws IOException {
		if (!from.renameTo(to)) {
			throw new IOException("Failed to rename '" + from.getAbsolutePath()
					+ "' to '" + to.getAbsolutePath() + "'.");
		}
	}

	public static void freshenLastModified (final File f, final long unlessOlderThan, final TimeUnit timeUnit) throws IOException {
		if (!f.exists()) throw new FileNotFoundException(f.getAbsolutePath());
		final long now = System.currentTimeMillis();
		if (now - f.lastModified() < timeUnit.toMillis(unlessOlderThan)) return;

		if (f.setLastModified(now)) return;
		if (Touch.touch(f)) return;
		throw new IOException("Failed to touch file: " + f.getAbsolutePath());
	}

	public static File getSomeRootDir() {
		final File[] roots = File.listRoots();
		if (roots != null && roots.length > 0) {
			return roots[0];
		}
		return new File("/");
	}

}
