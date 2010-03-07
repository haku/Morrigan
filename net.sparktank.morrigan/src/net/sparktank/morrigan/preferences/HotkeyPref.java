package net.sparktank.morrigan.preferences;

import net.sparktank.morrigan.Activator;
import net.sparktank.morrigan.dialogs.RunnableDialog;
import net.sparktank.morrigan.engines.HotkeyKeys;
import net.sparktank.morrigan.engines.HotkeyRegister;
import net.sparktank.morrigan.engines.hotkey.HotkeyValue;
import net.sparktank.morrigan.dialogs.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class HotkeyPref extends PreferencePage implements IWorkbenchPreferencePage {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String PREF_HK_STOP = "PREF_HK_STOP";
	public static final String PREF_HK_PLAYPAUSE = "PREF_HK_PLAYPAUSE";
	public static final String PREF_HK_NEXT = "PREF_HK_NEXT";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Hotkey preferences.");
	}
	
	@Override
	protected Control createContents(Composite parent) {
		Control contents = makeContents(parent);
		initialize();
		return contents;
	}
	
	// Read data.
	private void initialize () {
		try {
			hkStop.setValue(getHkStop());
			hkPlaypause.setValue(getHkPlaypause());
			hkNext.setValue(getHkNext());
			
		} catch (Exception e) {
			new MorriganMsgDlg(e).open();
		}
	}
	
	@Override
	public boolean performOk () {
		HotkeyValue hkStopValue = hkStop.getValue();
		if (hkStopValue!=null) {
			getPreferenceStore().setValue(PREF_HK_STOP, hkStopValue.serialise());
		} else {
			getPreferenceStore().setValue(PREF_HK_STOP, "");
		}
		
		HotkeyValue hkPlaypauseValue = hkPlaypause.getValue();
		if (hkPlaypauseValue!=null) {
			getPreferenceStore().setValue(PREF_HK_PLAYPAUSE, hkPlaypauseValue.serialise());
		} else {
			getPreferenceStore().setValue(PREF_HK_PLAYPAUSE, "");
		}
		
		HotkeyValue hkNextValue = hkNext.getValue();
		if (hkNextValue!=null) {
			getPreferenceStore().setValue(PREF_HK_NEXT, hkNextValue.serialise());
		} else {
			getPreferenceStore().setValue(PREF_HK_NEXT, "");
		}
		
		try {
			HotkeyRegister.readConfig(true);
		} catch (Throwable t) {
			getShell().getDisplay().asyncExec(new RunnableDialog(t));
			return false;
		}
		
		return super.performOk();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private HotkeyChooser hkStop;
	private HotkeyChooser hkPlaypause;
	private HotkeyChooser hkNext;
	
	protected Control makeContents(Composite parent) {
		initializeDialogUnits(parent);
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		composite.setLayout(layout);
		
		hkStop = new HotkeyChooser(composite, "stop");
		hkPlaypause = new HotkeyChooser(composite, "play / pause");
		hkNext = new HotkeyChooser(composite, "next");
		
		applyDialogFont(composite);
		return composite;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private class HotkeyChooser {
		
		private Combo cmbKey;
		private Button chkCtrl;
		private Button chkShift;
		private Button chkAlt;
		private Button chkSupr;
		
		public HotkeyChooser (Composite parent, String title) {
			initializeDialogUnits(parent);
			
			Group group = new Group(parent, SWT.NONE);
			group.setText(title);
			group.setLayout(new RowLayout(SWT.HORIZONTAL));
			
			Label label = new Label (group, SWT.NONE);
			label.setText("key");
			
			cmbKey = new Combo (group, SWT.READ_ONLY);
			cmbKey.setItems(HotkeyKeys.HkKeys.values().toArray(new String[HotkeyKeys.HkKeys.values().size()]));
			
			chkCtrl  = makeCheckBox(group, "ctrl",  false);
			chkShift = makeCheckBox(group, "shift", false);
			chkAlt   = makeCheckBox(group, "alt",   false);
			chkSupr  = makeCheckBox(group, "super", false);
		}
		
		public HotkeyValue getValue () {
			if (cmbKey.getSelectionIndex() < 0) return null;
			
			if (!chkCtrl.getSelection()
				&& !chkShift.getSelection()
				&& !chkAlt.getSelection()
				&& !chkSupr.getSelection()
				) return null;
			
			Integer selKey = -1;
			String selKeyName = cmbKey.getItem(cmbKey.getSelectionIndex());
			for (Integer i : HotkeyKeys.HkKeys.keySet()) {
				if (HotkeyKeys.HkKeys.get(i).equals(selKeyName)) {
					selKey = i;
					break;
				}
			}
			
			if (selKey < 0) return null;
			
			return new HotkeyValue(
					selKey,
					chkCtrl.getSelection(),
					chkShift.getSelection(),
					chkAlt.getSelection(),
					chkSupr.getSelection()
					);
		}
		
		public void setValue (HotkeyValue value) {
			if (value == null) {
				cmbKey.setText("");
				chkCtrl.setSelection(false);
				chkShift.setSelection(false);
				chkAlt.setSelection(false);
				chkSupr.setSelection(false);
				
			} else {
				cmbKey.setText(HotkeyKeys.HkKeys.get(value.getKey()));
				chkCtrl.setSelection(value.getCtrl());
				chkShift.setSelection(value.getShift());
				chkAlt.setSelection(value.getAlt());
				chkSupr.setSelection(value.getSupr());
			}
		}
		
	}
	
	private Button makeCheckBox (Composite composite, String text, boolean value) {
		Button button = new Button(composite, SWT.CHECK);
		button.setText(text);
		button.setSelection(value);
		return button;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private HotkeyValue getHk (String key) {
		String s = Activator.getDefault().getPreferenceStore().getString(key);
		if (s != null && s.length() > 0) {
			return new HotkeyValue(s);
		} else {
			return null;
		}
	}
	
	static public HotkeyValue getHkStop () {
		return getHk(PREF_HK_STOP);
	}
	
	static public HotkeyValue getHkPlaypause () {
		return getHk(PREF_HK_PLAYPAUSE);
	}
	
	static public HotkeyValue getHkNext () {
		return getHk(PREF_HK_NEXT);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
