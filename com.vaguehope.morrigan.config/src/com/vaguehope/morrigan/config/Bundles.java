package com.vaguehope.morrigan.config;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public enum Bundles {

	GUI("com.vaguehope.morrigan.gui"),
	JETTY("com.vaguehope.morrigan.jetty"),
	SERVER_BOOT("com.vaguehope.morrigan.server.boot");

	private final String pkgName;

	private Bundles (final String pkgName) {
		this.pkgName = pkgName;
	}

	public String getPkgName () {
		return this.pkgName;
	}

	public boolean isPresent (final BundleContext context) {
		for (final Bundle bundle : context.getBundles()) {
			if (bundle.getSymbolicName().equals(this.pkgName)) return true;
		}
		return false;
	}

	/**
	 * This should never return null.
	 */
	public Bundle getBundle (final BundleContext context) {
		return findBundle(context, this.pkgName);
	}

	/**
	 * This should never return null.
	 */
	public static Bundle findBundle (final BundleContext context, final String name) {
		for (Bundle bundle : context.getBundles()) {
			if (bundle.getSymbolicName().equals(name)) return bundle;
		}
		throw new IllegalStateException("Bundle '"+name+"' not found.");
	}

}
