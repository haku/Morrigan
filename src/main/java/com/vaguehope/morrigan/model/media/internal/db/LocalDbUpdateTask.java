package com.vaguehope.morrigan.model.media.internal.db;

import java.io.File;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.MediaItem;
import com.vaguehope.morrigan.model.media.MediaItem.MediaType;
import com.vaguehope.morrigan.model.media.MediaDb;
import com.vaguehope.morrigan.model.media.MediaAlbum;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.tasks.MorriganTask;
import com.vaguehope.morrigan.tasks.TaskEventListener;
import com.vaguehope.morrigan.tasks.TaskOutcome;
import com.vaguehope.morrigan.tasks.TaskResult;
import com.vaguehope.morrigan.util.ChecksumHelper;
import com.vaguehope.morrigan.util.ChecksumHelper.Md5AndSha1;
import com.vaguehope.morrigan.util.FileSystem;
import com.vaguehope.morrigan.util.MimeType;

public abstract class LocalDbUpdateTask<Q extends MediaDb> implements MorriganTask {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected enum ScanOption {
		KEEP, DELREF, MOVEFILE
	}

	protected class OpResult {

		private final Exception e;
		private final String msg;
		private final boolean faital;

		public OpResult (final String msg, final Exception e) {
			this.msg = msg;
			this.e = e;
			this.faital = false;
		}

		public OpResult (final String msg, final Exception e, final boolean faital) {
			this.msg = msg;
			this.e = e;
			this.faital = faital;
		}

		public String getMsg () {
			return this.msg;
		}

		public Exception getException () {
			return this.e;
		}

		public boolean isFaital () {
			return this.faital;
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private FileSystem fileSystem = new FileSystem();

	public void setFileSystem(final FileSystem fileSystem) {
		this.fileSystem = fileSystem;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final Q itemList;

	private volatile boolean isFinished = false;

	public LocalDbUpdateTask (final Q itemList) {
		this.itemList = itemList;
	}

	@Override
	public String getTitle () {
		return "Update " + this.itemList.getListName();
	}

	public boolean isFinished () {
		return this.isFinished;
	}

	public void setFinished () {
		this.isFinished = true;
	}

	protected abstract Q getTransactional (Q list) throws DbException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Main run method.

	/**
	 * TODO use monitor.worked(1);
	 */
	@Override
	public TaskResult run (final TaskEventListener taskEventListener) {
		TaskResult ret = null;
		final List<MediaItem> changedItems = new ArrayList<>();

		try {
			taskEventListener.onStart();
			taskEventListener.logMsg(this.itemList.getListName(), "Starting update...");
			taskEventListener.beginTask(getTitle(), 100);

			// Ensure list is initialised.
			this.itemList.read();

			// Scan directories for new files.
			ret = scanLibraryDirectories(taskEventListener);

			if (ret == null) {
				// Check known files exist and update metadata.
				ret = updateLibraryMetadata(taskEventListener, 40, changedItems);
			}

			if (ret == null) {
				// Check for duplicate items and merge matching items.
				checkForDuplicates(taskEventListener, 15);
			}

			if (ret == null) {
				// Read track duration.
				ret = updateTrackMetadata1(taskEventListener, 20, changedItems);
			}

			if (ret == null) {
				// Read track tags duration.
				ret = updateTrackMetadata2(taskEventListener, 20, changedItems);
			}

			if (ret == null) {
				// Scan for albums.
				ret = updateAlbums(taskEventListener, 5);
			}

//			if (ret == null) {
//				 TODO : vacuum DB?
//			}

			if (ret == null) {
				/*
				 * FIXME This refresh is here until
				 * AbstractMixedMediaDb.setItemMediaType() is properly finished.
				 */
				this.itemList.forceRead(); // Item types in MMDB might have changed.

				if (taskEventListener.isCanceled()) {
					taskEventListener.logMsg(this.itemList.getListName(), "Update was canceled desu~.");
					taskEventListener.done(TaskOutcome.CANCELLED);
					ret = new TaskResult(TaskOutcome.CANCELLED);
				}
				else {
					ret = new TaskResult(TaskOutcome.SUCCESS);
				}
			}
		}
		catch (final Exception e) {
			taskEventListener.done(TaskOutcome.FAILED);
			ret = new TaskResult(TaskOutcome.FAILED, "Error while updating.", e);
		}
		this.setFinished();
		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Generic scanning.

	private TaskResult scanLibraryDirectories (final TaskEventListener taskEventListener) throws DbException, MorriganException {
		taskEventListener.subTask("Scanning sources");
		final long startTime = System.currentTimeMillis();

		Set<String> supportedFormats;
		try {
			supportedFormats = getItemFileExtensions();
		}
		catch (final MorriganException e) {
			taskEventListener.done(TaskOutcome.FAILED);
			return new TaskResult(TaskOutcome.FAILED, "Failed to retrieve list of supported formats.", e);
		}

		List<String> sources = null;
		try {
			sources = this.itemList.getSources();
		}
		catch (final MorriganException e) {
			taskEventListener.done(TaskOutcome.FAILED);
			return new TaskResult(TaskOutcome.FAILED, "Failed to retrieve list of media sources.", e);
		}

		final List<File> filesToAdd = new ArrayList<>();
		int filesAdded = 0;

		if (sources != null) {
			final Queue<File> dirs = new LinkedList<>();
			for (final String source : sources) {
				dirs.add(this.fileSystem.makeFile(source));
			}

			while (!dirs.isEmpty()) {
				if (taskEventListener.isCanceled()) break;
				final File dirItem = dirs.poll();
				taskEventListener.subTask("(" + filesToAdd.size() + ") Scanning " + dirItem.getAbsolutePath());

				final File[] arrFiles = dirItem.listFiles();
				if (arrFiles != null) {
					for (final File file : arrFiles) {
						if (taskEventListener.isCanceled()) break;

						if (file.canRead()) {
							if (file.isDirectory()) {
								dirs.add(file);
							}
							else if (file.isFile()) {
								String ext = file.getName();
								ext = ext.substring(ext.lastIndexOf('.') + 1).toLowerCase();
								if (supportedFormats.contains(ext)) {
									switch (this.itemList.hasFile(file)) {
										case UNKNOWN:
										case MISSING: // Work around for path case changes.
											filesToAdd.add(file);
											break;
										default:
											break;
									}
								}
							}
						}
						else {
							taskEventListener.logMsg(this.itemList.getListName(), "Can not read: " + file.getAbsolutePath());
						}
					}
				}
				else {
					taskEventListener.logMsg(this.itemList.getListName(), "Failed to read directory: " + dirItem.getAbsolutePath());
				}
			}
		} // End directory scanning.

		if (filesToAdd.size() > 0) {
			taskEventListener.logMsg(this.itemList.getListName(), "Adding " + filesToAdd.size() + " files...");

			Collections.sort(filesToAdd); // Ensure sequential files are in expected order.

			final Q transClone = getTransactional(this.itemList);
			try {
				transClone.addFiles(MediaType.UNKNOWN, filesToAdd);
				filesAdded = filesAdded + filesToAdd.size();
			}
			finally {
				taskEventListener.logMsg(this.itemList.getListName(), "Committing " + filesAdded + " inserts...");
				try {
					transClone.commitOrRollback();
				}
				finally {
					transClone.dispose();
				}
			}
		}
		if (filesAdded > 0) this.itemList.forceRead();

		final long duration = TimeUnit.MILLISECONDS.toSeconds(startTime - System.currentTimeMillis());
		taskEventListener.logMsg(this.itemList.getListName(), "Added " + filesAdded + " files in " + duration + " seconds.");
		this.itemList.getDbLayer().getChangeEventCaller().eventMessage("Added " + filesAdded + " items to " + this.itemList.getListName() + ".");

		return null;
	}

	protected abstract Set<String> getItemFileExtensions () throws MorriganException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private TaskResult updateLibraryMetadata (final TaskEventListener taskEventListener, final int prgTotal, final List<MediaItem> changedItems) throws DbException {
		if (changedItems.size() > 0) throw new IllegalArgumentException("changedItems list must be empty.");

		final String SUBTASK_TITLE = "Reading file metadata";
		taskEventListener.subTask(SUBTASK_TITLE);

		final long startTime = System.currentTimeMillis();
		int progress = 0;
		int n = 0;
		final int N = this.itemList.getAllDbEntries().size();

		final ByteBuffer byteBuffer = ChecksumHelper.createByteBuffer();

		final List<MediaItem> allLibraryEntries = this.itemList.getAllDbEntries();
		for (final MediaItem mi : allLibraryEntries) {
			if (taskEventListener.isCanceled()) break;
			taskEventListener.subTask(SUBTASK_TITLE + ": " + mi.getTitle());

			// Existence test.
			final File file = this.fileSystem.makeFile(mi.getFilepath());
			if (file.exists()) {
				// If was missing, mark as found.
				if (mi.isMissing()) {
					try {
						taskEventListener.logMsg(this.itemList.getListName(), "[FOUND] " + mi.getFilepath());
						this.itemList.setItemMissing(mi, false);
					}
					catch (final Exception e) {
						taskEventListener.logError(this.itemList.getListName(), "Error while marking file as found '" + mi.getFilepath() + "': " + e.getMessage(), e);
					}
				}

				// Last modified date and hash code.
				final long lastModified = file.lastModified();
				boolean fileModified = false;
				if (mi.getDateLastModified() == null || mi.getDateLastModified().getTime() != lastModified) {
					fileModified = true;

					if (mi.getDateLastModified() == null) {
						taskEventListener.logMsg(this.itemList.getListName(), "[NEW] " + mi.getTitle());
					}
					else {
						taskEventListener.logMsg(this.itemList.getListName(), "[CHANGED] " + mi.getTitle());
					}

					try {
						this.itemList.setItemDateLastModified(mi, new Date(lastModified));
					}
					catch (final Exception e) {
						taskEventListener.logError(this.itemList.getListName(), "Error while writing file last modified date '" + mi.getFilepath() + "': " + e.getMessage(), e);
					}

					changedItems.add(mi);
				}

				// Hash code.
				if (fileModified
						|| mi.getMd5() == null || mi.getMd5().equals(BigInteger.ZERO)
						|| mi.getSha1() == null || mi.getSha1().equals(BigInteger.ZERO)) {
					Md5AndSha1 md5AndSha1 = null;

					try {
						md5AndSha1 = this.fileSystem.generateMd5AndSha1(file, byteBuffer); // This is slow.
					}
					catch (final Exception e) {
						taskEventListener.logError(this.itemList.getListName(), "Error while generating MD5/SHA1 for '" + mi.getFilepath() + ": " + e.getMessage(), e);
					}

					if (md5AndSha1 != null) {
						try {
							if (!md5AndSha1.getMd5().equals(mi.getMd5())) this.itemList.setItemMd5(mi, md5AndSha1.getMd5());
							if (!md5AndSha1.getSha1().equals(mi.getSha1())) this.itemList.setItemSha1(mi, md5AndSha1.getSha1());
						}
						catch (final Exception e) {
							taskEventListener.logError(this.itemList.getListName(), "Error while setting MD5/SHA1 code for '" + mi.getFilepath() + "' to '" + md5AndSha1 + "': " + e.getMessage(), e);
						}
					}

				}
			}
			else { // The file is missing.
				if (!mi.isMissing()) {
					try {
						taskEventListener.logMsg(this.itemList.getListName(), "[MISSING] " + mi.getFilepath());
						this.itemList.setItemMissing(mi, true);

						// Missing items need to be removed from albums so that the album can be deleted if it is also removed.
						this.itemList.removeFromAllAlbums(mi);
					}
					catch (final Exception e) {
						taskEventListener.logError(this.itemList.getListName(), "Error while marking file as missing '" + mi.getFilepath() + "': " + e.getMessage(), e);
					}
				}
			}

			n++;
			final int p = N > 0 ? (n * prgTotal) / N : 0;
			if (p > progress) {
				taskEventListener.worked(p - progress);
				progress = p;
			}
		} // End metadata scanning.

		final long duration = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);
		taskEventListener.logMsg(this.itemList.getListName(), "Read file metadata in " + duration + " seconds.");
		return null;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void checkForDuplicates (final TaskEventListener taskEventListener, final int prgTotal) throws MorriganException, DbException {
		taskEventListener.subTask("Scanning for duplicates");
		final List<MediaItem> tracks = this.itemList.getAllDbEntries();
		final Map<MediaItem, ScanOption> dupicateItems = findDuplicates(taskEventListener, tracks, prgTotal);
		if (dupicateItems.size() > 0) {
			mergeDuplicates(taskEventListener, dupicateItems);
			printDuplicates(taskEventListener, dupicateItems);
			this.itemList.getDbLayer().getChangeEventCaller().eventMessage(this.itemList.getListName() + " contains " + dupicateItems.size() + " duplicates.");
		}
		else {
			taskEventListener.logMsg(this.itemList.getListName(), "No duplicates found.");
		}
	}

	// Eclipse lies.
	private Map<MediaItem, ScanOption> findDuplicates (final TaskEventListener taskEventListener, final List<MediaItem> tracks, final int prgTotal) {
		final Map<MediaItem, ScanOption> dupicateItems = new HashMap<>();
		int progress = 0;
		int n = 0;
		final int N = tracks.size();
		final long startTime = System.currentTimeMillis();

		for (int i = 0; i < tracks.size(); i++) {
			if (hasMd5(tracks.get(i))) {
				final boolean a = this.fileSystem.makeFile(tracks.get(i).getFilepath()).exists();
				for (int j = i + 1; j < tracks.size(); j++) {
					if (hasMd5(tracks.get(j))) {
						if (tracks.get(i).getMd5().equals(tracks.get(j).getMd5())) {
							final boolean b = this.fileSystem.makeFile(tracks.get(j).getFilepath()).exists();
							if (a && b) { // Both exist.
								// TODO prompt to move the newer one?
								if (!dupicateItems.containsKey(tracks.get(i))) dupicateItems.put(tracks.get(i), ScanOption.KEEP);
								if (!dupicateItems.containsKey(tracks.get(j))) dupicateItems.put(tracks.get(j), ScanOption.MOVEFILE);
							}
							else if (a != b) { // Only one exists.
								if (!dupicateItems.containsKey(tracks.get(i))) dupicateItems.put(tracks.get(i), a ? ScanOption.KEEP : ScanOption.DELREF);
								if (!dupicateItems.containsKey(tracks.get(j))) dupicateItems.put(tracks.get(j), b ? ScanOption.KEEP : ScanOption.DELREF);
							}
							// If both missing do not worry about it.
						}
					}
					if (taskEventListener.isCanceled()) break;
				}
			}

			n++;
			final int p = N > 0 ? (n * prgTotal) / N : 0;
			if (p > progress) {
				taskEventListener.worked(p - progress);
				progress = p;
			}
		}
		final long duration = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);
		taskEventListener.logMsg(this.itemList.getListName(), "Duplicate scan completed in " + duration + " seconds.");
		return dupicateItems;
	}

	private static boolean hasMd5 (final MediaItem item) {
		return item.getMd5() != null && !item.getMd5().equals(BigInteger.ZERO);
	}

	/**
	 * Duplicates will be removed from the supplied Map if they are merged.
	 */
	private void mergeDuplicates (final TaskEventListener taskEventListener, final Map<MediaItem, ScanOption> dupicateItems) throws MorriganException, DbException {
		taskEventListener.subTask("Merging " + dupicateItems.size() + " duplicates");
		int count = 0;
		final long startTime = System.currentTimeMillis();
		final Q transClone = getTransactional(this.itemList);
		try {
			transClone.read(); // FIXME is this needed?
			count = mergeDuplicates(taskEventListener, transClone, dupicateItems);
		}
		finally {
			taskEventListener.logMsg(this.itemList.getListName(), "Committing " + count + " merges... (" + dupicateItems.size() + " duplicates remain)");
			try {
				transClone.commitOrRollback();
			}
			finally {
				transClone.dispose();
			}
		}
		if (count > 0) this.itemList.forceRead();
		final long duration = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);
		taskEventListener.logMsg(this.itemList.getListName(), "Merged " + count + " in " + duration + " seconds.");
	}

	/**
	 * @return Number of items merged.
	 */
	private int mergeDuplicates (final TaskEventListener taskEventListener, final Q list, final Map<MediaItem, ScanOption> dupicateItems) throws MorriganException {
		// Make a list of all the unique hashcodes we know.
		final Set<BigInteger> md5s = new HashSet<>();
		for (final MediaItem mi : dupicateItems.keySet()) {
			md5s.add(mi.getMd5());
		}
		taskEventListener.logMsg(this.itemList.getListName(), "Found " + md5s.size() + " unique MD5s.");

		// Resolve each unique hashcode.
		int countMerges = 0;
		for (final BigInteger h : md5s) {
			if (taskEventListener.isCanceled()) break;
			final Map<MediaItem, ScanOption> items = findByMd5(dupicateItems, h);

			// If there is only one entry that still exists, merge metadata and remove bad references.
			// This is the only supported merge case at the moment.
			if (countEntriesInMap(items, ScanOption.KEEP) == 1 && countEntriesInMap(items, ScanOption.DELREF) == items.size() - 1) {
				MediaItem keep = null;
				for (final MediaItem i : items.keySet()) {
					if (items.get(i) == ScanOption.KEEP) keep = i;
				}
				if (keep == null) throw new NullPointerException("Out of cheese error.  Please reinstall universe and reboot.");
				items.remove(keep);

				// Now merge: start count, end count, added data, last played data.
				// Then remove missing tracks from library.
				for (final MediaItem i : items.keySet()) {
					mergeItems(list, keep, i);
					list.removeItem(i);
					taskEventListener.logMsg(list.getListName(), "[REMOVED] " + i.getFilepath());
					countMerges++;
				}

				// Removed processed entries from duplicate items list.
				dupicateItems.remove(keep);
				for (final MediaItem i : items.keySet()) {
					dupicateItems.remove(i);
				}
			}
		}
		return countMerges;
	}

	/*
	 * Find all the entries with this hashcode.
	 */
	private static Map<MediaItem, ScanOption> findByMd5 (final Map<MediaItem, ScanOption> items, final BigInteger md5) {
		final Map<MediaItem, ScanOption> ret = new HashMap<>();
		for (final Entry<MediaItem, ScanOption> i : items.entrySet()) {
			if (md5.equals(i.getKey().getMd5())) {
				ret.put(i.getKey(), i.getValue());
			}
		}
		return ret;
	}

	private void printDuplicates (final TaskEventListener taskEventListener, final Map<MediaItem, ScanOption> items) {
		final List<Entry<MediaItem, ScanOption>> dups = new ArrayList<>(items.entrySet());
		Collections.sort(dups, new Md5Comparator());

		taskEventListener.logMsg(this.itemList.getListName(), "Found " + dups.size() + " duplicates:");
		for (final Entry<MediaItem, ScanOption> e : dups) {
			final BigInteger md5 = e.getKey().getMd5();
			final String md5String = md5 == null ? "null" : md5.toString(16);
			taskEventListener.logMsg(this.itemList.getListName(), md5String + " : " + e.getValue() + " : " + e.getKey().getTitle());
		}
	}

	private final class Md5Comparator implements Comparator<Entry<MediaItem, ScanOption>> {

		public Md5Comparator () {}

		@Override
		public int compare (final Entry<MediaItem, ScanOption> o1, final Entry<MediaItem, ScanOption> o2) {
			// comp(1234, null) == -1, comp(null, null) == 0, comp(null, 1234) == 1

			final BigInteger h1 = o1.getKey().getMd5();
			final BigInteger h2 = o2.getKey().getMd5();

			return h1 == null ? (h2 == null ? 0 : 1) : h1.compareTo(h2);
		}

	}

	protected abstract void mergeItems (Q list, MediaItem itemToKeep, MediaItem itemToBeRemove) throws MorriganException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private TaskResult updateTrackMetadata1 (final TaskEventListener taskEventListener, final int prgTotal, final List<MediaItem> changedItems) throws MorriganException, DbException {
		taskEventListener.subTask("Reading metadata");
		final long startTime = System.currentTimeMillis();
		int progress = 0;
		int n = 0;
		final int N = this.itemList.getAllDbEntries().size();

		final List<MediaItem> allLibraryEntries = this.itemList.getAllDbEntries();
		try {
			for (final MediaItem mi : allLibraryEntries) {
				if (taskEventListener.isCanceled()) break;

				if (!mi.hasMimeType()) {
					final MimeType mt = MimeType.identify(mi.getFilepath());
					if (mt != null) this.itemList.setItemMimeType(mi, mt.getMimeType());
				}

				if (shouldTrackMetaData1(taskEventListener, this.itemList, mi) || changedItems.contains(mi)) {
					if (mi.isEnabled()) {
						taskEventListener.subTask("Reading metadata: " + mi.getTitle());

						final File file = this.fileSystem.makeFile(mi.getFilepath());
						if (file.exists()) {
							final OpResult ret = readTrackMetaData1(this.itemList, mi, file);
							if (ret != null) {
								if (ret.isFaital()) {
									throw ret.getException();
								}

								taskEventListener.logError(this.itemList.getListName(), "Error while reading metadata for '" + mi.getFilepath() + "'.", ret.getException());

								// Tag track as unreadable.
								//library.markAsUnreadabled(mi); // FIXME what if the user wants to try again?
							}
						} // End exists test.
					}
					else { // If marked as disabled.
						taskEventListener.logMsg(this.itemList.getListName(), "Ignoring disabled file '" + mi.getFilepath() + "'.");
					}
				}// End duration > 0 test.

				n++;
				final int p = N > 0 ? (n * prgTotal) / N : 0;
				if (p > progress) {
					taskEventListener.worked(p - progress);
					progress = p;
				}
			} // End file metadata scanning.
		}
		catch (final Exception e) {
			throw new MorriganException("Faital error while reading metadata.", e);
		}
		finally {
			try {
				cleanUpAfterTrackMetaData1();
			}
			catch (final Exception e) {
				taskEventListener.logError(this.itemList.getListName(), "Failed to clean up after track-metadata-1.", e);
			}
		}

		final long duration = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);
		taskEventListener.logMsg(this.itemList.getListName(), "Read metadata in " + duration + " seconds.");
		return null;
	}

	protected abstract boolean shouldTrackMetaData1 (TaskEventListener taskEventListener, Q library, MediaItem item) throws MorriganException;

	protected abstract OpResult readTrackMetaData1 (Q library, MediaItem item, File file);

	protected abstract void cleanUpAfterTrackMetaData1 ();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private TaskResult updateTrackMetadata2 (final TaskEventListener taskEventListener, final int prgTotal, final List<MediaItem> changedItems) throws DbException {
		taskEventListener.subTask("Reading more metadata");
		final long startTime = System.currentTimeMillis();
		int progress = 0;
		int n = 0;
		final int N = this.itemList.getAllDbEntries().size();

		for (final MediaItem mlt : changedItems) {
			if (taskEventListener.isCanceled()) break;
			taskEventListener.subTask("Reading more metadata: " + mlt.getTitle());

			try {
				final File file = this.fileSystem.makeFile(mlt.getFilepath());
				if (file.exists()) {
					readTrackMetaData2(this.itemList, mlt, file);
				}
			}
			catch (final Exception e) {
				taskEventListener.logError(this.itemList.getListName(), "Error while reading more metadata for '" + mlt.getFilepath() + "'.", e);
			}

			n++;
			final int p = N > 0 ? (n * prgTotal) / N : 0;
			if (p > progress) {
				taskEventListener.worked(p - progress);
				progress = p;
			}
		} // End track metadata scanning.

		final long duration = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);
		taskEventListener.logMsg(this.itemList.getListName(), "Read more metadata in " + duration + " seconds.");
		return null;
	}

	protected abstract void readTrackMetaData2 (Q library, MediaItem item, File file) throws Exception;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private TaskResult updateAlbums (final TaskEventListener taskEventListener, final int prgTotal) {
		List<String> sources = null;
		try {
			sources = this.itemList.getSources();
		}
		catch (final MorriganException e) {
			taskEventListener.done(TaskOutcome.FAILED);
			return new TaskResult(TaskOutcome.FAILED, "Failed to retrieve list of media sources.", e);
		}
		final List<File> albumDirs = new ArrayList<>();
		if (sources != null) {
			final Queue<File> dirs = new LinkedList<>();
			for (final String source : sources) {
				dirs.add(this.fileSystem.makeFile(source));
			}
			while (!dirs.isEmpty()) {
				if (taskEventListener.isCanceled()) break;
				final File dirItem = dirs.poll();
				taskEventListener.subTask("Album searching " + dirItem.getAbsolutePath());

				final File[] arrFiles = dirItem.listFiles();
				if (arrFiles != null) {
					for (final File file : arrFiles) {
						if (taskEventListener.isCanceled()) break;
						if (file.isDirectory()) {
							if (isDirectoryAnAlbum(file)) {
								albumDirs.add(file);
							}
							else {
								dirs.add(file);
							}
						}
					}
				}
				else {
					taskEventListener.logMsg(this.itemList.getListName(), "Failed to read directory: " + dirItem.getAbsolutePath());
				}
			}
		}
		try {
			taskEventListener.subTask("Found " + albumDirs.size() + " albums");
			if (!albumDirs.isEmpty()) {
				int progress = 0;
				int n = 0;
				final int N = albumDirs.size();
				for (final File dir : albumDirs) {
					if (taskEventListener.isCanceled()) break;
					taskEventListener.subTask("Album: " + dir.getAbsolutePath());
					final MediaAlbum album = this.itemList.createAlbum(dir.getName());
					for (final File file : dir.listFiles()) {
						if (this.itemList.hasFile(file).isKnown()) {
							final MediaItem item = this.itemList.getByFile(file);
							this.itemList.addToAlbum(album, item);
						}
					}
					n++;
					final int p = N > 0 ? (n * prgTotal) / N : 0;
					if (p > progress) {
						taskEventListener.worked(p - progress);
						progress = p;
					}
				}
			}

			taskEventListener.subTask("Checking for removed albums");
			int albumsRemoved = 0;
			for (final MediaAlbum album : this.itemList.getAlbums()) {
				if (taskEventListener.isCanceled()) break;
				for (final MediaItem item : this.itemList.getAlbumItems(MediaType.UNKNOWN, album)) {
					if (!isDirectoryAnAlbum(this.fileSystem.makeFile(item.getFilepath()).getParentFile())) {
						this.itemList.removeFromAlbum(album, item);
					}
				}
				if (this.itemList.getAlbumItems(MediaType.UNKNOWN, album).size() < 1) {
					this.itemList.removeAlbum(album);
					albumsRemoved += 1;
				}
				// TODO track prg here.
			}
			taskEventListener.logMsg(this.itemList.getListName(), ("Removed " + albumsRemoved + " albums"));
		}
		catch (final MorriganException e) {
			return new TaskResult(TaskOutcome.FAILED, "Failed to update albums.", e);
		}
		return null;
	}

	private boolean isDirectoryAnAlbum (final File dir) {
		final File marker = this.fileSystem.makeFile(dir, ".album");
		return marker.exists() && marker.isFile();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Helper methods.

	static protected int countEntriesInMap (final Map<?, ?> map, final Object value) {
		int n = 0;
		for (final Entry<?, ?> e : map.entrySet()) {
			if (e.getValue().equals(value)) n++;
		}
		return n;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
