package net.sparktank.morrigan.gui;

import net.sparktank.morrigan.gui.actions.NewLibraryAction;
import net.sparktank.morrigan.gui.actions.NewPlaylistAction;
import net.sparktank.morrigan.gui.display.MinToTrayAction;
import net.sparktank.morrigan.gui.views.ShowViewAction;
import net.sparktank.morrigan.gui.views.ViewMediaExplorer;

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
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.actions.RetargetAction;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

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
	private IAction newLibraryAction;
	private IAction newPlayListAction;
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
		
		newLibraryAction = new NewLibraryAction(window);
		register(newLibraryAction);
		
		newPlayListAction = new NewPlaylistAction(window);
		register(newPlayListAction);
		
		exitAction = ActionFactory.QUIT.create(window);
		register(exitAction);
		
		newWindowAction = ActionFactory.OPEN_NEW_WINDOW.create(window);
		register(newWindowAction);
		
		minToTrayAction = new MinToTrayAction(window);
		register(minToTrayAction);
		
		resetPerspectiveAction = ActionFactory.RESET_PERSPECTIVE.create(window);
		register(resetPerspectiveAction);
		
		showViewMenuMgr = new MenuManager("Show view", "showView");
		showViewItemShortList = ContributionItemFactory.VIEWS_SHORTLIST.create(window);
		
		showMediaExplorer = new ShowViewAction(ViewMediaExplorer.ID, "Media Explorer", Activator.getImageDescriptor("icons/library.gif"));
		register(showMediaExplorer);
		
		showPrefAction = ActionFactory.PREFERENCES.create(window);
		register(showPrefAction);
		
		// Editor actions.
		
		saveAction = ActionFactory.SAVE.create(window);
		register(saveAction);
		
		saveAllAction = ActionFactory.SAVE_ALL.create(window);
		register(saveAllAction);
		
		addAction = new RetargetAction(ACTIONID_ADD, "&add files...");
		addAction.setImageDescriptor(Activator.getImageDescriptor("icons/plus.gif"));
		getActionBarConfigurer().registerGlobalAction(addAction);
		register(addAction);
		window.getPartService().addPartListener(addAction);
		
		removeAction = new RetargetAction(ACTIONID_REMOVE, "&remove selected...");
		removeAction.setImageDescriptor(Activator.getImageDescriptor("icons/minus.gif"));
		getActionBarConfigurer().registerGlobalAction(removeAction);
		register(removeAction);
		window.getPartService().addPartListener(removeAction);
		
		showPropertiesAction = new RetargetAction(ACTIONID_SHOWPROPERTIES, "&properties...");
		showPropertiesAction.setImageDescriptor(Activator.getImageDescriptor("icons/pref.gif"));
		getActionBarConfigurer().registerGlobalAction(showPropertiesAction);
		register(showPropertiesAction);
		window.getPartService().addPartListener(showPropertiesAction);
		
		// Help actions.
		showAbout = ActionFactory.ABOUT.create(window);
		register(showAbout);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Action assignment.
	
	@Override
	protected void fillMenuBar(IMenuManager menuBar) {
		MenuManager fileMenu = new MenuManager("&Morrigan", IWorkbenchActionConstants.M_FILE);
		menuBar.add(fileMenu);
		fileMenu.add(exitAction);
		
		MenuManager collectionsMenu = new MenuManager("&Collections", "collections");
		menuBar.add(collectionsMenu);
		collectionsMenu.add(newLibraryAction);
		collectionsMenu.add(newPlayListAction);
		collectionsMenu.add(new Separator());
		collectionsMenu.add(saveAction);
		collectionsMenu.add(saveAllAction);
		collectionsMenu.add(addAction);
		collectionsMenu.add(removeAction);
		collectionsMenu.add(new Separator());
		collectionsMenu.add(showPropertiesAction);
		
		MenuManager windowMenu = new MenuManager("&Window", IWorkbenchActionConstants.M_WINDOW);
		menuBar.add(windowMenu);
		windowMenu.add(newWindowAction);
		windowMenu.add(minToTrayAction);
		windowMenu.add(new Separator());
		showViewMenuMgr.add(showMediaExplorer);
		showViewMenuMgr.add(new Separator());
		showViewMenuMgr.add(showViewItemShortList);
		windowMenu.add(showViewMenuMgr);
		windowMenu.add(resetPerspectiveAction);
		windowMenu.add(new Separator());
		windowMenu.add(showPrefAction);
		
		MenuManager helpMenu = new MenuManager("&Help", IWorkbenchActionConstants.M_HELP);
		menuBar.add(helpMenu);
		helpMenu.add(showAbout);
	}
	
	@Override
	protected void fillCoolBar(ICoolBarManager coolBar) {
		coolBar.add(new GroupMarker("group.list"));
		IToolBarManager fileToolBar = new ToolBarManager(coolBar.getStyle());
		fileToolBar.add(saveAction);
		fileToolBar.add(addAction);
		fileToolBar.add(removeAction);
		fileToolBar.add(showPropertiesAction);
		coolBar.add(new ToolBarContributionItem(fileToolBar));
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
