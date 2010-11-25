package net.sparktank.morrigan.danbooru;

import java.io.File;
import java.math.BigInteger;
import java.net.URL;

import net.sparktank.morrigan.gui.dialogs.RunnableDialog;
import net.sparktank.morrigan.gui.views.ViewTagEditor;
import net.sparktank.morrigan.helpers.ChecksumHelper;
import net.sparktank.morrigan.model.media.interfaces.IMediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMediaItemDb;
import net.sparktank.morrigan.model.tags.MediaTagClassification;
import net.sparktank.morrigan.model.tags.MediaTagType;
import net.sparktank.morrigan.server.HttpClient;
import net.sparktank.morrigan.server.HttpClient.HttpResponse;

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
		IMediaItem editedItem = this.viewTagEd.getEditedItem();
		
		if (editedItemDb != null && editedItem != null) {
			FetchTagsJob job = new FetchTagsJob(editedItemDb, editedItem, this.viewTagEd);
			job.schedule();
		}
	}
	
	static class FetchTagsJob extends Job {

		private final IMediaItemDb<?, ?, ?> editedItemDb;
		private final IMediaItem editedItem;
		private final ViewTagEditor viewTagEd;

		public FetchTagsJob (IMediaItemDb<?, ?, ?> editedItemDb, IMediaItem editedItem, ViewTagEditor viewTagEd) {
			super("Fetching tags from Danbooru");
			this.editedItemDb = editedItemDb;
			this.editedItem = editedItem;
			this.viewTagEd = viewTagEd;
		}
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			File file = new File(this.editedItem.getFilepath());
			try {
				BigInteger checksum = ChecksumHelper.generateMd5Checksum(file); // TODO update model to track MD5.
				String md5 = checksum.toString(16);
				
				String surl = "http://danbooru.donmai.us/post/index.xml?tags=md5:" + md5;
				URL url = new URL(surl);
				HttpResponse response = HttpClient.getHttpClient().doHttpRequest(url);
				
				String tagstring = substringByTokens(response.getBody(), "tags=\"", "\"");
				String[] tags = tagstring.split(" ");
				
				MediaTagClassification cls = this.editedItemDb.getTagClassification(CATEGORY);
				if (cls == null) {
					this.editedItemDb.addTagClassification(CATEGORY);
					cls = this.editedItemDb.getTagClassification(CATEGORY);
					if (cls == null) throw new IllegalArgumentException("Failed to add tag category '"+CATEGORY+"'.");
				}
				
				int n = 0;
				for (String tag : tags) {
					if (!this.editedItemDb.hasTag(this.editedItem, tag, MediaTagType.AUTOMATIC, cls)) {
						this.editedItemDb.addTag(this.editedItem, tag, MediaTagType.AUTOMATIC, cls);
						n++;
					}
				}
				
				if (n > 0) {
					this.viewTagEd.refreshContent();
				}
				
				Display.getDefault().asyncExec(new RunnableDialog("Found " + tags.length + " tags, added " + n + " new ones."));
				
				return Status.OK_STATUS;
			}
			catch (Exception e) {
				Display.getDefault().asyncExec(new RunnableDialog(e));
				return new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
			}
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public String substringByTokens (String d, String k0, String k1) {
		String ret;
		int x0;
		int l;
		
		try {
			if (k0 == null) {
				x0 = 0;
				l = 0;
			}
			else {
				x0 = d.indexOf(k0);
				if (x0 < 0) throw new IllegalArgumentException("k0 '"+k0+"' not found in '"+d+"'.");
				l = k0.length();
			}

			if (k1 != null) {
				int x1 = d.indexOf(k1, x0+l+1);
				if (x1 < 0) throw new IllegalArgumentException("k1 '"+k1+"' not found in '"+d+"'.");
				ret = d.substring(x0+l, x1);
			}
			else {
				ret = d.substring(x0+l);
			}
		}
		catch (Exception e) {
			throw new IllegalArgumentException("data='"+d+"' k0='"+k0+"' k1='"+k1+"'.", e);
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}