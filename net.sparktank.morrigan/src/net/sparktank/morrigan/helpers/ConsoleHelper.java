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
	
	static public void appendToConsole (String s) {
		getMessageConsoleStream().println(s);  
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private MessageConsoleStream messageConsoleStream = null;
	
	static private MessageConsoleStream getMessageConsoleStream () {
		if (messageConsoleStream == null) {
			messageConsole = getMessageConsole();
			messageConsoleStream = messageConsole.newMessageStream();
		}
		return messageConsoleStream;
	}
	
	static private MessageConsole messageConsole;
	
	static private MessageConsole getMessageConsole () {
		if (messageConsole == null) {
			messageConsole = new MessageConsole("Morrigan", null);
			ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { messageConsole });
		}
		return messageConsole;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
