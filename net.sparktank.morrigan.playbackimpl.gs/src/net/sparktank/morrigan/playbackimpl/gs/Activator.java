package net.sparktank.morrigan.playbackimpl.gs;

import net.sparktank.morrigan.engines.playback.PlaybackEngineRegister;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
	
	@Override
	public void start (BundleContext context) throws Exception {
		PlaybackEngineRegister.registerFactory(context.getBundle().getSymbolicName(), new EngineFactory());
	}
	
	@Override
	public void stop (BundleContext context) throws Exception {
		PlaybackEngineRegister.unregisterFactory(context.getBundle().getSymbolicName());
	}
	
}
