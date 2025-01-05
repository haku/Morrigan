package com.vaguehope.morrigan.rpc;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.vaguehope.morrigan.Args;
import com.vaguehope.morrigan.Args.ArgsException;

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;

public class RpcTarget {

	private final String target;
	private final boolean plainText;

	public RpcTarget(final String target, final boolean plainText) {
		this.target = target;
		this.plainText = plainText;
	}

	public ManagedChannel buildChannel() {
		final ChannelCredentials channelCredentials;
		if (this.plainText) {
			channelCredentials = InsecureChannelCredentials.create();
		}
		else {
			channelCredentials = TlsChannelCredentials.create();
		}
		final ManagedChannel chan = Grpc.newChannelBuilder(this.target, channelCredentials)
				.intercept(RpcMetrics.clientInterceptor(this.target))
				.build();
		RpcMetrics.monitorChannel(chan, this.target);
		return chan;
	}

	@Override
	public String toString() {
		return String.format("RpcTarget{%s, %s}", this.target, this.plainText);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.target, this.plainText);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (!(obj instanceof RpcTarget)) return false;
		final RpcTarget that = (RpcTarget) obj;
		return Objects.equals(this.target, that.target)
				&& Objects.equals(this.plainText, that.plainText);
	}

	public static RpcTarget fromHttpUrl(final String http) throws ArgsException {
		final URI uri;
		try {
			uri = new URI(http);
		}
		catch (final URISyntaxException e) {
			throw new Args.ArgsException("Invalid URI: " + http);
		}

		if (StringUtils.isAllBlank(uri.getHost())) {
			throw new Args.ArgsException("Invalid host: " + http);
		}

		int port;
		boolean plainText;
		if ("https".equals(uri.getScheme().toLowerCase())) {
			port = 443;
			plainText = false;
		}
		else if ("http".equals(uri.getScheme().toLowerCase())) {
			port = 80;
			plainText = true;
		}
		else {
			throw new Args.ArgsException("Invalid scheme: " + uri.getScheme());
		}
		if (uri.getPort() > 0) port = uri.getPort();

		// 3 /// because https://grpc.io/docs/guides/custom-name-resolution/
		final String target = String.format("dns:///%s:%s/", uri.getHost(), port);
		return new RpcTarget(target, plainText);
	}

}
