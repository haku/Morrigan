package net.sparktank.morrigan.dialogs;

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
		if (s!=null) {
			new MorriganMsgDlg(s).open();
		} else if (t!=null) {
			new MorriganMsgDlg(t).open();
		} else {
			new MorriganMsgDlg("null").open();
		}
	}
	
}
