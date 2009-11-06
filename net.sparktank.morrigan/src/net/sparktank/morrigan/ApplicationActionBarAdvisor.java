package net.sparktank.morrigan;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.helpers.PlaylistHelper;
import net.sparktank.morrigan.views.ViewMediaExplorer;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

/**
 * An action bar advisor is responsible for creating, adding, and disposing of
 * the actions added to a workbench window. Each window will be populated with
 * new actions.
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

	// Actions - important to allocate these only in makeActions, and then use
	// them
	// in the fill methods. This ensures that the actions aren't recreated
	// when fillActionBars is called with FILL_PROXY.
	private IWorkbenchAction exitAction;
	private MenuManager showViewMenuMgr;
	private IContributionItem showViewItemShortList;
	private IAction newPlayListAction;

	public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
		super(configurer);
	}

	protected void makeActions(final IWorkbenchWindow window) {
		// Creates the actions and registers them.
		// Registering is needed to ensure that key bindings work.
		// The corresponding commands keybindings are defined in the plugin.xml file.
		// Registering also provides automatic disposal of the actions when the window is closed.
		
		newPlayListAction = new IAction() {
			@Override
			public void setToolTipText(String text) {}
			@Override
			public void setText(String text) {}
			@Override
			public void setMenuCreator(IMenuCreator creator) {}
			@Override
			public void setImageDescriptor(ImageDescriptor newImage) {}
			@Override
			public void setId(String id) {}
			@Override
			public void setHoverImageDescriptor(ImageDescriptor newImage) {}
			@Override
			public void setHelpListener(HelpListener listener) {}
			@Override
			public void setEnabled(boolean enabled) {}
			@Override
			public void setDisabledImageDescriptor(ImageDescriptor newImage) {}
			@Override
			public void setDescription(String text) {}
			@Override
			public void setChecked(boolean checked) {}
			@Override
			public void setActionDefinitionId(String id) {}
			@Override
			public void setAccelerator(int keycode) {}
			
			@Override
			public void runWithEvent(Event event) {
				InputDialog dlg = new InputDialog(
						Display.getCurrent().getActiveShell(),
						"", "Enter playlist name.", "newPl", null);
				if (dlg.open() == Window.OK) {
					
					// create playlist.
					String plName = dlg.getValue();
					try {
						PlaylistHelper.instance.createPl(plName);
					} catch (MorriganException e) {
						throw new RuntimeException(e); // FIXME show nice error message.
					}
					
					// refresh explorer.
					IWorkbenchPage page = window.getActivePage();
					ViewMediaExplorer view = (ViewMediaExplorer) page.findView(ViewMediaExplorer.ID);
					view.refresh();
				}
			}
			
			@Override
			public void run() {}
			@Override
			public void removePropertyChangeListener(IPropertyChangeListener listener) {}
			@Override
			public boolean isHandled() {
				return false;
			}
			@Override
			public boolean isEnabled() {
				return true;
			}
			@Override
			public boolean isChecked() {
				return false;
			}
			@Override
			public String getToolTipText() {
				return null;
			}
			@Override
			public String getText() {
				return "New playlist...";
			}
			@Override
			public int getStyle() {
				return 0;
			}
			@Override
			public IMenuCreator getMenuCreator() {
				return null;
			}
			@Override
			public ImageDescriptor getImageDescriptor() {
				return null;
			}
			@Override
			public String getId() {
				return "newpl";
			}
			@Override
			public ImageDescriptor getHoverImageDescriptor() {
				return null;
			}
			@Override
			public HelpListener getHelpListener() {
				return null;
			}
			@Override
			public ImageDescriptor getDisabledImageDescriptor() {
				return null;
			}
			@Override
			public String getDescription() {
				return null;
			}
			@Override
			public String getActionDefinitionId() {
				return null;
			}
			@Override
			public int getAccelerator() {
				return 0;
			}
			@Override
			public void addPropertyChangeListener(IPropertyChangeListener listener) {}
		};
		register(newPlayListAction);
		
		exitAction = ActionFactory.QUIT.create(window);
		register(exitAction);
		
		showViewMenuMgr = new MenuManager("Show view", "showView");
		showViewItemShortList = ContributionItemFactory.VIEWS_SHORTLIST.create(window);
	}
	
	protected void fillMenuBar(IMenuManager menuBar) {
		MenuManager fileMenu = new MenuManager("&File", IWorkbenchActionConstants.M_FILE);
		menuBar.add(fileMenu);
		fileMenu.add(newPlayListAction);
		fileMenu.add(exitAction);
		
		MenuManager windowMenu = new MenuManager("&Window", IWorkbenchActionConstants.M_WINDOW);
		menuBar.add(windowMenu);
		showViewMenuMgr.add(showViewItemShortList);
		windowMenu.add(showViewMenuMgr);
	}

}
