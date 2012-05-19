package com.vaguehope.morrigan.hotkeyimpl.jintellitype;


import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.vaguehope.morrigan.engines.hotkey.HotkeyEngineFactory;

public class Activator implements BundleActivator {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void start (BundleContext context) throws Exception {
		context.registerService(HotkeyEngineFactory.class, new EngineFactory(), null);
	}

	@Override
	public void stop (BundleContext context) throws Exception {
		// Unused.
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
