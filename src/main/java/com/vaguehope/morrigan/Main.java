package com.vaguehope.morrigan;

import java.io.PrintStream;
import java.util.concurrent.Executors;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.playbackimpl.vlc.VlcEngineFactory;
import com.vaguehope.morrigan.tasks.AsyncTasksRegister;
import com.vaguehope.morrigan.tasks.AsyncTasksRegisterImpl;
import com.vaguehope.morrigan.util.DaemonThreadFactory;
import com.vaguehope.morrigan.util.LogHelper;


public final class Main {

	private static final Logger LOG = LoggerFactory.getLogger(Main.class);

	private Main () {
		throw new AssertionError();
	}

	public static void main (final String[] rawArgs) throws Exception { // NOSONAR
		LogHelper.bridgeJul();

		final PrintStream err = System.err;
		final Args args = new Args();
		final CmdLineParser parser = new CmdLineParser(args);
		try {
			parser.parseArgument(rawArgs);
			run(args);
		}
		catch (final CmdLineException e) {
			err.println(e.getMessage());
			help(parser, err);
			return;
		}
		catch (final Exception e) {
			err.println("An unhandled error occured.");
			e.printStackTrace(err);
			System.exit(1);
		}
	}

	private static void run (final Args args) throws Exception { // NOSONAR
		final AsyncTasksRegister asyncTasksRegister = new AsyncTasksRegisterImpl(
				Executors.newCachedThreadPool(new DaemonThreadFactory("tsk")));

		final VlcEngineFactory playbackEngineFactory = new VlcEngineFactory();

		// TODO
	}

	private static void help (final CmdLineParser parser, final PrintStream ps) {
		ps.print("Usage:");
		parser.printSingleLineUsage(ps);
		ps.println();
		parser.printUsage(ps);
		ps.println();
	}

}
