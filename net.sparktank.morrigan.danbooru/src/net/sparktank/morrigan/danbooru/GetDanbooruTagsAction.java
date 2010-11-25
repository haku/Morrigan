package net.sparktank.morrigan.danbooru;

import java.io.File;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.gui.dialogs.RunnableDialog;
import net.sparktank.morrigan.gui.views.ViewTagEditor;
import net.sparktank.morrigan.helpers.ChecksumHelper;
import net.sparktank.morrigan.model.media.interfaces.IMediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMediaItemDb;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaItem;
import net.sparktank.morrigan.model.tags.MediaTagClassification;
import net.sparktank.morrigan.model.tags.MediaTagType;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;

public class GetDanbooruTagsAction extends Action {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.danbooru.GetDanbooruTagsAction";
	
	public static final String CATEGORY = "Danbooru"; // Tag category.
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final ViewTagEditor viewTagEd;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public GetDanbooruTagsAction (ViewTagEditor viewTagEd) {
		super("Get tags from Danbooru");
		this.setId(ID);
		this.setImageDescriptor(Activator.getImageDescriptor("icons/danbooru.png"));
		this.viewTagEd = viewTagEd;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void run() {
		IMediaItemDb<?,?,?> editedItemDb = this.viewTagEd.getEditedItemDb();
		List<IMediaItem> editedItems = this.viewTagEd.getEditedItems();
		
		if (editedItemDb != null && editedItems != null && editedItems.size() > 0) {
			List<IMixedMediaItem> items = new LinkedList<IMixedMediaItem>();
			
			for (IMediaItem item : editedItems) {
				if (item instanceof IMixedMediaItem) {
					IMixedMediaItem mmItem = (IMixedMediaItem) item;
					items.add(mmItem);
				}
			}
			
			if (items.size() > 0) {
				FetchTagsJob job = new FetchTagsJob(editedItemDb, items, this.viewTagEd);
				job.schedule();
			}
			else {
				Display.getDefault().asyncExec(new RunnableDialog("No pictures in selection."));
			}
		}
	}
	
	static class FetchTagsJob extends Job {

		private final IMediaItemDb<?, ?, ?> editedItemDb;
		private final List<IMixedMediaItem> editedItems;
		private final ViewTagEditor viewTagEd;

		public FetchTagsJob (IMediaItemDb<?, ?, ?> editedItemDb, List<IMixedMediaItem> editedItems, ViewTagEditor viewTagEd) {
			super("Fetching tags from Danbooru");
			this.editedItemDb = editedItemDb;
			this.editedItems = editedItems;
			this.viewTagEd = viewTagEd;
		}
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
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
							MediaTagClassification cls = this.editedItemDb.getTagClassification(CATEGORY);
							if (cls == null) {
								this.editedItemDb.addTagClassification(CATEGORY);
								cls = this.editedItemDb.getTagClassification(CATEGORY);
								if (cls == null) throw new MorriganException("Failed to add tag category '"+CATEGORY+"'.");
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
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}