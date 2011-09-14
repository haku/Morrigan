package com.vaguehope.morrigan.playbackimpl.jmf;


import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.vaguehope.morrigan.engines.playback.PlaybackEngineRegister;

public class Activator implements BundleActivator {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void start (BundleContext context) throws Exception {
		PlaybackEngineRegister.registerFactory(context.getBundle().getSymbolicName(), new EngineFactory());
	}
	
	@Override
	public void stop (BundleContext context) throws Exception {
		PlaybackEngineRegister.unregisterFactory(context.getBundle().getSymbolicName());
	}
	
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
