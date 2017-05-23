package com.vaguehope.morrigan.server.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jetty.webapp.WebAppContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.vaguehope.morrigan.config.Bundles;

public class WebAppHelper {

	private WebAppHelper () {}

	private static final String JETTY_WEBDEFAULT_PATH = "org/eclipse/jetty/webapp/webdefault.xml";

	/**
	 * Look up and load a WAR bundle in an OSGI environment.
	 */
	public static WebAppContext getWarBundleAsContext (final BundleContext context, final String warBundleName, final String contextPath) throws IOException {
		Bundle warBundle = Bundles.findBundle(context, warBundleName);
		URL fileURL = FileLocator.toFileURL(warBundle.getEntry("/")); // I think org.eclipse.core.runtime must be started before this is called.  Or something like that.
		File warFile = new File(fileURL.getPath());

		WebAppContext warContext = new WebAppContext();
		warContext.setContextPath(contextPath);
		warContext.setWar(warFile.getAbsolutePath());

		/* Jetty needs to find webdefault.xml but does not know how
		 * OSGi class loaders work, so we need to give it some help.
		 */
		Bundle jettyWebappBundle = Bundles.JETTY.getBundle(context);
		URL webDefaultUrl = jettyWebappBundle.getResource(JETTY_WEBDEFAULT_PATH);
		warContext.setDefaultsDescriptor(webDefaultUrl.toExternalForm());

		return warContext;
	}

}
