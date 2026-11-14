package morrigan.rpc.client;

import java.io.File;
import java.io.IOException;
import java.util.List;

import faecommon.mediatoadclient.RemoteInstance;
import faecommon.mediatoadclient.RpcRemotesManager;
import morrigan.Args;
import morrigan.Args.ArgsException;
import morrigan.config.Config;
import morrigan.dlna.extcd.MetadataStorage;
import morrigan.model.media.ListRef;
import morrigan.model.media.MediaFactory;
import morrigan.model.media.MediaStorageLayer;
import morrigan.sqlitewrapper.DbException;

public class MnRpcRemotesManager extends RpcRemotesManager {

	private static final String METADATA_DB_DIR = "rpcmetadata";  // TODO move to Config class.

	private final MediaFactory mediaFactory;
	private final Config config;
	private final RpcContentServlet rpcContentServer;
	private final RpcItemCache itemCache;

	public MnRpcRemotesManager(final Args args, final MediaFactory mediaFactory, final Config config) throws ArgsException, IOException {
		this(MnRemoteInstances.fromArgs(args), mediaFactory, config, args.isPrintAccessLog());
	}

	public MnRpcRemotesManager(final List<RemoteInstance> instances, final MediaFactory mediaFactory, final Config config, final boolean printAccessLog) throws ArgsException, IOException {
		super(instances, config.getConfigDir(), printAccessLog);
		this.mediaFactory = mediaFactory;
		this.config = config;
		this.rpcContentServer = new RpcContentServlet(this.rpcClient);
		this.itemCache = new RpcItemCache();
	}

	@Override
	public void start() {
		super.start();

		try {
			for (final RemoteInstance ri : this.rpcClient.getRemoteInstances()) {
				final ListRef ref = ListRef.forRpcNode(ri.getLocalIdentifier(), ListRef.RPC_ROOT_NODE_ID);
				final MediaStorageLayer storageLayer = this.mediaFactory.getStorageLayerWithNewItemFactory(getMetadataDbPath(ri.getLocalIdentifier()).getAbsolutePath());
				final MetadataStorage storage = new MetadataStorage(storageLayer);
				this.mediaFactory.addExternalList(new RpcMediaNodeList(ref, "", ri, this.rpcClient, this.itemCache, this.rpcContentServer, storage));
			}
		}
		catch (final DbException e) {
			// can probably think of a better exception class to put here.
			throw new IllegalStateException("Failed to setup RPC remotes.", e);
		}
	}

	private File getMetadataDbPath (final String id) {
		final File d = new File(this.config.getConfigDir(), METADATA_DB_DIR);
		if (!d.exists() && !d.mkdirs() && !d.exists()) throw new IllegalStateException("Failed to create direactory '" + d.getAbsolutePath() + "'.");
		return new File(d, id);
	}

}
