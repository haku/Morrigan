package net.sparktank.morrigan;

import net.sparktank.morrigan.views.ViewControls;
import net.sparktank.morrigan.views.ViewDisplay;
import net.sparktank.morrigan.views.ViewLibraryProperties;
import net.sparktank.morrigan.views.ViewMediaExplorer;
import net.sparktank.morrigan.views.ViewPlayer;
import net.sparktank.morrigan.views.ViewQueue;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IPlaceholderFolderLayout;
import org.eclipse.ui.console.IConsoleConstants;

public class PerspectiveMain implements IPerspectiveFactory {

	public static final String ID = "net.sparktank.morrigan.perspectiveMain";
	
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(true);
		layout.setFixed(false);
		
		layout.addStandaloneView(ViewControls.ID, false, IPageLayout.TOP, 0.2f, editorArea);
		
		IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT, 0.3f, editorArea);
		left.addView(ViewMediaExplorer.ID);
		
		IPlaceholderFolderLayout topleft = layout.createPlaceholderFolder("topleft", IPageLayout.TOP, 0.5f, "left");
		topleft.addPlaceholder(ViewDisplay.ID);
		topleft.addPlaceholder(ViewPlayer.ID);
		
		IPlaceholderFolderLayout bottom = layout.createPlaceholderFolder("bottom", IPageLayout.BOTTOM, 0.7f, editorArea);
		bottom.addPlaceholder(ViewQueue.ID);
		bottom.addPlaceholder(ViewLibraryProperties.ID);
		bottom.addPlaceholder("org.eclipse.ui.views.ProgressView");
		bottom.addPlaceholder(IConsoleConstants.ID_CONSOLE_VIEW);
		
	}

}
