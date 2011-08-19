package com.vaguehope.morrigan.gui.actions;


import org.eclipse.jface.action.Action;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

import com.vaguehope.morrigan.gui.dialogs.MorriganMsgDlg;
import com.vaguehope.morrigan.server.MorriganServer;
import com.vaguehope.morrigan.server.ServerMain;

@Deprecated
public class ServerAction extends Action implements IWorkbenchAction {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	// We only want 1 instance, as there is only 1 server.
	private static ServerAction instance;
	
	public static synchronized ServerAction getInstance () {
		if (instance == null) {
			instance = new ServerAction();
		}
		return instance;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private ServerAction () {
		super("Server", AS_CHECK_BOX);
	}
	
	@Override
	public void dispose() {/* UNUSED */}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getId() { return "server"; }
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	MorriganServer _server = null;
	
	private MorriganServer getServer () throws Exception {
		if (this._server == null) {
			this._server = ServerMain.makeServer();
			
			this._server.setOnStopRunnable(new Runnable() {
				@Override
				public void run() {
					setChecked(false);
				}
			});
			
		}
		return this._server;
	}
	
	@Override
	public void run() {
		if (isChecked()) {
			try {
				getServer().start();
			} catch (Exception e) {
				new MorriganMsgDlg(e).open();
				return;
			}
			
		} else {
			try {
				getServer().stop();
				// TODO now save reference in a WeakReference so it can be GCed? 
			} catch (Exception e) {
				new MorriganMsgDlg(e).open();
				return;
			}
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
