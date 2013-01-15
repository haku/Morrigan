package com.vaguehope.morrigan.gui.helpers;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

public final class ConsoleHelper {

	private ConsoleHelper () {}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static void showConsole () {
		ConsolePlugin.getDefault().getConsoleManager().showConsoleView(getMessageConsole());
	}

	public static void appendToConsole (String topic, String s) {
		// TODO create separate consoles for each topic?

		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(topic);
		sb.append("] ");
		sb.append(s);

		getMessageConsoleStream().println(sb.toString());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static Object messageConsoleStreamLock = new Object();
	private static MessageConsoleStream messageConsoleStream = null;

	private static MessageConsoleStream getMessageConsoleStream () {
		synchronized (messageConsoleStreamLock) {
			if (messageConsoleStream == null) {
				messageConsole = getMessageConsole();
				messageConsole.setWaterMarks(99000, 100000);
				messageConsoleStream = messageConsole.newMessageStream();
			}
		}
		return messageConsoleStream;
	}

	private static Object messageConsoleLock = new Object();
	private static MessageConsole messageConsole;

	private static MessageConsole getMessageConsole () {
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
