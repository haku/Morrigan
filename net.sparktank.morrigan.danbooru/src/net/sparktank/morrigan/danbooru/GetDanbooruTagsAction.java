package net.sparktank.morrigan.danbooru;

import java.io.File;
import java.math.BigInteger;
import java.net.URL;

import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.views.ViewTagEditor;
import net.sparktank.morrigan.helpers.ChecksumHelper;
import net.sparktank.morrigan.model.media.interfaces.IMediaItem;
import net.sparktank.morrigan.server.HttpClient;
import net.sparktank.morrigan.server.HttpClient.HttpResponse;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;

public class GetDanbooruTagsAction extends Action {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.danbooru.GetDanbooruTagsAction";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final ViewTagEditor viewTagEd;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public GetDanbooruTagsAction (ViewTagEditor viewTagEd) {
		super("Get tags from Danbooru");
		this.setId(ID);
		//this.setImageDescriptor(Activator.getImageDescriptor("icons/display.gif")); // TODO choose icon.
		this.viewTagEd = viewTagEd;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void run() {
		IMediaItem editedItem = this.viewTagEd.getEditedItem();
		
		if (editedItem != null) {
			FetchTagsJob job = new FetchTagsJob(editedItem);
			job.schedule();
		}
		
//		IMediaItemDb<?, ?, ?> editedItemDb = this.viewTagEd.getEditedItemDb();
//		editedItemDb.addTag(this.editedItem, text, MediaTagType.MANUAL, (MediaTagClassification)null);
	}
	
	static class FetchTagsJob extends Job {

		private final IMediaItem editedItem;

		public FetchTagsJob (IMediaItem editedItem) {
			super("Fetching tags from Danbooru");
			this.editedItem = editedItem;
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
				
				// TODO parse out tags.
				// TODO add tags to image.
				
				new MorriganMsgDlg("TODO: parse:\n" + response).open();
				
				return Status.OK_STATUS;
			}
			catch (Exception e) {
				new MorriganMsgDlg(e).open();
				return new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
			}
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}