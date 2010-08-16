package net.sparktank.morrigan.gui.preferences;

import net.sparktank.morrigan.gui.Activator;
import net.sparktank.morrigan.gui.editors.MediaColumn;
import net.sparktank.morrigan.gui.editors.mmdb.MixedMediaListEditor;
import net.sparktank.morrigan.gui.editors.tracks.MediaTrackListEditor;

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
	public static final String PREF_COL_DMODIFIED = "PREF_COL_DMODIFIED";
	public static final String PREF_COL_DURATION  = "PREF_COL_DURATION";
	public static final String PREF_COL_DIMENSIONS  = "PREF_COL_DIMENSIONS";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void createFieldEditors() {
		addField(new BooleanFieldEditor(PREF_SHOWHEADER, "Show column headers", getFieldEditorParent()));
		addField(new BooleanFieldEditor(PREF_COL_COUNTS, "Show counts column", getFieldEditorParent()));
		addField(new BooleanFieldEditor(PREF_COL_DADDED, "Show date added column", getFieldEditorParent()));
		addField(new BooleanFieldEditor(PREF_COL_DLASTPLAY, "Show date last played column", getFieldEditorParent()));
		addField(new BooleanFieldEditor(PREF_COL_HASHCODE, "Show hashcode column", getFieldEditorParent()));
		addField(new BooleanFieldEditor(PREF_COL_DMODIFIED, "Show date last modified column", getFieldEditorParent()));
		addField(new BooleanFieldEditor(PREF_COL_DURATION, "Show duration column", getFieldEditorParent()));
		addField(new BooleanFieldEditor(PREF_COL_DIMENSIONS, "Show dimensions column", getFieldEditorParent()));
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
	
	public static boolean getColPref (MediaTrackListEditor<?,?> editor, MediaColumn column) {
		if (column == editor.COL_ADDED) {
			return Activator.getDefault().getPreferenceStore().getBoolean(PREF_COL_DADDED);
		}
		else if (column == editor.COL_COUNTS) {
			return Activator.getDefault().getPreferenceStore().getBoolean(PREF_COL_COUNTS);
		}
		else if (column == editor.COL_LASTPLAYED) {
			return Activator.getDefault().getPreferenceStore().getBoolean(PREF_COL_DLASTPLAY);
		}
		else if (column == editor.COL_HASH) {
			return Activator.getDefault().getPreferenceStore().getBoolean(PREF_COL_HASHCODE);
		}
		else if (column == editor.COL_MODIFIED) {
			return Activator.getDefault().getPreferenceStore().getBoolean(PREF_COL_DMODIFIED);
		}
		else if (column == editor.COL_DURATION) {
			return Activator.getDefault().getPreferenceStore().getBoolean(PREF_COL_DURATION);
		}
		else {
			return true;
		}
	}
	
	public static boolean getColPref (MixedMediaListEditor<?,?> editor, MediaColumn column) {
		if (column == editor.COL_ADDED) {
			return Activator.getDefault().getPreferenceStore().getBoolean(PREF_COL_DADDED);
		}
		else if (column == editor.COL_COUNTS) {
			return Activator.getDefault().getPreferenceStore().getBoolean(PREF_COL_COUNTS);
		}
		else if (column == editor.COL_LASTPLAYED) {
			return Activator.getDefault().getPreferenceStore().getBoolean(PREF_COL_DLASTPLAY);
		}
		else if (column == editor.COL_HASH) {
			return Activator.getDefault().getPreferenceStore().getBoolean(PREF_COL_HASHCODE);
		}
		else if (column == editor.COL_MODIFIED) {
			return Activator.getDefault().getPreferenceStore().getBoolean(PREF_COL_DMODIFIED);
		}
		else if (column == editor.COL_DURATION) {
			return Activator.getDefault().getPreferenceStore().getBoolean(PREF_COL_DURATION);
		}
		else if (column == editor.COL_DIMENSIONS) {
			return Activator.getDefault().getPreferenceStore().getBoolean(PREF_COL_DIMENSIONS);
		}
		else {
			return true;
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
