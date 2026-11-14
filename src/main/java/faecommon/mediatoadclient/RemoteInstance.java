package faecommon.mediatoadclient;

import java.util.Objects;

import faecommon.rpc.RpcTarget;

public class RemoteInstance {

	private final String localIdentifier;
	private final RpcTarget target;

	public RemoteInstance(final String localIdentifier, final RpcTarget target) {
		this.localIdentifier = localIdentifier;
		this.target = target;
	}

	public String getLocalIdentifier() {
		return this.localIdentifier;
	}

	public RpcTarget getTarget() {
		return this.target;
	}

	@Override
	public String toString() {
		return String.format("RemoteInstance{%s, %s}", this.localIdentifier, this.target);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.localIdentifier, this.target);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (!(obj instanceof RemoteInstance)) return false;
		final RemoteInstance that = (RemoteInstance) obj;
		return Objects.equals(this.target, that.target)
				&& Objects.equals(this.localIdentifier, that.localIdentifier);
	}

}
