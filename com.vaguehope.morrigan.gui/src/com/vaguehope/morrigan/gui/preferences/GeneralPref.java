package com.vaguehope.morrigan.gui.preferences;


import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.vaguehope.morrigan.gui.Activator;

public class GeneralPref extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String PREF_WINDOW_MINTOTRAY = "PREF_WINDOW_MINTOTRAY";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(PREF_WINDOW_MINTOTRAY, "Minimize to tray", getFieldEditorParent()));
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("General preferences.");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static boolean getMinToTray () {
		return Activator.getDefault().getPreferenceStore().getBoolean(PREF_WINDOW_MINTOTRAY);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
