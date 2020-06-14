package com.vaguehope.morrigan.model.media.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.engines.playback.PlaybackEngineFactoryTracker;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.internal.MediaFactoryImpl;

public class MediaActivator implements BundleActivator {

	private PlaybackEngineFactoryTracker playbackEngineFactoryTracker;

	@Override
	public void start (final BundleContext context) throws Exception {
		this.playbackEngineFactoryTracker = new PlaybackEngineFactoryTracker(context);
		context.registerService(MediaFactory.class, new MediaFactoryImpl(Config.DEFAULT, this.playbackEngineFactoryTracker), null);
	}

	@Override
	public void stop (final BundleContext context) throws Exception {
		this.playbackEngineFactoryTracker.dispose();
	}

}
