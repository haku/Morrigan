package com.vaguehope.morrigan.gui.controls;


import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;

import com.vaguehope.morrigan.config.Bundles;
import com.vaguehope.morrigan.gui.Activator;
import com.vaguehope.morrigan.gui.dialogs.MorriganMsgDlg;

public class ServerTrim extends WorkbenchWindowControlContribution {

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	WorkbenchWindowControlContribution and GUI methods.

	private Label lblStatus;
	Button btnStartStop;

	@Override
	protected Control createControl (final Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		this.lblStatus = new Label(composite, SWT.NONE);
		this.btnStartStop = new Button(composite, SWT.PUSH);

		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		composite.setLayout(layout);

		this.lblStatus.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, true));
		this.btnStartStop.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, true));
		this.btnStartStop.addSelectionListener(this.btnStartStopListener);

		Activator.getContext().addBundleListener(this.bundleListener);
		updateStatus(getServerBundle(), false);

		return composite;
	}

	@Override
	public void dispose() {
		super.dispose();

		Activator.getContext().removeBundleListener(this.bundleListener);
	}

	@Override
	public boolean isDynamic() {
		return true;
	}

	private final BundleListener bundleListener = new BundleListener() {
		@Override
		public void bundleChanged (final BundleEvent event) {
			final Bundle bundle = event.getBundle();
			if (bundle.getSymbolicName().equals(Bundles.SERVER_BOOT.getPkgName())) {
				getWorkbenchWindow().getShell().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						updateStatus(bundle, true);
					}
				});
			}
		}
	};

	/**
	 * Must be called in UI thread.
	 * @param serverBundle
	 */
	protected void updateStatus (final Bundle serverBundle, final boolean update) {
		Boolean a = isBundleActive(serverBundle);
		if (a != null) {
			if (a.booleanValue()) {
				this.lblStatus.setText("Server active.");
				this.btnStartStop.setText("Stop");
			}
			else {
				this.lblStatus.setText("Server stopped.");
				this.btnStartStop.setText("Start");
			}
			this.btnStartStop.setEnabled(true);
		}
		else {
			this.lblStatus.setText("Server not installed.");
			this.btnStartStop.setEnabled(false);
			this.btnStartStop.setText("Start");
		}
		if (update) getParent().update(true);
	}

	private final SelectionListener btnStartStopListener = new SelectionAdapter() {
		@Override
		public void widgetSelected(final SelectionEvent e) {
			try {
				ServerTrim.this.btnStartStop.setEnabled(false);
				invertServer();
			}
			catch (BundleException e1) {
				new MorriganMsgDlg(e1).open();
			}
		}
	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected Bundle getServerBundle () {
		for (Bundle bundle : Activator.getContext().getBundles()) {
			if (bundle.getSymbolicName().equals(Bundles.SERVER_BOOT.getPkgName())) return bundle;
		}
		return null;
	}

	protected Boolean isBundleActive (final Bundle bundle) {
		if (bundle != null) return Boolean.valueOf(bundle.getState() == Bundle.ACTIVE);
		return null;
	}

	protected void startServer () throws BundleException {
		Bundle b = getServerBundle();
		if (b != null && b.getState() != Bundle.ACTIVE) {
			b.start();
		}
		else {
			// TODO
		}
	}

	protected void stopServer () throws BundleException {
		Bundle b = getServerBundle();
		if (b != null && b.getState() == Bundle.ACTIVE) {
			b.stop();
		}
		else {
			// TODO
		}
	}

	protected void invertServer () throws BundleException {
		Bundle b = getServerBundle();
		Boolean a = isBundleActive(b);
		if (a != null) {
			if (a.booleanValue()) {
				stopServer();
			}
			else {
				startServer();
			}
		}
		else {
			// TODO
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
