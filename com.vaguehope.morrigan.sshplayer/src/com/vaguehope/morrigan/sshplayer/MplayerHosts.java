package com.vaguehope.morrigan.sshplayer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.config.FileExtFilter;
import com.vaguehope.morrigan.util.PropertiesFile;

public class MplayerHosts {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String KEY_NAME = "name";
	private static final String KEY_HOST = "host";
	private static final String KEY_PASS = "pass";
	private static final String KEY_USER = "user";

	private static final String KEY_PORT = "port";
	private static final int DEFAULT_PORT = 22;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final Logger LOG = Logger.getLogger(CliPlayer.class.getName());

	private final String configDir = Config.getConfigDir() + "/sshp";
	private final AtomicReference<Collection<MplayerHost>> hosts = new AtomicReference<Collection<MplayerHost>>(Collections.<MplayerHost>emptyList());

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public void load () throws IOException {
		List<MplayerHost> list = new ArrayList<MplayerHost>();

		for (File file : configFiles()) {
			try {
				list.add(readHostFile(file));
			}
			catch (NotConfiguredException e) {
				LOG.warning(e.getMessage());
			}
		}

		this.hosts.set(list);
	}

	public Collection<MplayerHost> getHosts () {
		return Collections.unmodifiableCollection(this.hosts.get());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private File[] configFiles () {
		File dir = new File(this.configDir);
		if (!dir.exists()) return new File[0];
		return dir.listFiles(new FileExtFilter(".properties"));
	}

	private static MplayerHost readHostFile (File file) throws IOException, NotConfiguredException {
		PropertiesFile propFile = new PropertiesFile(file.getAbsolutePath());
		String name = propFile.getString(KEY_NAME, null);
		String host = propFile.getString(KEY_HOST, null);
		String user = propFile.getString(KEY_USER, null);
		String pass = propFile.getString(KEY_PASS, null);
		int port = propFile.getInt(KEY_PORT, DEFAULT_PORT);

		if (name == null || host == null || user == null || pass == null) {
			throw new NotConfiguredException("Config incomplete: missing name, host, user or pass from '" + file.getAbsolutePath() + "'.");
		}

		return new MplayerHost(name, host, port, user, pass);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static class NotConfiguredException extends Exception {

		private static final long serialVersionUID = -9100329407703909774L;

		public NotConfiguredException (String msg) {
			super(msg);
		}

	}

}
