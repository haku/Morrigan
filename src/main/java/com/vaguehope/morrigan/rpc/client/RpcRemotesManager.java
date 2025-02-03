package com.vaguehope.morrigan.rpc.client;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import com.vaguehope.morrigan.Args;
import com.vaguehope.morrigan.Args.ArgsException;
import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.dlna.extcd.MetadataStorage;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.ListRef;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.MediaStorageLayer;

public class RpcRemotesManager {

	private static final String PRIVATE_KEY_FILE_NAME = "rpcPrivateKey.jwk";  // TODO move to Config class.
	private static final String METADATA_DB_DIR = "rpcmetadata";  // TODO move to Config class.

	private final RpcClient rpcClient;
	private final MediaFactory mediaFactory;
	private final Config config;
	private final RpcContentServlet rpcContentServer;
	private final RpcItemCache itemCache;

	public RpcRemotesManager(final Args args, final MediaFactory mediaFactory, final Config config) throws ArgsException, IOException {
		this(RemoteInstance.fromArgs(args), mediaFactory, config, args.isPrintAccessLog());
	}

	public RpcRemotesManager(final List<RemoteInstance> instances, final MediaFactory mediaFactory, final Config config, final boolean printAccessLog) throws ArgsException, IOException {
		final JwtIdentity credentials = makeCredentials(config);
		this.rpcClient = new RpcClient(instances, credentials);
		this.mediaFactory = mediaFactory;
		this.config = config;
		this.rpcContentServer = new RpcContentServlet(this.rpcClient);
		this.itemCache = new RpcItemCache();
	}

	private static JwtIdentity makeCredentials(final Config config) throws IOException {
		final File file = new File(config.getConfigDir(), PRIVATE_KEY_FILE_NAME);
		setPrivateKeyFilePermissions(file);
		final JwtIdentity credentials = new JwtIdentity(file, findHostName());
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

	public void start() throws ArgsException, MorriganException {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				RpcRemotesManager.this.rpcClient.shutdown();
			}
		});
		this.rpcClient.start();

		for (final RemoteInstance ri : this.rpcClient.getRemoteInstances()) {
			final ListRef ref = ListRef.forRpcNode(ri.getLocalIdentifier(), ListRef.RPC_ROOT_NODE_ID);
			final MediaStorageLayer storageLayer = this.mediaFactory.getStorageLayerWithNewItemFactory(getMetadataDbPath(ri.getLocalIdentifier()).getAbsolutePath());
			final MetadataStorage storage = new MetadataStorage(storageLayer);
			this.mediaFactory.addExternalList(new RpcMediaNodeList(ref, "", ri, this.rpcClient, this.itemCache, this.rpcContentServer, storage));
		}
	}

	private File getMetadataDbPath (final String id) {
		final File d = new File(this.config.getConfigDir(), METADATA_DB_DIR);
		if (!d.exists() && !d.mkdirs() && !d.exists()) throw new IllegalStateException("Failed to create direactory '" + d.getAbsolutePath() + "'.");
		return new File(d, id);
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
