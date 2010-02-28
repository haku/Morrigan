package net.sparktank.morrigan.dialogs;

public class RunnableDialog implements Runnable {
	
	private String s;
	private Exception e;
	
	public RunnableDialog (String s) {
		this.s = s;
	}
	
	public RunnableDialog (Exception e) {
		this.e = e;
	}
	
	@Override
	public void run() {
		if (s!=null) {
			new MorriganMsgDlg(s).open();
		} else if (e!=null) {
			new MorriganMsgDlg(e).open();
		} else {
			new MorriganMsgDlg("null").open();
		}
	}
	
}
