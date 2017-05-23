package com.vaguehope.morrigan.server.boot;

import java.lang.reflect.Field;

import org.eclipse.swt.widgets.Display;

/**
 * This nasty hack is to get the default display if it exists but not accidently create it if it did not already exist.
 */
public class DisplayGetter {

	private static Field displayField;

	public static Display getDefaultDisplayIfExists () {
		try {
			if (displayField == null) {
				final Field f = Display.class.getDeclaredField("Default");
				if (!f.isAccessible()) f.setAccessible(true);
				displayField = f;
			}
			return (Display) displayField.get(Display.class);
		}
		catch (final NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
		catch (final IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
