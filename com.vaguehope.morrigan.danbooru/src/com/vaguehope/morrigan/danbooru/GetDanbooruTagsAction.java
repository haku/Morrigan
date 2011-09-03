package com.vaguehope.morrigan.danbooru;

import java.util.LinkedList;
import java.util.List;


import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;

import com.vaguehope.morrigan.gui.dialogs.RunnableDialog;
import com.vaguehope.morrigan.gui.views.ViewTagEditor;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemDb;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;

public class GetDanbooruTagsAction extends Action {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "com.vaguehope.morrigan.danbooru.GetDanbooruTagsAction";
	
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
		IMediaItemDb<?,?> editedItemDb = this.viewTagEd.getEditedItemDb();
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
				FetchDanbooruTagsJob job = new FetchDanbooruTagsJob(editedItemDb, items, this.viewTagEd);
				job.schedule();
			}
			else {
				Display.getDefault().asyncExec(new RunnableDialog("No pictures in selection."));
			}
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}