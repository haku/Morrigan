package com.vaguehope.morrigan.gui;


import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IPlaceholderFolderLayout;
import org.eclipse.ui.console.IConsoleConstants;

import com.vaguehope.morrigan.gui.views.ViewControls;
import com.vaguehope.morrigan.gui.views.ViewDisplay;
import com.vaguehope.morrigan.gui.views.ViewLibraryProperties;
import com.vaguehope.morrigan.gui.views.ViewMediaExplorer;
import com.vaguehope.morrigan.gui.views.ViewPlayer;
import com.vaguehope.morrigan.gui.views.ViewQueue;
import com.vaguehope.morrigan.gui.views.ViewTagEditor;

public class PerspectiveMain implements IPerspectiveFactory {

	public static final String ID = "net.sparktank.morrigan.gui.PerspectiveMain";
	
	@Override
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
		bottom.addPlaceholder(ViewTagEditor.ID);
		bottom.addPlaceholder("org.eclipse.ui.views.ProgressView");
		bottom.addPlaceholder(IConsoleConstants.ID_CONSOLE_VIEW);
		
	}

}
