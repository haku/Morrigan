package com.vaguehope.morrigan.util;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Touch {

	private static final Logger LOG = LoggerFactory.getLogger(Touch.class);

	private static Boolean isAvailable = null;

	private static boolean isAvailable () {
		if (isAvailable == null) {
			try {
				if (ProcessHelper.runAndWait("touch", "--version").size() > 0) {
					isAvailable = true;
				}
				else {
					isAvailable = false;
					LOG.warn("touch --version returned no output.");
				}
			}
			catch (final IOException e) {
				isAvailable = false;
				LOG.warn("touch command not available: {}", ExceptionHelper.causeTrace(e, " --> "));
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
