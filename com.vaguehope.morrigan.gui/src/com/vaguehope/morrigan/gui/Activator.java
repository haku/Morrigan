package com.vaguehope.morrigan.gui;


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

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public Activator() {/* UNUSED */}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		if (Activator.plugin != null || Activator.context != null) throw new IllegalStateException("Bundle is already started.");
		Activator.plugin = this;
		Activator.context = bundleContext;

		super.start(bundleContext);

		this.mediaFactoryTracker = new MediaFactoryTracker(bundleContext);
		this.hotkeyEngineFactoryTracker = new HotkeyEngineFactoryTracker(bundleContext);
		this.hotkeyRegister = new HotkeyRegister(this.hotkeyEngineFactoryTracker);
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
		Activator.plugin = null;

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
	public static ImageDescriptor getImageDescriptor(String path) {
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

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
