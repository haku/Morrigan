package com.vaguehope.morrigan.sshplayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.util.PropertiesFile;

public class MplayerHosts {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String SERVER_PROPS = "sshplayer.properties";

	private static final String KEY_HOST = "host";
	private static final String KEY_PASS = "pass";
	private static final String KEY_USER = "user";

	private static final String KEY_PORT = "port";
	private static final int DEFAULT_PORT = 22;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final Logger LOG = Logger.getLogger(Mplayer.class.getName());

	private final String filepath = Config.getConfigDir() + '/' + SERVER_PROPS;
	private final AtomicReference<Collection<MplayerHost>> hosts = new AtomicReference<Collection<MplayerHost>>(Collections.<MplayerHost>emptyList());

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public void load () throws IOException {
		List<MplayerHost> list = new ArrayList<MplayerHost>();

		try {
			list.add(readHostFile(this.filepath));
		}
		catch (NotConfiguredException e) {
			LOG.warning(e.getMessage());
		}

		this.hosts.set(list);
	}

	public Collection<MplayerHost> getHosts () {
		return Collections.unmodifiableCollection(this.hosts.get());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private MplayerHost readHostFile (String file) throws IOException, NotConfiguredException {
		PropertiesFile propFile = new PropertiesFile(file);
		String host = propFile.getString(KEY_HOST, null);
		String user = propFile.getString(KEY_USER, null);
		String pass = propFile.getString(KEY_PASS, null);

		if (host == null || user == null || pass == null) {
			throw new NotConfiguredException("Config incomplete: missing host, user or pass from '" + this.filepath + "'.");
		}

		int port = propFile.getInt(KEY_PORT, DEFAULT_PORT);

		MplayerHost h = new MplayerHost(host, port, user, pass);
		return h;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static class NotConfiguredException extends Exception {

		private static final long serialVersionUID = -9100329407703909774L;

		public NotConfiguredException (String msg) {
			super(msg);
		}

	}

}
