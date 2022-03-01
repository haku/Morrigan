package com.vaguehope.morrigan.util;

import java.io.File;
import java.io.IOException;

public class Touch {

	private static Boolean isAvailable = null;

	private static boolean isAvailable () {
		if (isAvailable == null) {
			try {
				isAvailable = ProcessHelper.runAndWait("touch", "--version").size() > 0;
			}
			catch (final IOException e) {
				isAvailable = false;
			}
		}
		return isAvailable.booleanValue();
	}

	static boolean touch(final File path) throws IOException {
		if (!isAvailable()) return false;

		ProcessHelper.runAndWait(new String[] {
				"touch",
				path.getAbsolutePath(),
		});
		return true;
	}

}
