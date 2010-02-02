package net.sparktank.morrigan;

import net.sparktank.morrigan.views.ViewMediaExplorer;
import net.sparktank.morrigan.views.ViewPlayer;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class PerspectiveMain implements IPerspectiveFactory {

	public static final String ID = "net.sparktank.morrigan.perspectiveMain";
	
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);
		layout.setFixed(false);
		
//		layout.addStandaloneView(ViewMediaExplorer.ID,  true, IPageLayout.LEFT, 0.3f, editorArea);
		layout.addView(ViewMediaExplorer.ID, IPageLayout.LEFT, 0.3f, editorArea);
		
		layout.addView(ViewPlayer.ID, IPageLayout.BOTTOM, 0.8f, editorArea);
	}

}
