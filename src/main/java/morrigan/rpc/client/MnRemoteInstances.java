package morrigan.rpc.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import faecommon.mediatoadclient.RemoteInstance;
import faecommon.rpc.RpcTarget;
import faecommon.rpc.RpcTarget.RpcConfigException;
import morrigan.Args;
import morrigan.Args.ArgsException;

public class MnRemoteInstances {

	private static Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z0-9-_]+$");

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
