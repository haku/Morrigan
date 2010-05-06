package net.sparktank.morrigan.gui.actions;

import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.server.MorriganServer;
import net.sparktank.morrigan.server.ServerMain;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

public class ServerAction extends Action implements IWorkbenchAction {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public ServerAction () {
		super("Server", AS_CHECK_BOX);
	}
	
	@Override
	public void dispose() {
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getId() { return "server"; }
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	MorriganServer _server = null;
	
	private MorriganServer getServer () throws Exception {
		if (_server == null) {
			_server = ServerMain.makeServer();
			
			_server.setOnStopRunnable(new Runnable() {
				@Override
				public void run() {
					setChecked(false);
				}
			});
			
		}
		return _server;
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
