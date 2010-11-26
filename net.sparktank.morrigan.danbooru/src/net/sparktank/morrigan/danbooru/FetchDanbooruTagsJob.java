package net.sparktank.morrigan.danbooru;

import java.io.File;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.gui.dialogs.RunnableDialog;
import net.sparktank.morrigan.gui.views.ViewTagEditor;
import net.sparktank.morrigan.helpers.ChecksumHelper;
import net.sparktank.morrigan.model.media.interfaces.IMediaItemDb;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaItem;
import net.sparktank.morrigan.model.tags.MediaTag;
import net.sparktank.morrigan.model.tags.MediaTagClassification;
import net.sparktank.morrigan.model.tags.MediaTagType;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

class FetchDanbooruTagsJob extends Job {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String TAG_CATEGORY = "Danbooru";
	public static final String DATE_TAG_CATEGORY = "DanbooruDate";
	public static final String DATE_TAG_FORMAT = "yyyy-MM-dd";
	public static final long MIN_TIME_BETWEEN_SCANS_MILISECONDS = 10 * 24 * 60 * 60 * 1000; // 10 days.
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final IMediaItemDb<?, ?, ?> editedItemDb;
	private final List<IMixedMediaItem> editedItems;
	private final ViewTagEditor viewTagEd;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public FetchDanbooruTagsJob (IMediaItemDb<?, ?, ?> editedItemDb, List<IMixedMediaItem> editedItems, ViewTagEditor viewTagEd) {
		super("Fetching tags from Danbooru");
		this.editedItemDb = editedItemDb;
		this.editedItems = editedItems;
		this.viewTagEd = viewTagEd;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected IStatus run(IProgressMonitor monitor) { // TODO use progress monitor.  TODO use transactions.
		try {
			long nScanned = 0;
			long nUpdated = 0;
			long nTags = 0;
			long nAlreadyFresh = 0;
			
			MediaTagClassification dateTagCls = this.editedItemDb.getTagClassification(DATE_TAG_CATEGORY);
			if (dateTagCls == null) {
				this.editedItemDb.addTagClassification(DATE_TAG_CATEGORY);
				dateTagCls = this.editedItemDb.getTagClassification(DATE_TAG_CATEGORY);
				if (dateTagCls == null) throw new MorriganException("Failed to add tag category '"+DATE_TAG_CATEGORY+"'.");
			}
			
			MediaTagClassification tagCls = this.editedItemDb.getTagClassification(TAG_CATEGORY);
			if (tagCls == null) {
				this.editedItemDb.addTagClassification(TAG_CATEGORY);
				tagCls = this.editedItemDb.getTagClassification(TAG_CATEGORY);
				if (tagCls == null) throw new MorriganException("Failed to add tag category '"+TAG_CATEGORY+"'.");
			}
			
			SimpleDateFormat tagDateFormat = new SimpleDateFormat(DATE_TAG_FORMAT);
			Date now = new Date();
			String nowString = tagDateFormat.format(now);
			
			// Calculate work to do.
			List<IMixedMediaItem> itemsToWork = new LinkedList<IMixedMediaItem>();
			for (IMixedMediaItem item : this.editedItems) {
				if (item.isPicture()) {
					MediaTag markerTag = getMarkerTag(this.editedItemDb, item, dateTagCls);
					Date markerDate = null;
					if (markerTag != null) markerDate = tagDateFormat.parse(markerTag.getTag());
					if (markerDate == null || now.getTime() - markerDate.getTime() > MIN_TIME_BETWEEN_SCANS_MILISECONDS) {
						itemsToWork.add(item);
					}
    				else {
    					nAlreadyFresh++;
    				}
				}
			}
			
			System.err.println("itemsToWork.size()=" + itemsToWork.size());
			
			// Batch work that needs doing.
			List<List<IMixedMediaItem>> batchedWork = new LinkedList<List<IMixedMediaItem>>();
			int n = 0;
			List<IMixedMediaItem> newBatch = new LinkedList<IMixedMediaItem>();
			for (IMixedMediaItem item : itemsToWork) {
				newBatch.add(item);
				n++;
				
				if (n >= 10) { // Batch size = 10.
					n = 0;
					batchedWork.add(newBatch);
					newBatch = new LinkedList<IMixedMediaItem>();
				}
			}
			if (newBatch.size() > 0) batchedWork.add(newBatch);
			
			System.err.println("batchedWork.size()=" + batchedWork.size());
			
			// Do work that needs doing.
			monitor.beginTask("Fetching", itemsToWork.size());
			for (List<IMixedMediaItem> batch : batchedWork) {
				Map<IMixedMediaItem, String> md5s = new HashMap<IMixedMediaItem, String>();
				
				// TODO update model to track MD5s so this block is not longer needed.
				for (IMixedMediaItem item : batch) {
					File file = new File(item.getFilepath());
					BigInteger checksum = ChecksumHelper.generateMd5Checksum(file);
					String md5 = checksum.toString(16);
					md5s.put(item, md5);
				}
				
				System.err.println("Looking up " + md5s.values().size() + " MD5s...");
				
				Map<String, String[]> tagSets = Danbooru.getTags(md5s.values());
				
				for (IMixedMediaItem item : batch) {
					String[] tags = tagSets.get(md5s.get(item));
					if (tags != null) {
						boolean added = false;
						for (String tag : tags) {
							if (!this.editedItemDb.hasTag(item, tag, MediaTagType.AUTOMATIC, tagCls)) {
								this.editedItemDb.addTag(item, tag, MediaTagType.AUTOMATIC, tagCls);
								added = true;
								nTags++;
							}
						}
						if (added) nUpdated++;
					}
					
					MediaTag markerTag = getMarkerTag(this.editedItemDb, item, dateTagCls);
					updateMarkerTag(this.editedItemDb, item, dateTagCls, markerTag, nowString);
					nScanned++;
					
					monitor.worked(1);
					if (monitor.isCanceled()) break;
				}
			}
			
			if (this.editedItems.size() == 1 && nTags > 0) {  // TODO improve this by checking the selected item was updated.
				this.viewTagEd.refreshContent();
			}
			
			String msg = "Scanned "+nScanned+" items, "+nAlreadyFresh+" already up to date."
					+ "\nFound " + nTags + " new tags for " + nUpdated + " items.";
			if (monitor.isCanceled()) msg = msg + "\n\nTask canceled desu~.";
			Display.getDefault().asyncExec(new RunnableDialog(msg));
			
			return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
		}
		catch (Exception e) {
			Display.getDefault().asyncExec(new RunnableDialog(e));
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public MediaTag getMarkerTag (IMediaItemDb<?, ?, ?> itemDb, IMixedMediaItem item, MediaTagClassification cls) throws MorriganException {
		List<MediaTag> tags = itemDb.getTags(item);
		
		MediaTag markerTag = null;
		for (MediaTag tag : tags) {
			if (tag.getClassification().equals(cls)) {
				if (markerTag == null) {
					markerTag = tag;
				}
				else {
					throw new MorriganException("Item '"+item.getFilepath()+"' has more than one marker tag '"+cls.getClassification()+"'.");
				}
			}
		}
		
		return markerTag;
	}
	
	static public void updateMarkerTag (IMediaItemDb<?, ?, ?> itemDb, IMixedMediaItem item, MediaTagClassification cls, MediaTag markerTag, String newString) throws MorriganException {
		if (markerTag != null) itemDb.removeTag(markerTag);
		itemDb.addTag(item, newString, MediaTagType.AUTOMATIC, cls);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
}
