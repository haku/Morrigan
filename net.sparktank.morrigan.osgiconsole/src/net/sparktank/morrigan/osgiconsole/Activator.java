package net.sparktank.morrigan.osgiconsole;

import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator  {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void start(BundleContext context) throws Exception {
		context.registerService(CommandProvider.class.getName(), new MorriganCommandProvider(), null);
	}
	
	@Override
	public void stop(BundleContext context) throws Exception {
		// UNUSED.
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
