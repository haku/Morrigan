package net.sparktank.morrigan.preferences;

import net.sparktank.morrigan.Activator;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

public class PreferenceInitializer extends AbstractPreferenceInitializer {
	
	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences defaults = new DefaultScope().getNode(Activator.PLUGIN_ID);
		defaults.putBoolean(MediaListPref.PREF_COL_DADDED, false);
		defaults.putBoolean(MediaListPref.PREF_COL_STARTCNT, true);
		defaults.putBoolean(MediaListPref.PREF_COL_ENDCNT, true);
		defaults.putBoolean(MediaListPref.PREF_COL_DLASTPLAY, false);
	}
	
}
