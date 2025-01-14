package com.vaguehope.morrigan.rpc.client;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.vaguehope.dlnatoad.rpc.MediaGrpc;
import com.vaguehope.dlnatoad.rpc.MediaGrpc.MediaBlockingStub;
import com.vaguehope.morrigan.Args.ArgsException;

import io.grpc.ManagedChannel;

public class RpcClient {

	private final List<RemoteInstance> remoteInstances;
	private final Map<RemoteInstance, ManagedChannel> managedChannels = new ConcurrentHashMap<>();
	private final Map<String, MediaBlockingStub> mediaBlockingStubs = new ConcurrentHashMap<>();

	public RpcClient(final List<RemoteInstance> remoteInstances) throws ArgsException {
		this.remoteInstances = remoteInstances;
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
		return stub.withDeadlineAfter(1, TimeUnit.MINUTES);
	}

	private void startChannel(final RemoteInstance ri) {
		final ManagedChannel channel = ri.getTarget().buildChannel();
		this.managedChannels.put(ri, channel);
		this.mediaBlockingStubs.put(ri.getLocalIdentifier(), MediaGrpc.newBlockingStub(channel));
	}

}
