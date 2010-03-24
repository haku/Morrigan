package net.sparktank.morrigan.helpers;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

public class ConsoleHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public void showConsole () {
		ConsolePlugin.getDefault().getConsoleManager().showConsoleView(getMessageConsole());
	}
	
	static private boolean firstAppend = true;
	
	static public void appendToConsole (String s) {
		getMessageConsoleStream().print(firstAppend ? s :"\n" + s);
		firstAppend = false;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private Object messageConsoleStreamLock = new Object();
	static private MessageConsoleStream messageConsoleStream = null;
	
	static private MessageConsoleStream getMessageConsoleStream () {
		synchronized (messageConsoleStreamLock) {
			if (messageConsoleStream == null) {
				messageConsole = getMessageConsole();
				messageConsole.setWaterMarks(9000, 10000);
				messageConsoleStream = messageConsole.newMessageStream();
			}
		}
		return messageConsoleStream;
	}
	
	static private Object messageConsoleLock = new Object();
	static private MessageConsole messageConsole;
	
	static private MessageConsole getMessageConsole () {
		synchronized (messageConsoleLock) {
			if (messageConsole == null) {
				messageConsole = new MessageConsole("Morrigan", null);
				ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { messageConsole });
			}
		}
		return messageConsole;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
