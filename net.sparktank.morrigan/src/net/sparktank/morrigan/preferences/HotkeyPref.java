package net.sparktank.morrigan.preferences;

import net.sparktank.morrigan.Activator;
import net.sparktank.morrigan.engines.hotkey.HotkeyValue;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class HotkeyPref extends PreferencePage implements IWorkbenchPreferencePage {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String PREF_HK_PLAYPAUSE = "PREF_HK_PLAYPAUSE";
	
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
		hkPlaypause.setValue(getHkPlaypause());
	}
	
	@Override
	public boolean performOk () {
		HotkeyValue hkPlaypauseValue = hkPlaypause.getValue();
		if (hkPlaypauseValue!=null) {
			getPreferenceStore().setValue(PREF_HK_PLAYPAUSE, hkPlaypauseValue.serialise());
		} else {
			getPreferenceStore().setValue(PREF_HK_PLAYPAUSE, "");
		}
		
		return super.performOk();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private HotkeyChooser hkPlaypause;
	
	protected Control makeContents(Composite parent) {
		initializeDialogUnits(parent);
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		composite.setLayout(layout);
		
		hkPlaypause = new HotkeyChooser(composite, "play / pause");
		
		applyDialogFont(composite);
		return composite;
	}
	
	private class HotkeyChooser {
		
		private final Text textKey;
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
			
			textKey = new Text (group, SWT.BORDER | SWT.SINGLE);
			textKey.setTextLimit(1);
			textKey.setLayoutData(new RowData(convertHorizontalDLUsToPixels(10), SWT.DEFAULT));
			
			chkCtrl  = makeCheckBox(group, "ctrl",  false);
			chkShift = makeCheckBox(group, "shift", false);
			chkAlt   = makeCheckBox(group, "alt",   false);
			chkSupr  = makeCheckBox(group, "super", false);
		}
		
		public HotkeyValue getValue () {
			if (textKey.getText().length() != 1) return null;
			
			return new HotkeyValue(
					(int)textKey.getText().toCharArray()[0],
					chkCtrl.getSelection(),
					chkShift.getSelection(),
					chkAlt.getSelection(),
					chkSupr.getSelection()
					);
		}
		
		public void setValue (HotkeyValue value) {
			if (value == null) {
				textKey.setText("");
				chkCtrl.setSelection(false);
				chkShift.setSelection(false);
				chkAlt.setSelection(false);
				chkSupr.setSelection(false);
				
			} else {
				textKey.setText( String.valueOf((char) value.getKey()) );
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
	
	public static HotkeyValue getHkPlaypause () {
		String s = Activator.getDefault().getPreferenceStore().getString(PREF_HK_PLAYPAUSE);
		if (s != null && s.length() > 0) {
			return new HotkeyValue(s);
		} else {
			return null;
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
