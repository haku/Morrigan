package com.vaguehope.morrigan.gui.dialogs;

public class RunnableDialog implements Runnable {
	
	private String s;
	private Throwable t;
	
	public RunnableDialog (String s) {
		this.s = s;
	}
	
	public RunnableDialog (Throwable t) {
		this.t = t;
	}
	
	@Override
	public void run() {
		if (this.s!=null) {
			new MorriganMsgDlg(this.s).open();
		} else if (this.t!=null) {
			new MorriganMsgDlg(this.t).open();
		} else {
			new MorriganMsgDlg("null").open();
		}
	}
	
}
