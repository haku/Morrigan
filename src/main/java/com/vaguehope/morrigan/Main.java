package com.vaguehope.morrigan;

import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.internal.MediaFactoryImpl;
import com.vaguehope.morrigan.playbackimpl.vlc.VlcEngineFactory;
import com.vaguehope.morrigan.player.PlayerContainer;
import com.vaguehope.morrigan.player.PlayerRegister;
import com.vaguehope.morrigan.player.PlayerStateStorage;
import com.vaguehope.morrigan.player.internal.PlayerRegisterImpl;
import com.vaguehope.morrigan.server.AsyncActions;
import com.vaguehope.morrigan.server.MorriganServer;
import com.vaguehope.morrigan.server.ServerConfig;
import com.vaguehope.morrigan.server.boot.ServerPlayerContainer;
import com.vaguehope.morrigan.sshui.SshUi;
import com.vaguehope.morrigan.tasks.AsyncTasksRegister;
import com.vaguehope.morrigan.tasks.AsyncTasksRegisterImpl;
import com.vaguehope.morrigan.transcode.Transcoder;
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
		final MediaFactory mediaFactory = new MediaFactoryImpl(Config.DEFAULT, playbackEngineFactory);

		final ScheduledThreadPoolExecutor playerEx = new ScheduledThreadPoolExecutor(1, new DaemonThreadFactory("player"));
		playerEx.setKeepAliveTime(1, TimeUnit.MINUTES);
		playerEx.allowCoreThreadTimeOut(true);
		final PlayerRegister playerRegister = new PlayerRegisterImpl(playbackEngineFactory,
				new PlayerStateStorage(mediaFactory, playerEx), playerEx);

		final Transcoder transcoder = new Transcoder("srv");

		if (args.getSshPort() > 0) {
			final SshUi sshUi = new SshUi(args.getSshPort(), playerRegister, mediaFactory, asyncTasksRegister);
			sshUi.start();
		}

		final ServerConfig config = new ServerConfig(args);
		if (config.isServerPlayerEnabled()) {
			final ExecutorService srvEx = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new DaemonThreadFactory("srvplayer"));
			final ServerPlayerContainer playerContainer = new ServerPlayerContainer(srvEx);
			fillPlayerContainer(playerContainer, playerRegister);
		}

		final AsyncActions asyncActions = new AsyncActions(asyncTasksRegister, mediaFactory, Config.DEFAULT);
		final ScheduledExecutorService srvSchEx = Executors.newScheduledThreadPool(1, new DaemonThreadFactory("srvsch"));
		final MorriganServer server = new MorriganServer(config, playerRegister, mediaFactory, asyncTasksRegister, asyncActions, transcoder, srvSchEx);
		server.start();
		server.join();  // Block forever.
	}

	private static void fillPlayerContainer (final PlayerContainer container, final PlayerRegister playerRegister) {
		container.setPlayer(playerRegister.makeLocal(container.getPrefix(), container.getName(), container.getLocalPlayerSupport()));
	}

	private static void help (final CmdLineParser parser, final PrintStream ps) {
		ps.print("Usage:");
		parser.printSingleLineUsage(ps);
		ps.println();
		parser.printUsage(ps);
		ps.println();
	}

}
