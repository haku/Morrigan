package com.vaguehope.morrigan.android.helper;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import com.vaguehope.morrigan.android.helper.DialogHelper.Listener;

public class FileHelper {

	public static void recursiveList (final File root, final Listener<File> onFile) throws IOException {
		final Queue<File> dirs = new LinkedList<File>();
		dirs.add(root);
		while (!dirs.isEmpty()) {
			final File dir = dirs.poll();
			final File[] listFiles = dir.listFiles();
			if (listFiles != null) {
				for (final File file : listFiles) {
					if (file.getName().startsWith(".")) {
						continue;
					}
					else if (file.isDirectory()) {
						dirs.add(file);
					}
					else if (file.isFile()) {
						onFile.onAnswer(file);
					}
				}
			}
		}
	}

}
