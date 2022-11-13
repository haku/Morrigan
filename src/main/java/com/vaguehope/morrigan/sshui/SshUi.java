package com.vaguehope.morrigan.sshui;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.sshd.core.CoreModuleProperties;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.config.keys.AuthorizedKeysAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.player.PlayerReader;
import com.vaguehope.morrigan.server.ServerConfig;
import com.vaguehope.morrigan.sshui.ssh.MnPasswordAuthenticator;
import com.vaguehope.morrigan.tasks.AsyncTasksRegister;
import com.vaguehope.morrigan.transcode.Transcoder;
import com.vaguehope.morrigan.util.DaemonThreadFactory;

public class SshUi {

	// can be DSA/RSA/EC (http://docs.oracle.com/javase/6/docs/technotes/guides/security/StandardNames.html#KeyPairGenerator)
	private static final String HOSTKEY_NAME = "hostkey.ser";  // TODO move to Config class.
	private static final String KEYS_FILE_NAME = "authorized_keys";
	private static final Duration IDLE_TIMEOUT = Duration.ofDays(1);
	private static final Logger LOG = LoggerFactory.getLogger(SshUi.class);

	private final int port;
	private final List<InetAddress> bindAddresses;
	private final Config config;
	private final ServerConfig serverConfig;
	private final PlayerReader playerReader;
	private final MediaFactory mediaFactory;
	private final AsyncTasksRegister asyncTasksRegister;
	private final Transcoder transcoder;

	private ExecutorService unreliableEs;
	private MnCommandFactory mnCommandFactory;
	private SshServer sshd;

	public SshUi(final int port, final List<InetAddress> bindAddresses, final Config config, final ServerConfig serverConfig, final PlayerReader playerReader, final MediaFactory mediaFactory, final AsyncTasksRegister asyncTasksRegister, final Transcoder transcoder) {
		this.port = port;
		this.bindAddresses = bindAddresses;
		this.config = config;
		this.serverConfig = serverConfig;
		this.playerReader = playerReader;
		this.mediaFactory = mediaFactory;
		this.asyncTasksRegister = asyncTasksRegister;
		this.transcoder = transcoder;
	}

	public void start() throws IOException {
		final File hostKey = new File(this.config.getConfigDir(), HOSTKEY_NAME);
		LOG.info("Host key: {}", hostKey.getAbsolutePath());

		this.unreliableEs = new ThreadPoolExecutor(0, 1,
				1L, TimeUnit.MINUTES,
				new LinkedBlockingQueue<Runnable>(1),
				new DaemonThreadFactory("sshbg"),
				new ThreadPoolExecutor.DiscardOldestPolicy());

		final MnContext mnContext = new MnContext(
				this.playerReader, this.mediaFactory, this.asyncTasksRegister, this.transcoder,
				this.config, new UserPrefs(this.config), this.unreliableEs);
		this.mnCommandFactory = new MnCommandFactory(mnContext);

		this.sshd = SshServer.setUpDefaultServer();
		this.sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(hostKey.toPath()));
		this.sshd.setShellFactory(this.mnCommandFactory);
		this.sshd.setPasswordAuthenticator(new MnPasswordAuthenticator(this.serverConfig));

		final File AuthKeyFile = new File(this.config.getConfigDir(), KEYS_FILE_NAME);
		if (AuthKeyFile.exists()) {
			this.sshd.setPublickeyAuthenticator(new AuthorizedKeysAuthenticator(AuthKeyFile.toPath()));
		}

		CoreModuleProperties.IDLE_TIMEOUT.set(this.sshd, IDLE_TIMEOUT);

		if (this.bindAddresses != null) {
			final String hosts = this.bindAddresses.stream().map(a -> a.getHostAddress()).collect(Collectors.joining(","));
			this.sshd.setHost(hosts);
		}
		else {
			final InetAddress bindAddress = this.serverConfig.getBindAddress("SSH");
			if (bindAddress == null) throw new IllegalStateException("Failed to find bind address.");
			this.sshd.setHost(bindAddress.getHostAddress());
		}

		this.sshd.setPort(this.port);
		this.sshd.start();
		LOG.info("sshUI ready on: {}.", this.sshd.getBoundAddresses().stream().map(a -> a.toString()).collect(Collectors.joining(", ")));
	}

}
