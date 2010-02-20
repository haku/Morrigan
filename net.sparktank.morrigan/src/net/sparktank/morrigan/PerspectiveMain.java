package net.sparktank.morrigan;

import net.sparktank.morrigan.views.ViewLibraryProperties;
import net.sparktank.morrigan.views.ViewMediaExplorer;
import net.sparktank.morrigan.views.ViewPlayer;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class PerspectiveMain implements IPerspectiveFactory {

	public static final String ID = "net.sparktank.morrigan.perspectiveMain";
	
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);
		layout.setFixed(false);
		
		layout.addView(ViewPlayer.ID, IPageLayout.LEFT, 0.3f, editorArea);
		
		IFolderLayout bottomleft = layout.createFolder("bottomleft", IPageLayout.BOTTOM, 0.5f, ViewPlayer.ID);
		bottomleft.addView(ViewMediaExplorer.ID);
		bottomleft.addPlaceholder(ViewLibraryProperties.ID);
		bottomleft.addPlaceholder("org.eclipse.ui.views.ProgressView");
		
	}

}
