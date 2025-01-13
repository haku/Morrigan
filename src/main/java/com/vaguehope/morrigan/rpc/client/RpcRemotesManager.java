package com.vaguehope.morrigan.rpc.client;

import java.io.File;
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

	private final RpcClient rpcClient;
	private final MediaFactory mediaFactory;
	private final Config config;
	private final RpcContentServlet rpcContentServer;

	public RpcRemotesManager(final Args args, final MediaFactory mediaFactory, final Config config) throws ArgsException {
		this(RemoteInstance.fromArgs(args), mediaFactory, config, args.isPrintAccessLog());
	}

	public RpcRemotesManager(final List<RemoteInstance> instances, final MediaFactory mediaFactory, final Config config, final boolean printAccessLog) throws ArgsException {
		this.mediaFactory = mediaFactory;
		this.rpcClient = new RpcClient(instances);
		this.config = config;
		this.rpcContentServer = new RpcContentServlet(this.rpcClient);
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
			this.mediaFactory.addExternalList(new RpcMediaNodeList(ref, "", ri, this.rpcClient, this.rpcContentServer, storage));
		}
	}

	private static final String METADATA_DB_DIR = "rpcmetadata";  // TODO move to Config class.

	private File getMetadataDbPath (final String id) {
		final File d = new File(this.config.getConfigDir(), METADATA_DB_DIR);
		if (!d.exists() && !d.mkdirs() && !d.exists()) throw new IllegalStateException("Failed to create direactory '" + d.getAbsolutePath() + "'.");
		return new File(d, id);
	}

}
