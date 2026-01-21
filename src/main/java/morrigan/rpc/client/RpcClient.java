package morrigan.rpc.client;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.grpc.CallCredentials;
import io.grpc.ManagedChannel;
import mediatoad.rpc.MediaGrpc;
import mediatoad.rpc.MediaGrpc.MediaBlockingStub;
import morrigan.Args.ArgsException;

public class RpcClient {

	private final List<RemoteInstance> remoteInstances;
	private final Map<RemoteInstance, ManagedChannel> managedChannels = new ConcurrentHashMap<>();
	private final Map<String, MediaBlockingStub> mediaBlockingStubs = new ConcurrentHashMap<>();
	private final CallCredentials callCredentials;

	public RpcClient(final List<RemoteInstance> remoteInstances, CallCredentials callCredentials) throws ArgsException {
		this.remoteInstances = remoteInstances;
		this.callCredentials = callCredentials;
	}

	public void start() {
		for (final RemoteInstance ri : this.remoteInstances) {
			startChannel(ri);
		}
	}

	public void shutdown() {
		for (final Entry<RemoteInstance, ManagedChannel> e : this.managedChannels.entrySet()) {
			e.getValue().shutdown();
		}

		for (final Entry<RemoteInstance, ManagedChannel> e : this.managedChannels.entrySet()) {
			try {
				e.getValue().awaitTermination(30, TimeUnit.SECONDS);
			}
			catch (final InterruptedException e1) {
				// oh well we tried.
			}
		}
	}

	public List<RemoteInstance> getRemoteInstances() {
		return this.remoteInstances;
	}

	public MediaBlockingStub getMediaBlockingStub(final String rid) {
		final MediaBlockingStub stub = this.mediaBlockingStubs.get(rid);
		if (stub == null) throw new IllegalArgumentException("No stub found for: " + rid);
		return stub
				.withCallCredentials(this.callCredentials)
				.withDeadlineAfter(1, TimeUnit.MINUTES);
	}

	private void startChannel(final RemoteInstance ri) {
		final ManagedChannel channel = ri.getTarget().buildChannel();
		this.managedChannels.put(ri, channel);
		this.mediaBlockingStubs.put(ri.getLocalIdentifier(), MediaGrpc.newBlockingStub(channel));
	}

}
