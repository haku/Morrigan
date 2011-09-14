package com.vaguehope.morrigan.gui;


import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.actions.RetargetAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

import com.vaguehope.morrigan.gui.actions.MinToTrayAction;
import com.vaguehope.morrigan.gui.views.ShowViewAction;
import com.vaguehope.morrigan.gui.views.ViewMediaExplorer;

/**
 * An action bar advisor is responsible for creating, adding, and disposing of
 * the actions added to a workbench window. Each window will be populated with
 * new actions.
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	IDs for RetargetAction.
	
	public static final String ACTIONID_ADD = "morrigan.add";
	public static final String ACTIONID_REMOVE = "morrigan.remove";
	public static final String ACTIONID_SHOWPROPERTIES = "morrigan.showproperties";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Action instances.
/*
 * 	Actions - important to allocate these only in makeActions, and then use them
 *  in the fill methods. This ensures that the actions aren't recreated
 *  when fillActionBars is called with FILL_PROXY.
 */
	
	// Global actions.
	private IWorkbenchAction exitAction;
	
	// Window management actions.
	private IWorkbenchAction newWindowAction;
	private IWorkbenchAction minToTrayAction;
	private IWorkbenchAction resetPerspectiveAction;
	private IContributionItem showViewItemShortList;
	private IAction showMediaExplorer;
	private IWorkbenchAction showPrefAction;
	
	// List actions.
	private IWorkbenchAction saveAction;
	private IWorkbenchAction saveAllAction;
	private RetargetAction addAction;
	private RetargetAction removeAction;
	private RetargetAction showPropertiesAction;
	
	// Help.
	private IWorkbenchAction showAbout;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private MenuManager showViewMenuMgr = null;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructor.
	
	public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
		super(configurer);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Action creation.
	
	@Override
	protected void makeActions(final IWorkbenchWindow window) {
		/* Creates the actions and registers them.
		 * Registering is needed to ensure that key bindings work.
		 * The corresponding commands keybindings are defined in the plugin.xml file.
		 * Registering also provides automatic disposal of the actions when the window is closed.
		 */
		
		this.exitAction = ActionFactory.QUIT.create(window);
		register(this.exitAction);
		
		this.newWindowAction = ActionFactory.OPEN_NEW_WINDOW.create(window);
		register(this.newWindowAction);
		
		this.minToTrayAction = new MinToTrayAction(window);
		register(this.minToTrayAction);
		
		this.resetPerspectiveAction = ActionFactory.RESET_PERSPECTIVE.create(window);
		register(this.resetPerspectiveAction);
		
		this.showViewMenuMgr = new MenuManager("Show view", "showView");
		this.showViewItemShortList = ContributionItemFactory.VIEWS_SHORTLIST.create(window);
		
		this.showMediaExplorer = new ShowViewAction(ViewMediaExplorer.ID, "Media Explorer", Activator.getImageDescriptor("icons/library.gif"));
		register(this.showMediaExplorer);
		
		this.showPrefAction = ActionFactory.PREFERENCES.create(window);
		register(this.showPrefAction);
		
		// Editor actions.
		
		this.saveAction = ActionFactory.SAVE.create(window);
		register(this.saveAction);
		
		this.saveAllAction = ActionFactory.SAVE_ALL.create(window);
		register(this.saveAllAction);
		
		this.addAction = new RetargetAction(ACTIONID_ADD, "&add files...");
		this.addAction.setImageDescriptor(Activator.getImageDescriptor("icons/plus.gif"));
		getActionBarConfigurer().registerGlobalAction(this.addAction);
		register(this.addAction);
		window.getPartService().addPartListener(this.addAction);
		
		this.removeAction = new RetargetAction(ACTIONID_REMOVE, "&remove selected...");
		this.removeAction.setImageDescriptor(Activator.getImageDescriptor("icons/minus.gif"));
		getActionBarConfigurer().registerGlobalAction(this.removeAction);
		register(this.removeAction);
		window.getPartService().addPartListener(this.removeAction);
		
		this.showPropertiesAction = new RetargetAction(ACTIONID_SHOWPROPERTIES, "&properties...");
		this.showPropertiesAction.setImageDescriptor(Activator.getImageDescriptor("icons/pref.gif"));
		getActionBarConfigurer().registerGlobalAction(this.showPropertiesAction);
		register(this.showPropertiesAction);
		window.getPartService().addPartListener(this.showPropertiesAction);
		
		// Help actions.
		this.showAbout = ActionFactory.ABOUT.create(window);
		register(this.showAbout);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Action assignment.
	
	@Override
	protected void fillMenuBar(IMenuManager menuBar) {
		MenuManager fileMenu = new MenuManager("&Morrigan", IWorkbenchActionConstants.M_FILE);
		menuBar.add(fileMenu);
		fileMenu.add(this.exitAction);
		
		MenuManager collectionsMenu = new MenuManager("&Collections", "collections");
		menuBar.add(collectionsMenu);
		collectionsMenu.add(this.saveAction);
		collectionsMenu.add(this.saveAllAction);
		collectionsMenu.add(this.addAction);
		collectionsMenu.add(this.removeAction);
		collectionsMenu.add(new Separator());
		collectionsMenu.add(this.showPropertiesAction);
		
		MenuManager windowMenu = new MenuManager("&Window", IWorkbenchActionConstants.M_WINDOW);
		menuBar.add(windowMenu);
		windowMenu.add(this.newWindowAction);
		windowMenu.add(this.minToTrayAction);
		windowMenu.add(new Separator());
		this.showViewMenuMgr.add(this.showMediaExplorer);
		this.showViewMenuMgr.add(new Separator());
		this.showViewMenuMgr.add(this.showViewItemShortList);
		windowMenu.add(this.showViewMenuMgr);
		windowMenu.add(this.resetPerspectiveAction);
		windowMenu.add(new Separator());
		windowMenu.add(this.showPrefAction);
		
		MenuManager helpMenu = new MenuManager("&Help", IWorkbenchActionConstants.M_HELP);
		menuBar.add(helpMenu);
		helpMenu.add(this.showAbout);
	}
	
	@Override
	protected void fillCoolBar(ICoolBarManager coolBar) {
		coolBar.add(new GroupMarker("group.list"));
		IToolBarManager fileToolBar = new ToolBarManager(coolBar.getStyle());
		fileToolBar.add(this.saveAction);
		fileToolBar.add(this.addAction);
		fileToolBar.add(this.removeAction);
		fileToolBar.add(this.showPropertiesAction);
		coolBar.add(new ToolBarContributionItem(fileToolBar));
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
