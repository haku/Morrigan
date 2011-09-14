package com.vaguehope.morrigan.hotkeyimpl.jintellitype;


import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.vaguehope.morrigan.engines.hotkey.HotkeyEngineRegister;

public class Activator implements BundleActivator {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void start (BundleContext context) throws Exception {
		HotkeyEngineRegister.registerFactory(context.getBundle().getSymbolicName(), new EngineFactory());
	}
	
	@Override
	public void stop (BundleContext context) throws Exception {
		HotkeyEngineRegister.unregisterFactory(context.getBundle().getSymbolicName());
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
