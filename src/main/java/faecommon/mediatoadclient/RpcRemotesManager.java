package faecommon.mediatoadclient;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public class RpcRemotesManager {

	private static final String PRIVATE_KEY_FILE_NAME = "rpcPrivateKey.jwk";

	protected final RpcClient rpcClient;

	public RpcRemotesManager(final List<RemoteInstance> instances, final File configDir, final String appName, final boolean printAccessLog) throws IOException {
		final JwtIdentity credentials = makeCredentials(configDir, appName);
		this.rpcClient = new RpcClient(instances, credentials);
	}

	private static JwtIdentity makeCredentials(final File configDir, final String appName) throws IOException {
		final File file = new File(configDir, PRIVATE_KEY_FILE_NAME);
		setPrivateKeyFilePermissions(file);
		final JwtIdentity credentials = new JwtIdentity(file, appName, findHostName());
		setPrivateKeyFilePermissions(file);
		return credentials;
	}

	private static void setPrivateKeyFilePermissions(final File file) throws IOException {
		if (!file.exists()) file.createNewFile();
		// there does not appear to be any way to check current permissions, so can only set them to what they should be.
		// only way to clear group and other bits seems to be to clear them all, then just set owner bits.
		if (!file.setReadable(false, false) | !file.setReadable(true, true)) {
			throw new IOException("Failed to set readability of: " + file);
		}
		if (!file.setWritable(false, false) | !file.setWritable(true, true)) {
			throw new IOException("Failed to set writability of: " + file);
		}
	}

	public void start() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				RpcRemotesManager.this.rpcClient.shutdown();
			}
		});
		this.rpcClient.start();
	}

	public RpcClient getClient() {
		return this.rpcClient;
	}

	private static String findHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		}
		catch (final UnknownHostException e) {
			throw new IllegalStateException("Failed to determine hostname.");
		}
	}

}
