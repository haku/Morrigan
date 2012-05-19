package com.vaguehope.morrigan.playbackimpl.jmf;


import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.vaguehope.morrigan.engines.playback.PlaybackEngineFactory;

public class Activator implements BundleActivator {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void start (BundleContext context) throws Exception {
		context.registerService(PlaybackEngineFactory.class, new EngineFactory(), null);
	}

	@Override
	public void stop (BundleContext context) throws Exception {
		// Unused.
	}


//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
