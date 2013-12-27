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

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemDb;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer;
import com.vaguehope.morrigan.model.media.MediaAlbum;
import com.vaguehope.morrigan.tasks.MorriganTask;
import com.vaguehope.morrigan.tasks.TaskEventListener;
import com.vaguehope.morrigan.tasks.TaskResult;
import com.vaguehope.morrigan.tasks.TaskResult.TaskOutcome;
import com.vaguehope.morrigan.util.ChecksumHelper;
import com.vaguehope.sqlitewrapper.DbException;

public abstract class LocalDbUpdateTask<Q extends IMediaItemDb<? extends IMediaItemStorageLayer<T>, T>, T extends IMediaItem> implements MorriganTask {
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
		List<T> changedItems = new ArrayList<T>();

		try {
			taskEventListener.onStart();
			taskEventListener.logMsg(this.itemList.getListName(), "Starting update...");
			taskEventListener.beginTask("Updating", 100);

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

			// TODO scan for albums?

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
					ret = new TaskResult(TaskOutcome.CANCELED);
				}
				else {
					ret = new TaskResult(TaskOutcome.SUCCESS);
				}
			}
		}
		catch (Exception e) {
			ret = new TaskResult(TaskOutcome.FAILED, "Error while updating.", e);
		}

		this.setFinished();
		taskEventListener.done();

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
		catch (MorriganException e) {
			taskEventListener.done();
			return new TaskResult(TaskOutcome.FAILED, "Failed to retrieve list of supported formats.", e);
		}

		List<String> sources = null;
		try {
			sources = this.itemList.getSources();
		}
		catch (MorriganException e) {
			taskEventListener.done();
			return new TaskResult(TaskOutcome.FAILED, "Failed to retrieve list of media sources.", e);
		}

		final List<File> filesToAdd = new ArrayList<File>();
		int filesAdded = 0;

		if (sources != null) {
			final Queue<File> dirs = new LinkedList<File>();
			for (final String source : sources) {
				dirs.add(new File(source));
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
								if (supportedFormats.contains(ext) && !this.itemList.hasFile(file)) {
									filesToAdd.add(file);
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

			Q transClone = getTransactional(this.itemList);
			try {
				transClone.addFiles(filesToAdd);
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

		long duration = (startTime - System.currentTimeMillis()) / 1000L;
		taskEventListener.logMsg(this.itemList.getListName(), "Added " + filesAdded + " files in " + duration + " seconds.");
		this.itemList.getDbLayer().getChangeEventCaller().eventMessage("Added " + filesAdded + " items to " + this.itemList.getListName() + ".");

		return null;
	}

	protected abstract Set<String> getItemFileExtensions () throws MorriganException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private TaskResult updateLibraryMetadata (final TaskEventListener taskEventListener, final int prgTotal, final List<T> changedItems) throws DbException {
		if (changedItems.size() > 0) throw new IllegalArgumentException("changedItems list must be empty.");

		final String SUBTASK_TITLE = "Reading file metadata";
		String subTaskTitle = null;
		taskEventListener.subTask(SUBTASK_TITLE);

		long startTime = System.currentTimeMillis();
		int progress = 0;
		int n = 0;
		int N = this.itemList.getAllDbEntries().size();

		ByteBuffer byteBuffer = ChecksumHelper.createByteBuffer();

		List<T> allLibraryEntries = this.itemList.getAllDbEntries();
		for (T mi : allLibraryEntries) {
			if (taskEventListener.isCanceled()) break;

			// Update status.
			if (subTaskTitle != null) {
				taskEventListener.subTask(SUBTASK_TITLE);
				subTaskTitle = null;
			}

			// Existence test.
			final File file = new File(mi.getFilepath());
			if (file.exists()) {
				// If was missing, mark as found.
				if (mi.isMissing()) {
					try {
						taskEventListener.logMsg(this.itemList.getListName(), "[FOUND] " + mi.getFilepath());
						this.itemList.setItemMissing(mi, false);
					}
					catch (Exception e) {
						taskEventListener.logError(this.itemList.getListName(), "Error while marking file as found '" + mi.getFilepath() + "': " + e.getMessage(), e);
					}
				}

				// Last modified date and hash code.
				long lastModified = file.lastModified();
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
					catch (Exception e) {
						taskEventListener.logError(this.itemList.getListName(), "Error while writing file last modified date '" + mi.getFilepath() + "': " + e.getMessage(), e);
					}

					changedItems.add(mi);
				}

				// Hash code.
				if (fileModified || mi.getHashcode() == null || mi.getHashcode().equals(BigInteger.ZERO)) {
					BigInteger hash = null;

					subTaskTitle = SUBTASK_TITLE + ": MD5 " + mi.getTitle();
					taskEventListener.subTask(subTaskTitle);
					try {
						hash = ChecksumHelper.generateMd5Checksum(file, byteBuffer); // This is slow.
					}
					catch (Exception e) {
						taskEventListener.logError(this.itemList.getListName(), "Error while generating checksum for '" + mi.getFilepath() + ": " + e.getMessage(), e);
					}

					if (hash != null && !hash.equals(BigInteger.ZERO)) {
						try {
							this.itemList.setItemHashCode(mi, hash);
						}
						catch (Exception e) {
							taskEventListener.logError(this.itemList.getListName(), "Error while setting hash code for '" + mi.getFilepath() + "' to '" + hash + "': " + e.getMessage(), e);
						}
					}

				}
			}
			else { // The file is missing.
				if (!mi.isMissing()) {
					try {
						taskEventListener.logMsg(this.itemList.getListName(), "[MISSING] " + mi.getFilepath());
						this.itemList.setItemMissing(mi, true);
					}
					catch (Exception e) {
						taskEventListener.logError(this.itemList.getListName(), "Error while marking file as missing '" + mi.getFilepath() + "': " + e.getMessage(), e);
					}
				}
			}

			n++;
			int p = N > 0 ? (n * prgTotal) / N : 0;
			if (p > progress) {
				taskEventListener.worked(p - progress);
				progress = p;
			}
		} // End metadata scanning.

		long duration = (System.currentTimeMillis() - startTime) / 1000L;
		taskEventListener.logMsg(this.itemList.getListName(), "Read file metadata in " + duration + " seconds.");
		return null;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void checkForDuplicates (final TaskEventListener taskEventListener, final int prgTotal) throws MorriganException, DbException {
		taskEventListener.subTask("Scanning for duplicates");
		List<T> tracks = this.itemList.getAllDbEntries();
		Map<T, ScanOption> dupicateItems = findDuplicates(taskEventListener, tracks, prgTotal);
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
	private Map<T, ScanOption> findDuplicates (final TaskEventListener taskEventListener, final List<T> tracks, final int prgTotal) {
		Map<T, ScanOption> dupicateItems = new HashMap<T, ScanOption>();
		int progress = 0;
		int n = 0;
		int N = tracks.size();
		long startTime = System.currentTimeMillis();

		for (int i = 0; i < tracks.size(); i++) {
			if (hasHashCode(tracks.get(i))) {
				boolean a = new File(tracks.get(i).getFilepath()).exists();
				for (int j = i + 1; j < tracks.size(); j++) {
					if (hasHashCode(tracks.get(j))) {
						if (tracks.get(i).getHashcode().equals(tracks.get(j).getHashcode())) {
							boolean b = new File(tracks.get(j).getFilepath()).exists();
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
			int p = N > 0 ? (n * prgTotal) / N : 0;
			if (p > progress) {
				taskEventListener.worked(p - progress);
				progress = p;
			}
		}
		long duration = (System.currentTimeMillis() - startTime) / 1000L;
		taskEventListener.logMsg(this.itemList.getListName(), "Duplicate scan completed in " + duration + " seconds.");
		return dupicateItems;
	}

	private boolean hasHashCode (final T item) {
		return item.getHashcode() != null && !item.getHashcode().equals(BigInteger.ZERO);
	}

	/**
	 * Duplicates will be removed from the supplied Map if they are merged.
	 */
	private void mergeDuplicates (final TaskEventListener taskEventListener, final Map<T, ScanOption> dupicateItems) throws MorriganException, DbException {
		taskEventListener.subTask("Merging duplicates");
		int count = 0;
		long startTime = System.currentTimeMillis();
		Q transClone = getTransactional(this.itemList);
		try {
			transClone.read(); // FIXME is this needed?
			count = mergeDuplocates(taskEventListener, transClone, dupicateItems);
		}
		finally {
			taskEventListener.logMsg(this.itemList.getListName(), "Committing merges...");
			try {
				transClone.commitOrRollback();
			}
			finally {
				transClone.dispose();
			}
		}
		if (count > 0) this.itemList.forceRead();
		long duration = (System.currentTimeMillis() - startTime) / 1000L;
		taskEventListener.logMsg(this.itemList.getListName(), "Merged " + count + " in " + duration + " seconds.");
	}

	/**
	 * @return Number of items merged.
	 */
	private int mergeDuplocates (final TaskEventListener taskEventListener, final Q list, final Map<T, ScanOption> dupicateItems) throws MorriganException {
		// Make a list of all the unique hashcodes we know.
		Set<BigInteger> hashcodes = new HashSet<BigInteger>();
		for (IMediaItem mi : dupicateItems.keySet()) {
			hashcodes.add(mi.getHashcode());
		}

		// Resolve each unique hashcode.
		int countMerges = 0;
		for (BigInteger h : hashcodes) {
			if (taskEventListener.isCanceled()) break;
			Map<T, ScanOption> items = findByHashcode(dupicateItems, h);

			// If there is only one entry that still exists, merge metadata and remove bad references.
			// This is the only supported merge case at the moment.
			if (countEntriesInMap(items, ScanOption.KEEP) == 1 && countEntriesInMap(items, ScanOption.DELREF) == items.size() - 1) {
				T keep = null;
				for (T i : items.keySet()) {
					if (items.get(i) == ScanOption.KEEP) keep = i;
				}
				if (keep == null) throw new NullPointerException("Out of cheese error.  Please reinstall universe and reboot.");
				items.remove(keep);

				// Now merge: start count, end count, added data, last played data.
				// Then remove missing tracks from library.
				for (T i : items.keySet()) {
					mergeItems(list, keep, i);
					list.removeItem(i);
					taskEventListener.logMsg(list.getListName(), "[REMOVED] " + i.getFilepath());
					countMerges++;
				}

				// Removed processed entries from duplicate items list.
				dupicateItems.remove(keep);
				for (IMediaItem i : items.keySet()) {
					dupicateItems.remove(i);
				}
			}
		}
		return countMerges;
	}

	/*
	 * Find all the entries with this hashcode.
	 */
	@SuppressWarnings("static-method")
	// Eclipse lies.
	private Map<T, ScanOption> findByHashcode (final Map<T, ScanOption> items, final BigInteger hashcode) {
		Map<T, ScanOption> ret = new HashMap<T, ScanOption>();
		for (Entry<T, ScanOption> i : items.entrySet()) {
			if (hashcode.equals(i.getKey().getHashcode())) {
				ret.put(i.getKey(), i.getValue());
			}
		}
		return ret;
	}

	private void printDuplicates (final TaskEventListener taskEventListener, final Map<T, ScanOption> items) {
		List<Entry<T, ScanOption>> dups = new ArrayList<Map.Entry<T, ScanOption>>(items.entrySet());
		Collections.sort(dups, new HashcodeComparator());

		taskEventListener.logMsg(this.itemList.getListName(), "Found " + dups.size() + " duplicates:");
		for (Entry<T, ScanOption> e : dups) {
			BigInteger hashcode = e.getKey().getHashcode();
			String hashcodeString = hashcode == null ? "null" : hashcode.toString(16);
			taskEventListener.logMsg(this.itemList.getListName(), hashcodeString + " : " + e.getValue() + " : " + e.getKey().getTitle());
		}
	}

	private final class HashcodeComparator implements Comparator<Entry<T, ScanOption>> {

		public HashcodeComparator () {}

		@Override
		public int compare (final Entry<T, ScanOption> o1, final Entry<T, ScanOption> o2) {
			// comp(1234, null) == -1, comp(null, null) == 0, comp(null, 1234) == 1

			BigInteger h1 = o1.getKey().getHashcode();
			BigInteger h2 = o2.getKey().getHashcode();

			return h1 == null ? (h2 == null ? 0 : 1) : h1.compareTo(h2);
		}

	}

	protected abstract void mergeItems (Q list, T itemToKeep, T itemToBeRemove) throws MorriganException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private TaskResult updateTrackMetadata1 (final TaskEventListener taskEventListener, final int prgTotal, final List<T> changedItems) throws MorriganException, DbException {
		taskEventListener.subTask("Reading metadata");
		long startTime = System.currentTimeMillis();
		int progress = 0;
		int n = 0;
		int N = this.itemList.getAllDbEntries().size();

		List<T> allLibraryEntries = this.itemList.getAllDbEntries();
		try {
			for (T mi : allLibraryEntries) {
				if (taskEventListener.isCanceled()) break;
				if (shouldTrackMetaData1(taskEventListener, this.itemList, mi) || changedItems.contains(mi)) {
					if (mi.isEnabled()) {
						taskEventListener.subTask("Reading metadata: " + mi.getTitle());

						File file = new File(mi.getFilepath());
						if (file.exists()) {
							OpResult ret = readTrackMetaData1(this.itemList, mi, file);
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
				int p = N > 0 ? (n * prgTotal) / N : 0;
				if (p > progress) {
					taskEventListener.worked(p - progress);
					progress = p;
				}
			} // End file metadata scanning.
		}
		catch (Exception e) {
			throw new MorriganException("Faital error while reading metadata.", e);
		}
		finally {
			try {
				cleanUpAfterTrackMetaData1();
			}
			catch (Exception e) {
				taskEventListener.logError(this.itemList.getListName(), "Failed to clean up after track-metadata-1.", e);
			}
		}

		long duration = (System.currentTimeMillis() - startTime) / 1000L;
		taskEventListener.logMsg(this.itemList.getListName(), "Read metadata in " + duration + " seconds.");
		return null;
	}

	protected abstract boolean shouldTrackMetaData1 (TaskEventListener taskEventListener, Q library, T item) throws MorriganException;

	protected abstract OpResult readTrackMetaData1 (Q library, T item, File file);

	protected abstract void cleanUpAfterTrackMetaData1 ();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private TaskResult updateTrackMetadata2 (final TaskEventListener taskEventListener, final int prgTotal, final List<T> changedItems) throws DbException {
		taskEventListener.subTask("Reading more metadata");
		long startTime = System.currentTimeMillis();
		int progress = 0;
		int n = 0;
		int N = this.itemList.getAllDbEntries().size();

		for (T mlt : changedItems) {
			if (taskEventListener.isCanceled()) break;
			taskEventListener.subTask("Reading more metadata: " + mlt.getTitle());

			try {
				File file = new File(mlt.getFilepath());
				if (file.exists()) {
					readTrackMetaData2(this.itemList, mlt, file);
				}
			}
			catch (Exception e) {
				taskEventListener.logError(this.itemList.getListName(), "Error while reading more metadata for '" + mlt.getFilepath() + "'.", e);
			}

			n++;
			int p = N > 0 ? (n * prgTotal) / N : 0;
			if (p > progress) {
				taskEventListener.worked(p - progress);
				progress = p;
			}
		} // End track metadata scanning.

		long duration = (System.currentTimeMillis() - startTime) / 1000L;
		taskEventListener.logMsg(this.itemList.getListName(), "Read more metadata in " + duration + " seconds.");
		return null;
	}

	protected abstract void readTrackMetaData2 (Q library, T item, File file) throws Exception;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private TaskResult updateAlbums (final TaskEventListener taskEventListener, final int prgTotal) {
		List<String> sources = null;
		try {
			sources = this.itemList.getSources();
		}
		catch (MorriganException e) {
			taskEventListener.done();
			return new TaskResult(TaskOutcome.FAILED, "Failed to retrieve list of media sources.", e);
		}
		List<File> albumDirs = new ArrayList<File>();
		if (sources != null) {
			Queue<File> dirs = new LinkedList<File>();
			for (String source : sources) {
				dirs.add(new File(source));
			}
			while (!dirs.isEmpty()) {
				if (taskEventListener.isCanceled()) break;
				File dirItem = dirs.poll();
				taskEventListener.subTask("Album searching " + dirItem.getAbsolutePath());

				File[] arrFiles = dirItem.listFiles();
				if (arrFiles != null) {
					for (File file : arrFiles) {
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
				int N = albumDirs.size();
				for (File dir : albumDirs) {
					if (taskEventListener.isCanceled()) break;
					taskEventListener.subTask("Album " + dir.getAbsolutePath());
					MediaAlbum album = this.itemList.createAlbum(dir.getName());
					for (File file : dir.listFiles()) {
						if (this.itemList.hasFile(file)) {
							T item = this.itemList.getByFile(file);
							this.itemList.addToAlbum(album, item);
						}
					}
					n++;
					int p = N > 0 ? (n * prgTotal) / N : 0;
					if (p > progress) {
						taskEventListener.worked(p - progress);
						progress = p;
					}
				}
			}
			taskEventListener.subTask("Checking for removed albums");
			for (MediaAlbum album : this.itemList.getAlbums()) {
				if (taskEventListener.isCanceled()) break;
				for (T item : this.itemList.getAlbumItems(album)) {
					if (!isDirectoryAnAlbum(new File(item.getFilepath()).getParentFile())) {
						this.itemList.removeFromAlbum(album, item);
					}
				}
				if (this.itemList.getAlbumItems(album).size() < 1) {
					this.itemList.removeAlbum(album);
				}
				// TODO track prg here.
			}
		}
		catch (MorriganException e) {
			return new TaskResult(TaskOutcome.FAILED, "Failed to update albums.", e);
		}
		catch (DbException e) {
			return new TaskResult(TaskOutcome.FAILED, "Failed to update albums.", e);
		}
		return null;
	}

	private static boolean isDirectoryAnAlbum (final File dir) {
		File marker = new File(dir, ".album");
		return marker.exists() && marker.isFile();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Helper methods.

	static protected int countEntriesInMap (final Map<?, ?> map, final Object value) {
		int n = 0;
		for (Entry<?, ?> e : map.entrySet()) {
			if (e.getValue().equals(value)) n++;
		}
		return n;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
