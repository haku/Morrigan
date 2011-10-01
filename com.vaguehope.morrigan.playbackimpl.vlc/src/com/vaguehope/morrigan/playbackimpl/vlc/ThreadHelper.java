package com.vaguehope.morrigan.playbackimpl.vlc;

import org.eclipse.swt.widgets.Widget;

public class ThreadHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private ThreadHelper () { /* Unused */ }
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	UI Threading.
	
	static public void runInUiThread (Widget w, Runnable r) {
		if (w.getDisplay().getThread().equals(Thread.currentThread())) {
			r.run();
		}
		else {
			w.getDisplay().syncExec(r);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
