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
		
		layout.addView(ViewMediaExplorer.ID, IPageLayout.LEFT, 0.3f, editorArea);
		
		IFolderLayout bottom = layout.createFolder("bottom", IPageLayout.BOTTOM, 0.8f, editorArea);
		bottom.addView(ViewPlayer.ID);
		bottom.addPlaceholder(ViewLibraryProperties.ID);
		
	}

}
