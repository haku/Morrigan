package net.sparktank.morrigan.dialogs;

public class RunnableDialog implements Runnable {
	
	private Exception e;
	
	public RunnableDialog (Exception e) {
		this.e = e;
	}

	@Override
	public void run() {
		new MorriganMsgDlg(e);
	}
	
}
