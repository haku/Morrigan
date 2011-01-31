package net.sparktank.morrigan.hotkeyimpl.jxgrabkey;

import net.sparktank.morrigan.engines.hotkey.HotkeyEngineRegister;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
//	private static final Logger logger = Logger.getLogger(Activator.class.getName());
	
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
