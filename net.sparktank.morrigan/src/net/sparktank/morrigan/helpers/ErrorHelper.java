package net.sparktank.morrigan.helpers;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class ErrorHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public String getStackTrace (Throwable t) {
		final Writer writer = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(writer);
		t.printStackTrace(printWriter);
		return writer.toString();
	}
	
	static public String getCauseTrace (Throwable t) {
		if (t != null) {
			StringBuilder sb = new StringBuilder();
			
			boolean first = true;
			Throwable c = t;
			while (true) {
				if (!first) sb.append("\n\ncaused by:\n   ");
				first = false;
				
				sb.append(c.getClass().getName());
				sb.append(": ");
				sb.append(c.getMessage());
				
				c = c.getCause();
				if (c==null) break;
			}
			
			return sb.toString();
		}
		else {
			return "Unable to display error message as Throwable object is null.";
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
