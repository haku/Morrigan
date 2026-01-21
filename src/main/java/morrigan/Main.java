package morrigan;

import java.io.PrintStream;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import morrigan.Args.ServerPlayerArgs;
import morrigan.config.Config;
import morrigan.dlna.DlnaService;
import morrigan.model.exceptions.MorriganException;
import morrigan.model.media.MediaFactory;
import morrigan.model.media.internal.MediaFactoryImpl;
import morrigan.player.PlayerRegister;
import morrigan.player.PlayerRegisterImpl;
import morrigan.player.PlayerStateStorage;
import morrigan.player.contentproxy.LocalHostContentServer;
import morrigan.rpc.client.RpcRemotesManager;
import morrigan.server.AsyncActions;
import morrigan.server.MorriganServer;
import morrigan.server.ServerConfig;
import morrigan.server.boot.ServerPlayerContainer;
import morrigan.sshui.SshUi;
import morrigan.tasks.AsyncTasksRegister;
import morrigan.tasks.AsyncTasksRegisterImpl;
import morrigan.transcode.Transcoder;
import morrigan.util.DaemonThreadFactory;
import morrigan.util.LogHelper;
import morrigan.vlc.discovery.VlcDiscovery;
import morrigan.vlc.player.VlcEngineFactory;

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
		if (args.isVerboseLog()) {
			final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
			final ConsoleAppender<ILoggingEvent> appender = (ConsoleAppender<ILoggingEvent>) context.getLogger(Logger.ROOT_LOGGER_NAME)
					.getAppender("CONSOLE");
			appender.clearAllFilters();
		}

		final Config config = Config.fromArgs(args);
		final ScheduledExecutorService playerEx = makePlayerEx();
		final VlcEngineFactory infoPlaybackEngineFactory = new VlcEngineFactory(playerEx, args.isVerboseLog(), Collections.emptyList());
		final MediaFactory mediaFactory = new MediaFactoryImpl(config, infoPlaybackEngineFactory);
		final PlayerRegister playerRegister = makePlayerRegister(config, args, playerEx, mediaFactory);

		final AsyncTasksRegister asyncTasksRegister = new AsyncTasksRegisterImpl(Executors.newCachedThreadPool(new DaemonThreadFactory("tsk")));
		final AsyncActions asyncActions = new AsyncActions(asyncTasksRegister, mediaFactory);
		final Transcoder transcoder = new Transcoder("srv");
		makeLocalPlayers(args, playerRegister, playerEx);

		final MorriganServer httpServer;
		final ServerConfig serverConfig = new ServerConfig(config, args);
		if (args.getHttpPort() > 0) {
			final ScheduledExecutorService srvSchEx = Executors.newScheduledThreadPool(1, new DaemonThreadFactory("srvsch"));
			httpServer = new MorriganServer(args, config, serverConfig, playerRegister, mediaFactory, asyncTasksRegister, asyncActions, transcoder, srvSchEx);
			httpServer.start();
		}
		else {
			httpServer = null;
		}

		if (args.getSshPort() > 0) {
			final SshUi sshUi = new SshUi(args.getSshPort(), args.getSshInterfaces(), config, serverConfig, playerRegister, mediaFactory, asyncTasksRegister, transcoder);
			sshUi.start();
		}

		if (args.isDlna()) {
			final DlnaService dlna = new DlnaService(args, config, serverConfig, mediaFactory, playerRegister);
			dlna.start();

			if (httpServer != null) httpServer.enableDlnaCtl(dlna);
		}

		if (args.getRemotes().size() > 0) {
			final RpcRemotesManager remotes = new RpcRemotesManager(args, mediaFactory, config);
			remotes.start();
		}

		if (args.isVlcDiscovery()) {
			final VlcDiscovery vlcDiscovery = new VlcDiscovery(args, playerRegister, playerEx);
			vlcDiscovery.start();
		}

		new CountDownLatch(1).await();  // Block forever.
	}

	private static ScheduledExecutorService makePlayerEx() {
		final ScheduledThreadPoolExecutor playerEx = new ScheduledThreadPoolExecutor(1, new DaemonThreadFactory("player"));
		playerEx.setKeepAliveTime(1, TimeUnit.MINUTES);
		playerEx.allowCoreThreadTimeOut(true);
		return playerEx;
	}

	private static PlayerRegister makePlayerRegister(final Config config, Args args, final ScheduledExecutorService playerEx, final MediaFactory mediaFactory) throws MorriganException {
		final LocalHostContentServer localHttpServer = new LocalHostContentServer(args.isPrintAccessLog());
		localHttpServer.start();

		final PlayerRegisterImpl playerRegister = new PlayerRegisterImpl(
				new PlayerStateStorage(mediaFactory, playerEx, config),
				mediaFactory,
				config,
				localHttpServer,
				playerEx);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			playerRegister.dispose();
		}));

		return playerRegister;
	}

	private static void makeLocalPlayers(final Args args, final PlayerRegister playerRegister, final ScheduledExecutorService playerEx) {
		for (final ServerPlayerArgs a : args.getServerPlayers()) {
			LOG.info("Making local player '{}' with vlc args: {}", a.getName(), a.getVlcArgs());
			final ServerPlayerContainer pc = new ServerPlayerContainer(a.getName());
			final VlcEngineFactory engineFactory = new VlcEngineFactory(playerEx, args.isVerboseLog(), a.getVlcArgs());
			pc.setPlayer(playerRegister.makeLocal(pc.getPrefix(), pc.getName(), engineFactory));

			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				pc.dispose();
			}));
		}
	}

	private static void help (final CmdLineParser parser, final PrintStream ps) {
		ps.print("Usage:");
		parser.printSingleLineUsage(ps);
		ps.println();
		parser.printUsage(ps);
		ps.println();
	}

}
