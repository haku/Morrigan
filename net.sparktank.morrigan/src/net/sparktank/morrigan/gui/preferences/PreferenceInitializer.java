package net.sparktank.morrigan.gui.preferences;

import net.sparktank.morrigan.gui.Activator;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

public class PreferenceInitializer extends AbstractPreferenceInitializer {
	
	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences defaults = new DefaultScope().getNode(Activator.PLUGIN_ID);
		
		defaults.putBoolean(GeneralPref.PREF_WINDOW_MINTOTRAY, false);
		
		defaults.putBoolean(MediaListPref.PREF_SHOWHEADER, true);
		
		defaults.putBoolean(MediaListPref.PREF_COL_DADDED, false);
		defaults.putBoolean(MediaListPref.PREF_COL_COUNTS, true);
		defaults.putBoolean(MediaListPref.PREF_COL_DLASTPLAY, false);
		defaults.putBoolean(MediaListPref.PREF_COL_HASHCODE, false);
		defaults.putBoolean(MediaListPref.PREF_COL_DMODIFIED, false);
		defaults.putBoolean(MediaListPref.PREF_COL_DURATION, true);
		defaults.putBoolean(MediaListPref.PREF_COL_DIMENSIONS, false);
	}
	
}
