/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.launchConfigurations;


import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.ui.AbstractDebugView;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.IDebugView;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.model.WorkbenchViewerSorter;

/**
 * A tree view of launch configurations
 */
public class LaunchConfigurationView extends AbstractDebugView implements ILaunchConfigurationListener {
	
	private Viewer fViewer;
	
	/**
	 * The launch group to display
	 */
	private LaunchGroupExtension fLaunchGroup;
	
	/**
	 * Actions
	 */
	private CreateLaunchConfigurationAction fCreateAction;
	private DeleteLaunchConfigurationAction fDeleteAction;
	private DuplicateLaunchConfigurationAction fDuplicateAction;
	
	/**
	 * Constructs a launch configuration view for the given launch group
	 */
	public LaunchConfigurationView(LaunchGroupExtension launchGroup) {
		super();
		fLaunchGroup = launchGroup;
	}
	
	/**
	 * Returns the launch group this view is displaying.
	 * 
	 * @return the launch group this view is displaying
	 */
	protected LaunchGroupExtension getLaunchGroup() {
		return fLaunchGroup;
	}

	/**
	 * @see org.eclipse.debug.ui.AbstractDebugView#createViewer(org.eclipse.swt.widgets.Composite)
	 */
	protected Viewer createViewer(Composite parent) {
		TreeViewer treeViewer = new TreeViewer(parent);
		treeViewer.setLabelProvider(DebugUITools.newDebugModelPresentation());
		treeViewer.setSorter(new WorkbenchViewerSorter());
		treeViewer.setContentProvider(new LaunchConfigurationTreeContentProvider(fLaunchGroup.getMode(), parent.getShell()));
		treeViewer.addFilter(new LaunchGroupFilter(getLaunchGroup()));
		treeViewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
		treeViewer.expandAll();
		treeViewer.getControl().addHelpListener(new HelpListener() {
			public void helpRequested(HelpEvent evt) {
				handleHelpRequest(evt);
			}
		});
		getLaunchManager().addLaunchConfigurationListener(this);
		return treeViewer;
	}
	
	/**
	 * Handle help events locally rather than deferring to WorkbenchHelp.  This
	 * allows help specific to the selected config type to be presented.
	 * 
	 * @since 2.1
	 */
	protected void handleHelpRequest(HelpEvent evt) {
		if (getTreeViewer().getTree() != evt.getSource()) {
			return;
		}
		try {
			ISelection selection = getViewer().getSelection();
			if (!selection.isEmpty() && selection instanceof IStructuredSelection ) {
				IStructuredSelection structuredSelection = (IStructuredSelection) selection;
				Object firstSelected = structuredSelection.getFirstElement();
				ILaunchConfigurationType configType = null;
				if (firstSelected instanceof ILaunchConfigurationType) {
					configType = (ILaunchConfigurationType) firstSelected;
				} else if (firstSelected instanceof ILaunchConfiguration) {
					configType = ((ILaunchConfiguration) firstSelected).getType();
				}
				if (configType != null) {
					String helpContextId = LaunchConfigurationPresentationManager.getDefault().getHelpContext(configType, getLaunchGroup().getMode());
					if (helpContextId != null) {
						WorkbenchHelp.displayHelp(helpContextId);
					}
				}
			}
		} catch (CoreException ce) {
			DebugUIPlugin.log(ce);
		}
	}

	/**
	 * @see org.eclipse.debug.ui.AbstractDebugView#createActions()
	 */
	protected void createActions() {
		
		fCreateAction = new CreateLaunchConfigurationAction(getViewer(), getLaunchGroup().getMode());
		setAction(CreateLaunchConfigurationAction.ID_CREATE_ACTION, fCreateAction);
		
		fDeleteAction = new DeleteLaunchConfigurationAction(getViewer(), getLaunchGroup().getMode());
		setAction(DeleteLaunchConfigurationAction.ID_DELETE_ACTION, fDeleteAction);
		setAction(IDebugView.REMOVE_ACTION, fDeleteAction);
		
		fDuplicateAction = new DuplicateLaunchConfigurationAction(getViewer(), getLaunchGroup().getMode());
		setAction(DuplicateLaunchConfigurationAction.ID_DUPLICATE_ACTION, fDuplicateAction);
		
	}

	/**
	 * @see org.eclipse.debug.ui.AbstractDebugView#getHelpContextId()
	 */
	protected String getHelpContextId() {
		return IDebugHelpContextIds.LAUNCH_CONFIGURATION_VIEW;
	}

	/**
	 * @see org.eclipse.debug.ui.AbstractDebugView#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	protected void fillContextMenu(IMenuManager menu) {
		menu.add(fCreateAction);
		menu.add(fDuplicateAction);
		menu.add(fDeleteAction);
		menu.add(new Separator());
	}

	/**
	 * @see org.eclipse.debug.ui.AbstractDebugView#configureToolBar(org.eclipse.jface.action.IToolBarManager)
	 */
	protected void configureToolBar(IToolBarManager tbm) {
	}
	
	/**
	 * Returns this view's tree viewer
	 * 
	 * @return this view's tree viewer 
	 */
	protected TreeViewer getTreeViewer() {
		return (TreeViewer)getViewer();
	}

	/**
	 * @see org.eclipse.ui.IWorkbenchPart#dispose()
	 */
	public void dispose() {
		fCreateAction.dispose();
		fDeleteAction.dispose();
		fDuplicateAction.dispose();
		getLaunchManager().removeLaunchConfigurationListener(this);
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationAdded(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void launchConfigurationAdded(ILaunchConfiguration configuration) {
		try {
			if (configuration.getAttribute(IDebugUIConstants.ATTR_PRIVATE, false)) {
				return;
			}
		} catch (CoreException e) {
			DebugUIPlugin.log(e);
			return;
		}
		TreeViewer viewer = getTreeViewer();
		viewer.getControl().setRedraw(false);
		try {
			viewer.add(configuration.getType(), configuration);
			// if moved, remove original now
			ILaunchConfiguration from = getLaunchManager().getMovedFrom(configuration);
			if (from != null) {
				viewer.remove(from);
			}
		} catch (CoreException e) {
		}
		viewer.getControl().setRedraw(true);
		getTreeViewer().setSelection(new StructuredSelection(configuration), true);
		
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationChanged(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void launchConfigurationChanged(ILaunchConfiguration configuration) {
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchConfigurationListener#launchConfigurationRemoved(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void launchConfigurationRemoved(ILaunchConfiguration configuration) {
		// if moved, ignore
		ILaunchConfiguration to = getLaunchManager().getMovedTo(configuration);
		if (to != null) {
			return;
		}
		
		ILaunchConfigurationType type = null;
		int typeIndex= -1; // The index of the deleted configuration's type
		int configIndex= -1; // The index of the deleted configuration		
		// Initialize data used to set the selection after deletion
		TreeItem[] items= getTreeViewer().getTree().getItems();
		TreeItem typeItem;
		for (int i= 0, numTypes= items.length; (i < numTypes && type == null); i++) {
			typeItem= items[i];
			typeIndex= i;
			TreeItem[] configs= typeItem.getItems();
			for (int j= 0, numConfigs= configs.length; j < numConfigs; j++) {
				if (configuration.equals(configs[j].getData())) {
					configIndex= j;
					type = (ILaunchConfigurationType)typeItem.getData();
					break;
				}
			}
		}			
			
		getTreeViewer().remove(configuration);
		if (getViewer().getSelection().isEmpty()) {
			IStructuredSelection newSelection= null;
			if (typeIndex != -1 && configIndex != -1) {
				// Reset selection to the next config
				TreeItem[] configItems= getTreeViewer().getTree().getItems()[typeIndex].getItems();
				int numItems= configItems.length;
				if (numItems > configIndex) { // Select the item at the same index as the deleted
					newSelection= new StructuredSelection(configItems[configIndex].getData());
				} else if (numItems > 0) { // Deleted the last item(s). Select the last item
					newSelection= new StructuredSelection(configItems[numItems - 1].getData());
				}
			}
			if (newSelection == null && type != null) {
				// Reset selection to the config type of the first selected configuration
				newSelection = new StructuredSelection(type);
			}
			getTreeViewer().setSelection(newSelection);
		}
	}

	/**
	 * This is similar to IWorkbenchPart#createPartControl(Composite), but it is
	 * called by the launch dialog when creating the launch config tree view.
	 * Since this view is not contained in the workbench, we cannot do all the
	 * usual initialzation (toolbars, etc).
	 */
	public void createLaunchDialogControl(Composite parent) {
		fViewer = createViewer(parent);
		createActions();
		createContextMenu(getViewer().getControl());
		WorkbenchHelp.setHelp(parent, getHelpContextId());
		getViewer().getControl().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				handleKeyPressed(e);
			}
		});
		if (getViewer() instanceof StructuredViewer) {
			((StructuredViewer)getViewer()).addDoubleClickListener(this);
		}
	}
	
	

	/**
	 * @see org.eclipse.debug.ui.IDebugView#getViewer()
	 */
	public Viewer getViewer() {
		return fViewer;
	}
	
	protected ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}

}
