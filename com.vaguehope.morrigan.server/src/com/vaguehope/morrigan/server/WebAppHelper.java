package com.vaguehope.morrigan.server;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jetty.webapp.WebAppContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class WebAppHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private WebAppHelper () {}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final String JETTY_WEBDEFAULT_PATH = "org/eclipse/jetty/webapp/webdefault.xml";
	private static final String JETTY_WEBAPP_BUNDLE_NAME = "org.eclipse.jetty.webapp";
	
	/**
	 * Look up and load a WAR bundle in an OSGI environment.
	 */
	public static WebAppContext getWarBundleAsContext (BundleContext context, String warBundleName, String contextPath) throws IOException {
		Bundle warBundle = findBundle(context, warBundleName);
		URL fileURL = FileLocator.toFileURL(warBundle.getEntry("/")); // I think org.eclipse.core.runtime must be started before this is called.  Or something like that.
		File warFile = new File(fileURL.getPath());
		
		WebAppContext warContext = new WebAppContext();
		warContext.setContextPath(contextPath);
		warContext.setWar(warFile.getAbsolutePath());
		
		/* Jetty needs to find webdefault.xml but does not know how
		 * OSGi class loaders work, so we need to give it some help.
		 */
		Bundle jettyWebappBundle = findBundle(context, JETTY_WEBAPP_BUNDLE_NAME);
		URL webDefaultUrl = jettyWebappBundle.getResource(JETTY_WEBDEFAULT_PATH);
		warContext.setDefaultsDescriptor(webDefaultUrl.toExternalForm());
		
		return warContext;
	}
	
	/**
	 * This should never return null.
	 */
	static private Bundle findBundle (BundleContext context, String name) {
		for (Bundle bundle : context.getBundles()) {
			if (bundle.getSymbolicName().equals(name)) return bundle;
		}
		throw new IllegalStateException("Bundle '"+name+"' not found.");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
