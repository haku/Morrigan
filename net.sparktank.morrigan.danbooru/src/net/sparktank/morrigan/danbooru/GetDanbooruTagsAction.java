package net.sparktank.morrigan.danbooru;

import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.views.ViewTagEditor;
import net.sparktank.morrigan.model.media.interfaces.IMediaItem;

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
		
		new MorriganMsgDlg("TODO: get tags from Danbooru for img '" + editedItem.getTitle() + "'.").open();
		
		// TODO get tags.
		// TODO add tags to image.
		
//		IMediaItemDb<?, ?, ?> editedItemDb = this.viewTagEd.getEditedItemDb();
//		editedItemDb.addTag(this.editedItem, text, MediaTagType.MANUAL, (MediaTagClassification)null);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}