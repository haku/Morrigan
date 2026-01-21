package morrigan.sshui;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import morrigan.model.media.MediaList;
import morrigan.util.FileHelper;

public class SessionState {

	public final AtomicReference<File> initialDir;

	private final Map<MediaList, AtomicReference<String>> savedSearchTerms = new ConcurrentHashMap<>();

	public SessionState() {
		this.initialDir = new AtomicReference<>(FileHelper.getSomeRootDir());
	}

	public AtomicReference<String> savedSearchTerm(final MediaList db) {
		final AtomicReference<String> v = this.savedSearchTerms.get(db);
		if (v != null) return v;

		final AtomicReference<String> newV = new AtomicReference<>();
		final AtomicReference<String> oldV = this.savedSearchTerms.putIfAbsent(db, newV);
		if (oldV != null) return oldV;
		return newV;
	}

}
