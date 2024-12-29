package com.vaguehope.morrigan.sshui;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.vaguehope.morrigan.model.media.IMediaItemDb;
import com.vaguehope.morrigan.util.FileHelper;

public class SessionState {

	public final AtomicReference<File> initialDir;

	private final Map<IMediaItemDb, AtomicReference<String>> savedSearchTerms = new ConcurrentHashMap<>();

	public SessionState() {
		this.initialDir = new AtomicReference<>(FileHelper.getSomeRootDir());
	}

	public AtomicReference<String> savedSearchTerm(final IMediaItemDb db) {
		final AtomicReference<String> v = this.savedSearchTerms.get(db);
		if (v != null) return v;

		final AtomicReference<String> newV = new AtomicReference<>();
		final AtomicReference<String> oldV = this.savedSearchTerms.putIfAbsent(db, newV);
		if (oldV != null) return oldV;
		return newV;
	}

}
