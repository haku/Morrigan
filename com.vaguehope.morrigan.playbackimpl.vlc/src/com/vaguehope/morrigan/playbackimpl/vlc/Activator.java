package com.vaguehope.morrigan.playbackimpl.vlc;

import java.io.File;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import uk.co.caprica.vlcj.player.MediaPlayerFactory;

import com.sun.jna.NativeLibrary;
import com.vaguehope.morrigan.engines.playback.PlaybackEngineFactory;

public class Activator implements BundleActivator {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static final String PROP_VLCLIB = "morrigan.vlclib";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected static final Logger logger = Logger.getLogger(Activator.class.getName());

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static MediaPlayerFactory factory;

	@Override
	public void start (BundleContext context) throws Exception {
		createFactory();
		context.registerService(PlaybackEngineFactory.class, new EngineFactory(), null);
	}

	@Override
	public void stop (BundleContext context) throws Exception {
		destroyFactory();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/* Possible switches:
	 * -vvv (for debugging)
	 * --no-video-title-show
	 */

	private final static String[] FACTORY_ARGS = {
		"--no-video-title-show"
		};

	public static void createFactory () {
		String vlclib = System.getProperty(PROP_VLCLIB);
		if (vlclib != null) {
			File f = new File(vlclib);
			if (f.exists()) {
				NativeLibrary.addSearchPath("vlc", vlclib);
				logger.info("Native library 'vlc' search path added: " + f.getAbsolutePath());
			}
			else {
				logger.warning("Native library 'vlc' specified via system property '"+PROP_VLCLIB+"' but not found: " + f.getAbsolutePath());
			}
		}

		factory = new MediaPlayerFactory(FACTORY_ARGS);
	}

	public static void destroyFactory () {
		factory.release();
	}

	public static MediaPlayerFactory getFactory () {
		if (factory == null) throw new IllegalStateException("Bundle is not active so factory does not exist.");
		return factory;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
