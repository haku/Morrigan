package com.vaguehope.morrigan.gui;


import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.vaguehope.morrigan.model.media.impl.MediaFactoryImpl;

public class Activator extends AbstractUIPlugin {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String PLUGIN_ID = "net.sparktank.morrigan.gui";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static BundleContext context;
	private static Activator plugin;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public Activator() {/* UNUSED */}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
		Activator.plugin = this;
		Activator.context = bundleContext;
	}
	
	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.plugin = null;
		Activator.context = null;
		
		try {
			MediaFactoryImpl.get().disposeAllPlaylists();
		}
		finally {
			super.stop(bundleContext);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static BundleContext getContext() {
		return Activator.context;
	}
	
	public static Activator getDefault() {
		return Activator.plugin;
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
