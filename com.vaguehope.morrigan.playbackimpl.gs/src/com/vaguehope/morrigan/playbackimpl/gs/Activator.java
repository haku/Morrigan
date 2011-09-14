package com.vaguehope.morrigan.playbackimpl.gs;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;


import org.gstreamer.Gst;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.vaguehope.morrigan.engines.playback.PlaybackEngineRegister;

public class Activator implements BundleActivator {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final Logger logger = Logger.getLogger(Activator.class.getName());
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void start (BundleContext context) throws Exception {
		init();
		PlaybackEngineRegister.registerFactory(context.getBundle().getSymbolicName(), new EngineFactory());
	}
	
	@Override
	public void stop (BundleContext context) throws Exception {
		PlaybackEngineRegister.unregisterFactory(context.getBundle().getSymbolicName());
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/*
	 * This a really dodgy way to ensure that Gst.init() is only called
	 * once per JVM instance.
	 * This is to work around defect:
	 * https://code.google.com/p/gstreamer-java/issues/detail?id=69
	 */
	
	private final static AtomicBoolean inited = new AtomicBoolean(false);
	
	public static void init () {
		boolean r = inited.compareAndSet(false, true);
		if (r) {
			logger.fine("Gst.init().");
			Gst.init("VideoPlayer", new String[] {});
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
