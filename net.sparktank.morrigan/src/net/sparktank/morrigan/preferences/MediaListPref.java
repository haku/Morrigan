package net.sparktank.morrigan.preferences;

import net.sparktank.morrigan.Activator;
import net.sparktank.morrigan.editors.MediaListEditor.MediaColumn;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class MediaListPref extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String PREF_SHOWHEADER = "PREF_SHOWHEADER";
	
	public static final String PREF_COL_DADDED    = "PREF_COL_DADDED";
	public static final String PREF_COL_COUNTS    = "PREF_COL_COUNTS";
	public static final String PREF_COL_DLASTPLAY = "PREF_COL_DLASTPLAY";
	public static final String PREF_COL_HASHCODE  = "PREF_COL_HASHCODE";
	public static final String PREF_COL_DURATION  = "PREF_COL_DURATION";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void createFieldEditors() {
		addField(new BooleanFieldEditor(PREF_SHOWHEADER, "Show column headers", getFieldEditorParent()));
		addField(new BooleanFieldEditor(PREF_COL_COUNTS, "Show counts column", getFieldEditorParent()));
		addField(new BooleanFieldEditor(PREF_COL_DADDED, "Show date added column", getFieldEditorParent()));
		addField(new BooleanFieldEditor(PREF_COL_DLASTPLAY, "Show date last played column", getFieldEditorParent()));
		addField(new BooleanFieldEditor(PREF_COL_HASHCODE, "Show hashcode column", getFieldEditorParent()));
		addField(new BooleanFieldEditor(PREF_COL_DURATION, "Show duration column", getFieldEditorParent()));
	}
	
	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Media list preferences.  This will only effect new editors.");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static boolean getShowHeadersPref () {
		return Activator.getDefault().getPreferenceStore().getBoolean(PREF_SHOWHEADER);
	}
	
	public static boolean getColPref (MediaColumn column) {
		switch (column) {
			case DADDED:
				return Activator.getDefault().getPreferenceStore().getBoolean(PREF_COL_DADDED);
				
			case COUNTS:
				return Activator.getDefault().getPreferenceStore().getBoolean(PREF_COL_COUNTS);
			
			case DLASTPLAY:
				return Activator.getDefault().getPreferenceStore().getBoolean(PREF_COL_DLASTPLAY);
				
			case HASHCODE:
				return Activator.getDefault().getPreferenceStore().getBoolean(PREF_COL_HASHCODE);
				
			case DURATION:
				return Activator.getDefault().getPreferenceStore().getBoolean(PREF_COL_DURATION);
				
			default:
				return true;
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
