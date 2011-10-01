package com.vaguehope.morrigan.playbackimpl.vlc;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import uk.co.caprica.vlcj.player.MediaPlayerFactory;

import com.vaguehope.morrigan.engines.playback.PlaybackEngineRegister;

public class Activator implements BundleActivator {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private MediaPlayerFactory factory;
	
	@Override
	public void start (BundleContext context) throws Exception {
		factory = new MediaPlayerFactory();
		PlaybackEngineRegister.registerFactory(context.getBundle().getSymbolicName(), new EngineFactory());
	}
	
	@Override
	public void stop (BundleContext context) throws Exception {
		PlaybackEngineRegister.unregisterFactory(context.getBundle().getSymbolicName());
		factory.release();
	}
	
	public static MediaPlayerFactory getFactory () {
		return factory;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
