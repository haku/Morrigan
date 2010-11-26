package net.sparktank.morrigan.danbooru;

import java.io.File;
import java.math.BigInteger;
import java.util.List;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.gui.dialogs.RunnableDialog;
import net.sparktank.morrigan.gui.views.ViewTagEditor;
import net.sparktank.morrigan.helpers.ChecksumHelper;
import net.sparktank.morrigan.model.media.interfaces.IMediaItemDb;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaItem;
import net.sparktank.morrigan.model.tags.MediaTagClassification;
import net.sparktank.morrigan.model.tags.MediaTagType;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

class FetchDanbooruTagsJob extends Job {
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
	protected IStatus run(IProgressMonitor monitor) { // TODO use progress monitor.
		try {
			long nItems = 0;
			long nTags = 0;
			
			for (IMixedMediaItem item : this.editedItems) {
				if (item.isPicture()) {
					File file = new File(item.getFilepath());
					BigInteger checksum = ChecksumHelper.generateMd5Checksum(file); // TODO update model to track MD5.
					String md5 = checksum.toString(16);
					
					String[] tags = Danbooru.getTags(md5);
					if (tags != null) {
						MediaTagClassification cls = this.editedItemDb.getTagClassification(GetDanbooruTagsAction.CATEGORY);
						if (cls == null) {
							this.editedItemDb.addTagClassification(GetDanbooruTagsAction.CATEGORY);
							cls = this.editedItemDb.getTagClassification(GetDanbooruTagsAction.CATEGORY);
							if (cls == null) throw new MorriganException("Failed to add tag category '"+GetDanbooruTagsAction.CATEGORY+"'.");
						}
						
						boolean added = false;
						for (String tag : tags) {
							if (!this.editedItemDb.hasTag(item, tag, MediaTagType.AUTOMATIC, cls)) {
								this.editedItemDb.addTag(item, tag, MediaTagType.AUTOMATIC, cls);
								added = true;
								nTags++;
							}
						}
						
						if (added) nItems++;
					}
				}
			}
			
			if (this.editedItems.size() == 1 && nTags > 0) {  // TODO improve this by checking the selected item was updated.
				this.viewTagEd.refreshContent();
			}
			
			Display.getDefault().asyncExec(new RunnableDialog("Found " + nTags + " new tags for " + nItems + " items."));
			
			return Status.OK_STATUS;
		}
		catch (Exception e) {
			Display.getDefault().asyncExec(new RunnableDialog(e));
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
}
