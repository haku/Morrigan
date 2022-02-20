package com.vaguehope.morrigan.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.CyclicBufferAppender;

public class LogServlet extends HttpServlet {

	public static final String CONTEXTPATH = "/log";

	private static final String LAYOUT = "%.-1level%d{MMdd HH:mm:ss.SSS} [%10.10thread] %25.25logger{25} %msg%n";
	private static final String CYCLIC_BUFFER_APPENDER_NAME = "CYCLIC";
	private static final long serialVersionUID = -4449366278899829464L;

	@SuppressWarnings("resource")
	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/plain;charset=utf-8");
		printLogs(resp.getWriter());
	}

	private static void printLogs(final PrintWriter p) {
		final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		final CyclicBufferAppender<ILoggingEvent> b = (CyclicBufferAppender<ILoggingEvent>) context.getLogger(Logger.ROOT_LOGGER_NAME)
				.getAppender(CYCLIC_BUFFER_APPENDER_NAME);

		if (b == null) {
			p.println("Cyclic buffer not found.");
			return;
		}

		synchronized (b) {
			final int length = b.getLength();
			if (length < 1) {
				p.println("Empty buffer.");
				return;
			}

			final PatternLayout layout = new PatternLayout();
			layout.setContext(context);
			layout.setPattern(LAYOUT);
			layout.start();

			for (int i = 0; i < length; i++) {
				p.append(layout.doLayout(b.get(i)));
			}
		}
	}

}
