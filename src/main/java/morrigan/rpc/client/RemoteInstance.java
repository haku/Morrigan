package morrigan.rpc.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.vaguehope.common.rpc.RpcTarget;
import com.vaguehope.common.rpc.RpcTarget.RpcConfigException;

import morrigan.Args;
import morrigan.Args.ArgsException;

public class RemoteInstance {

	private static Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z0-9-_]+$");

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

	public static List<RemoteInstance> fromArgs(final Args args) throws ArgsException {
		final Map<String, RemoteInstance> ret = new HashMap<>();
		for (final String rawArg : args.getRemotes()) {
			final int x = rawArg.indexOf('|');
			if (x < 0) throw new ArgsException("Invalid remote: " + rawArg);

			final String url = rawArg.substring(0,  x);
			final String identifier = rawArg.substring(x + 1);
			if (StringUtils.isBlank(url)) throw new ArgsException("Invalid URL: " + rawArg);
			if (StringUtils.isBlank(identifier)) throw new ArgsException("Invalid identifier: " + rawArg);
			if (!IDENTIFIER_PATTERN.matcher(identifier).find()) throw new ArgsException("Invalid identifier: " + rawArg);

			RpcTarget target;
			try {
				target = RpcTarget.fromHttpUrl(url);
			}
			catch (final RpcConfigException e) {
				throw new ArgsException(e.getMessage());
			}
			if (ret.put(identifier, new RemoteInstance(identifier, target)) != null) throw new ArgsException("Duplicate identifier: " + identifier);
		}
		return new ArrayList<>(ret.values());
	}

}
