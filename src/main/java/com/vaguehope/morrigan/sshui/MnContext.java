package com.vaguehope.morrigan.sshui;

import java.util.concurrent.ExecutorService;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.player.PlayerReader;
import com.vaguehope.morrigan.tasks.AsyncTasksRegister;
import com.vaguehope.morrigan.transcode.Transcoder;

public class MnContext {

	private final PlayerReader playerReader;
	private final MediaFactory mediaFactory;
	private final AsyncTasksRegister asyncTasksRegister;
	private final Transcoder transcoder;
	private final Config config;
	private final UserPrefs userPrefs;
	private final ExecutorService unreliableEs;

	public MnContext (
			final PlayerReader playerReader,
			final MediaFactory mediaFactory,
			final AsyncTasksRegister asyncTasksRegister,
			final Transcoder transcoder,
			final Config config,
			final UserPrefs userPrefs,
			final ExecutorService bgEs) {
		this.playerReader = playerReader;
		this.mediaFactory = mediaFactory;
		this.asyncTasksRegister = asyncTasksRegister;
		this.transcoder = transcoder;
		this.config = config;
		this.userPrefs = userPrefs;
		this.unreliableEs = bgEs;
	}

	public PlayerReader getPlayerReader () {
		return this.playerReader;
	}

	public MediaFactory getMediaFactory () {
		return this.mediaFactory;
	}

	public AsyncTasksRegister getAsyncTasksRegister () {
		return this.asyncTasksRegister;
	}

	public Transcoder getTranscoder() {
		return this.transcoder;
	}

	public Config getConfig() {
		return this.config;
	}

	public UserPrefs getUserPrefs () {
		return this.userPrefs;
	}

	public ExecutorService getUnreliableEs () {
		return this.unreliableEs;
	}

}
