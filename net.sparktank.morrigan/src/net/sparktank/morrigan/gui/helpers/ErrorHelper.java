package net.sparktank.morrigan.gui.helpers;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class ErrorHelper {
	
	static public String getStackTrace (Throwable t) {
		final Writer writer = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(writer);
		t.printStackTrace(printWriter);
		return writer.toString();
	}
	
}
