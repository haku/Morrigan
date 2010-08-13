package net.sparktank.morrigan.gui.preferences;

import java.util.Arrays;

import net.sparktank.morrigan.engines.EngineFactory;
import net.sparktank.morrigan.engines.HotkeyKeys;
import net.sparktank.morrigan.engines.HotkeyRegister;
import net.sparktank.morrigan.engines.hotkey.HotkeyValue;
import net.sparktank.morrigan.gui.Activator;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.dialogs.RunnableDialog;

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
	
	public static final String PREF_HK_SHOWHIDE = "PREF_HK_SHOWHIDE";
	public static final String PREF_HK_STOP = "PREF_HK_STOP";
	public static final String PREF_HK_PLAYPAUSE = "PREF_HK_PLAYPAUSE";
	public static final String PREF_HK_NEXT = "PREF_HK_NEXT";
	public static final String PREF_HK_JUMPTO = "PREF_HK_JUMPTO";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		if (EngineFactory.canMakeHotkeyEngine()) {
			setDescription("Hotkey preferences.");
		} else {
			setDescription("No hotkey engine is available.  Configured hotkeys will not function.");
		}
		
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
			
			this.hkShowhide.setValue(getHkShowHide());
			this.hkStop.setValue(getHkStop());
			this.hkPlaypause.setValue(getHkPlaypause());
			this.hkNext.setValue(getHkNext());
			this.hkJumpto.setValue(getHkJumpto());
			
		} catch (Exception e) {
			new MorriganMsgDlg(e).open();
		}
	}
	
	@Override
	public boolean performOk () {
		HotkeyValue hkShowhideValue = this.hkShowhide.getValue();
		if (hkShowhideValue!=null) {
			getPreferenceStore().setValue(PREF_HK_SHOWHIDE, hkShowhideValue.serialise());
		} else {
			getPreferenceStore().setValue(PREF_HK_SHOWHIDE, "");
		}
		
		HotkeyValue hkStopValue = this.hkStop.getValue();
		if (hkStopValue!=null) {
			getPreferenceStore().setValue(PREF_HK_STOP, hkStopValue.serialise());
		} else {
			getPreferenceStore().setValue(PREF_HK_STOP, "");
		}
		
		HotkeyValue hkPlaypauseValue = this.hkPlaypause.getValue();
		if (hkPlaypauseValue!=null) {
			getPreferenceStore().setValue(PREF_HK_PLAYPAUSE, hkPlaypauseValue.serialise());
		} else {
			getPreferenceStore().setValue(PREF_HK_PLAYPAUSE, "");
		}
		
		HotkeyValue hkNextValue = this.hkNext.getValue();
		if (hkNextValue!=null) {
			getPreferenceStore().setValue(PREF_HK_NEXT, hkNextValue.serialise());
		} else {
			getPreferenceStore().setValue(PREF_HK_NEXT, "");
		}
		
		HotkeyValue hkJumptoValue = this.hkJumpto.getValue();
		if (hkJumptoValue!=null) {
			getPreferenceStore().setValue(PREF_HK_JUMPTO, hkJumptoValue.serialise());
		} else {
			getPreferenceStore().setValue(PREF_HK_JUMPTO, "");
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
	
	private HotkeyChooser hkShowhide = null;
	private HotkeyChooser hkStop = null;
	private HotkeyChooser hkPlaypause = null;
	private HotkeyChooser hkNext = null;
	private HotkeyChooser hkJumpto = null;
	
	protected Control makeContents(Composite parent) {
		initializeDialogUnits(parent);
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		composite.setLayout(layout);
		
		this.hkShowhide = new HotkeyChooser(composite, "Show / hide windows");
		this.hkStop = new HotkeyChooser(composite, "stop");
		this.hkPlaypause = new HotkeyChooser(composite, "play / pause");
		this.hkNext = new HotkeyChooser(composite, "next");
		this.hkJumpto = new HotkeyChooser(composite, "jump to");
		
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
		
		@SuppressWarnings("synthetic-access")
		public HotkeyChooser (Composite parent, String title) {
			initializeDialogUnits(parent);
			
			Group group = new Group(parent, SWT.NONE);
			group.setText(title);
			group.setLayout(new RowLayout(SWT.HORIZONTAL));
			
			Label label = new Label (group, SWT.NONE);
			label.setText("key");
			
			this.cmbKey = new Combo (group, SWT.READ_ONLY);
			String[] keys = HotkeyKeys.HkKeys.values().toArray(new String[HotkeyKeys.HkKeys.values().size()]);
			Arrays.sort(keys);
			this.cmbKey.setItems(keys);
			
			this.chkCtrl  = makeCheckBox(group, "ctrl",  false);
			this.chkShift = makeCheckBox(group, "shift", false);
			this.chkAlt   = makeCheckBox(group, "alt",   false);
			this.chkSupr  = makeCheckBox(group, "super", false);
		}
		
		@SuppressWarnings("boxing")
		public HotkeyValue getValue () {
			if (this.cmbKey.getSelectionIndex() < 0) return null;
			
			if (!this.chkCtrl.getSelection()
				&& !this.chkShift.getSelection()
				&& !this.chkAlt.getSelection()
				&& !this.chkSupr.getSelection()
				) return null;
			
			Integer selKey = -1;
			String selKeyName = this.cmbKey.getItem(this.cmbKey.getSelectionIndex());
			for (Integer i : HotkeyKeys.HkKeys.keySet()) {
				if (HotkeyKeys.HkKeys.get(i).equals(selKeyName)) {
					selKey = i;
					break;
				}
			}
			
			if (selKey < 0) return null;
			
			return new HotkeyValue(
					selKey,
					this.chkCtrl.getSelection(),
					this.chkShift.getSelection(),
					this.chkAlt.getSelection(),
					this.chkSupr.getSelection()
					);
		}
		
		@SuppressWarnings("boxing")
		public void setValue (HotkeyValue value) {
			if (value == null) {
				this.cmbKey.setText("");
				this.chkCtrl.setSelection(false);
				this.chkShift.setSelection(false);
				this.chkAlt.setSelection(false);
				this.chkSupr.setSelection(false);
				
			} else {
				this.cmbKey.setText(HotkeyKeys.HkKeys.get(value.getKey()));
				this.chkCtrl.setSelection(value.getCtrl());
				this.chkShift.setSelection(value.getShift());
				this.chkAlt.setSelection(value.getAlt());
				this.chkSupr.setSelection(value.getSupr());
			}
		}
		
	}
	
	Button makeCheckBox (Composite composite, String text, boolean value) {
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
		}
		
		return null;
	}
	
	static public HotkeyValue getHkShowHide () {
		return getHk(PREF_HK_SHOWHIDE);
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
	
	static public HotkeyValue getHkJumpto () {
		return getHk(PREF_HK_JUMPTO);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
