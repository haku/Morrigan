package com.vaguehope.morrigan.gui;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.vaguehope.morrigan.engines.hotkey.HotkeyEngineFactory;
import com.vaguehope.morrigan.engines.hotkey.HotkeyEngineFactoryTracker;
import com.vaguehope.morrigan.gui.engines.HotkeyRegister;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.MediaFactoryTracker;

public class Activator extends AbstractUIPlugin {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static final String PLUGIN_ID = "com.vaguehope.morrigan.gui";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static BundleContext context;
	private static Activator plugin;

	private MediaFactoryTracker mediaFactoryTracker;
	private HotkeyEngineFactoryTracker hotkeyEngineFactoryTracker;
	private HotkeyRegister hotkeyRegister;
	private ExecutorService executorService;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public Activator() {/* UNUSED */}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void start(final BundleContext bundleContext) throws Exception {
		if (Activator.plugin != null || Activator.context != null) throw new IllegalStateException("Bundle is already started.");
		Activator.plugin = this;
		Activator.context = bundleContext;

		super.start(bundleContext);

		this.mediaFactoryTracker = new MediaFactoryTracker(bundleContext);
		this.hotkeyEngineFactoryTracker = new HotkeyEngineFactoryTracker(bundleContext);
		this.hotkeyRegister = new HotkeyRegister(this.hotkeyEngineFactoryTracker);
		this.executorService = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	}

	@Override
	public void stop(final BundleContext bundleContext) throws Exception {
		Activator.context = null;
		Activator.plugin = null;

		this.executorService.shutdownNow();
		this.hotkeyEngineFactoryTracker.dispose();
		this.mediaFactoryTracker.dispose();

		super.stop(bundleContext);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static BundleContext getContext() {
		if (context == null) throw new IllegalStateException("Bundle is not active.");
		return context;
	}

	public static Activator getDefault() {
		if (plugin == null) throw new IllegalStateException("Bundle is not active.");
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(final String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public static MediaFactory getMediaFactory () {
		return getDefault().mediaFactoryTracker;
	}

	public static HotkeyEngineFactory getHotkeyEngineFactory () {
		return getDefault().hotkeyEngineFactoryTracker;
	}

	public static HotkeyRegister getHotkeyRegister () {
		return getDefault().hotkeyRegister;
	}

	public static ExecutorService getExecutorService () {
		return getDefault().executorService;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
