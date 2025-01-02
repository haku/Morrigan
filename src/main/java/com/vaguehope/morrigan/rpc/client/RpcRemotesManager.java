package com.vaguehope.morrigan.rpc.client;

import java.io.File;
import java.util.List;
import java.util.function.Function;

import com.vaguehope.morrigan.Args;
import com.vaguehope.morrigan.Args.ArgsException;
import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer;
import com.vaguehope.morrigan.model.media.MediaFactory;

public class RpcRemotesManager {

	private final RpcClient rpcClient;
	private final MediaFactory mediaFactory;
	private final Config config;
	private final LocalHostServer localHttpServer;
	private final TransientContentIds transientContentIds;

	public RpcRemotesManager(final Args args, final MediaFactory mediaFactory, final Config config) throws ArgsException {
		this(RemoteInstance.fromArgs(args), mediaFactory, config);
	}

	public RpcRemotesManager(final List<RemoteInstance> instances, final MediaFactory mediaFactory, final Config config) throws ArgsException {
		this.mediaFactory = mediaFactory;
		this.rpcClient = new RpcClient(instances);
		this.config = config;
		this.transientContentIds = new TransientContentIds();
		this.localHttpServer = new LocalHostServer(new RpcContentServlet(this.transientContentIds, this.rpcClient));
	}

	public void start() throws ArgsException, MorriganException {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				RpcRemotesManager.this.rpcClient.shutdown();
			}
		});
		this.rpcClient.start();
		this.localHttpServer.start();

		for (final RemoteInstance ri : this.rpcClient.getRemoteInstances()) {
			final Function<String, String> itemRemoteLocation = (itemId) -> {
				String id = this.transientContentIds.makeId(ri.getLocalIdentifier(), itemId);
				return this.localHttpServer.uriFor(id);
			};
			final IMediaItemStorageLayer storage = this.mediaFactory.getStorageLayer(getMetadataDbPath(ri.getLocalIdentifier()).getAbsolutePath());
			this.mediaFactory.addExternalList(new RpcMediaList(ri, this.rpcClient, itemRemoteLocation, storage));
		}
	}

	private static final String METADATA_DB_DIR = "rpcmetadata";  // TODO move to Config class.

	private File getMetadataDbPath (final String id) {
		final File d = new File(this.config.getConfigDir(), METADATA_DB_DIR);
		if (!d.exists() && !d.mkdirs() && !d.exists()) throw new IllegalStateException("Failed to create direactory '" + d.getAbsolutePath() + "'.");
		return new File(d, id);
	}

}
