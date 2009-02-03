/*******************************************************************************
 * Copyright (c) 2009 Wind River Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.launch;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.internal.ui.viewers.breadcrumb.AbstractBreadcrumb;
import org.eclipse.debug.internal.ui.viewers.breadcrumb.BreadcrumbViewer;
import org.eclipse.debug.internal.ui.viewers.breadcrumb.IBreadcrumbDropDownSite;
import org.eclipse.debug.internal.ui.viewers.breadcrumb.TreeViewerDropDown;
import org.eclipse.debug.internal.ui.viewers.model.ILabelUpdateListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ILabelUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.debug.ui.contexts.AbstractDebugContextProvider;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.debug.ui.contexts.IDebugContextProvider;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreePathContentProvider;
import org.eclipse.jface.viewers.ITreePathLabelProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerLabel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.progress.UIJob;

/**
 * @since 3.5
 */
public class LaunchViewBreadcrumb extends AbstractBreadcrumb implements IDebugContextListener, ILabelUpdateListener  {

    private static class Input {
        final TreePath fPath;

        Input(TreePath path) {
            fPath = path;
        }
    }
    
    private static class ContentProvider implements ITreePathContentProvider {

        private static final Object[] EMPTY_ELEMENTS_ARRAY = new Object[0];
        
        public Input fInput;  
        
        public Object[] getChildren(TreePath parentPath) {
            if (hasChildren(parentPath)) {
                return new Object[] { fInput.fPath.getSegment(parentPath.getSegmentCount()) };
            }
            return EMPTY_ELEMENTS_ARRAY;
        }

        public TreePath[] getParents(Object element) {
            // Not supported
            return new TreePath[] { TreePath.EMPTY };
        }

        public boolean hasChildren(TreePath parentPath) {
            if ( parentPath.getSegmentCount() == 0) {
                return fInput != null;
            } else if (fInput != null && 
                       fInput.fPath != null && 
                       fInput.fPath.getSegmentCount() > parentPath.getSegmentCount()) 
            {
                for (int i = 0; i < parentPath.getSegmentCount(); i++) {
                    if (i >= fInput.fPath.getSegmentCount()) {
                        return false;
                    } else {
                        Object parentElement = parentPath.getSegment(i);
                        Object contextElement = fInput.fPath.getSegment(i);
                        if (!parentElement.equals(contextElement)) {
                            return false;
                        }
                    }
                }
                return true;
            }
            return false;
        }

        public Object[] getElements(Object inputElement) {
            if (fInput != null && 
                fInput.fPath != null) 
            {
                return getChildren(TreePath.EMPTY);
            } else {
                return new Object[] { fgEmptyDebugContextElement };
            }
        }

        public void dispose() {
            fInput = null;
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            if (newInput instanceof Input) {
                fInput = ((Input)newInput);
            } else {
                fInput = null;
            }
        }
    }
        
    private class LabelProvider extends BaseLabelProvider implements ITreePathLabelProvider {
        public void updateLabel(ViewerLabel label, TreePath elementPath) {
            if (fgEmptyDebugContextElement.equals(elementPath.getLastSegment())) {
                label.setText(LaunchViewMessages.Breadcrumb_NoActiveContext);
                label.setImage(null);
            } else {
                ViewerLabel treeViewerLabel = fTreeViewer.getElementLabel(elementPath, null);
                if (treeViewerLabel == null) {
                    label.setText(LaunchViewMessages.Breadcrumb_NoActiveContext);
                    label.setImage(null);
                } else {
                    label.setText(treeViewerLabel.getText());
                    label.setTooltipText(treeViewerLabel.getText());
                    label.setImage(treeViewerLabel.getImage());
                    label.setFont(treeViewerLabel.getFont());
                    label.setForeground(treeViewerLabel.getForeground());
                    label.setBackground(treeViewerLabel.getBackground());
                    
                }
            }            
        }
    }
    
    private final LaunchView fView;
    private final TreeModelViewer fTreeViewer;
    private final IDebugContextProvider fTreeViewerContextProvider;
    private Input fBreadcrumbInput;
    static final private Object fgEmptyDebugContextElement = new Object(); 
    private BreadcrumbViewer fViewer;
    private boolean fRefreshBreadcrumb = false;
    
    private class BreadcrumbContextProvider extends AbstractDebugContextProvider implements IDebugContextListener, ISelectionChangedListener {
        
        private ISelection fBreadcrumbSelection = null;
        
        BreadcrumbContextProvider() {
            super(fView);
            fViewer.addSelectionChangedListener(this);
            fBreadcrumbSelection = fViewer.getSelection();
            fTreeViewerContextProvider.addDebugContextListener(this);
        }
        
        public ISelection getActiveContext() {
            if (fBreadcrumbSelection != null && !fBreadcrumbSelection.isEmpty()) {
                return fBreadcrumbSelection;
            } else {
                ISelection treeViewerSelection = fTreeViewerContextProvider.getActiveContext();
                return treeViewerSelection != null ? treeViewerSelection : StructuredSelection.EMPTY;
            }
        }
        
        void dispose() {
            fViewer.removeSelectionChangedListener(this);
            fTreeViewerContextProvider.removeDebugContextListener(this);
        }
        
        public void debugContextChanged(DebugContextEvent event) {
            fire(new DebugContextEvent(this, getActiveContext(), event.getFlags()));
        }
        
        public void selectionChanged(SelectionChangedEvent event) {
            ISelection oldContext = getActiveContext();
            fBreadcrumbSelection = event.getSelection();
            if (!getActiveContext().equals(oldContext)) {
                fire(new DebugContextEvent(this, getActiveContext(), DebugContextEvent.ACTIVATED));
            }
        }
    }

    private BreadcrumbContextProvider fBreadcrumbContextProvider;
    
    public LaunchViewBreadcrumb(LaunchView view, TreeModelViewer treeViewer, IDebugContextProvider contextProvider) {
        fView = view;
        fTreeViewer = treeViewer;
        fTreeViewer.addLabelUpdateListener(this);
        fTreeViewerContextProvider = contextProvider;
        fBreadcrumbInput = new Input( getPathForSelection(fTreeViewerContextProvider.getActiveContext()) );
        fTreeViewerContextProvider.addDebugContextListener(this);
    }
    
    protected void activateBreadcrumb() {
    }

    protected void deactivateBreadcrumb() {
        if (fViewer.isDropDownOpen()) {
            Shell shell = fViewer.getDropDownShell();
            if (shell != null && !shell.isDisposed()) {
                shell.close();
            }
        }
    }

    protected BreadcrumbViewer createViewer(Composite parent) {
        fViewer = new BreadcrumbViewer(parent, SWT.NONE) {
            protected Control createDropDown(Composite dropDownParent, IBreadcrumbDropDownSite site, TreePath path) {
                return createDropDownControl(dropDownParent, site, path);
            }
        };

        // Force the layout of the breadcrumb viewer so that we may calcualte 
        // its proper size.
        parent.pack(true);

        fViewer.setContentProvider(new ContentProvider());
        fViewer.setLabelProvider(new LabelProvider());

        createMenuManager();
        
        fViewer.setInput(getCurrentInput());
        
        fBreadcrumbContextProvider = new BreadcrumbContextProvider();
        
        return fViewer;
    }

    protected void createMenuManager() {
        MenuManager menuMgr = new MenuManager("#PopUp"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager mgr) {
                fView.fillContextMenu(mgr);
            }
        });
        final Menu menu= menuMgr.createContextMenu(fViewer.getControl());

        // register the context menu such that other plug-ins may contribute to it
        if (fView.getSite() != null) {
            fView.getSite().registerContextMenu(menuMgr, fViewer);
        }
        fView.addContextMenuManager(menuMgr);

        fViewer.addMenuDetectListener(new MenuDetectListener() {
            public void menuDetected(MenuDetectEvent event) {
                menu.setLocation(event.x + 10, event.y + 10);
                menu.setVisible(true);
                while (!menu.isDisposed() && menu.isVisible()) {
                    if (!menu.getDisplay().readAndDispatch())
                        menu.getDisplay().sleep();
                }
            }
        });
    }
   
    protected Object getCurrentInput() {
        return fBreadcrumbInput;
    }

    protected boolean open(ISelection selection) {
        // Let the drop-down control implementation itself handle activating a new context.
        return false;
    }

    public void dispose() {
        fBreadcrumbContextProvider = null;
        fTreeViewerContextProvider.removeDebugContextListener(this);
        fTreeViewer.removeLabelUpdateListener(this);
        fBreadcrumbContextProvider.dispose();
        fViewer = null;
        super.dispose();
    }

    public void debugContextChanged(DebugContextEvent event) {
        if (fView.isBreadcrumbVisible()) {
            fBreadcrumbInput = new Input(getPathForSelection(event.getContext())); 
            if ((event.getFlags() & DebugContextEvent.ACTIVATED) != 0) {
                setInput(getCurrentInput());
            } else {
                refresh();
            }
        }
    }
    
    public void labelUpdateStarted(ILabelUpdate update) {
    }
    
    public void labelUpdateComplete(ILabelUpdate update) {
        if (fBreadcrumbInput != null && fBreadcrumbInput.fPath != null) {
            if (fBreadcrumbInput.fPath.startsWith(update.getElementPath(), null)) {
                synchronized (this) {
                    fRefreshBreadcrumb = true;
                }
            }
        }
    }
    
    public void labelUpdatesBegin() {
    }
    
    public void labelUpdatesComplete() {
        boolean refresh = false;
        synchronized(this) {
            refresh = fRefreshBreadcrumb;
            fRefreshBreadcrumb = false;
        }
        if (fView.isBreadcrumbVisible() && refresh) {
            new UIJob(fViewer.getControl().getDisplay(), "refresh breadcrumb") { //$NON-NLS-1$
                { setSystem(true); }
                public IStatus runInUIThread(IProgressMonitor monitor) {
                    refresh();
                    return Status.OK_STATUS;
                }
            }.schedule();
        }
    }
    
    IDebugContextProvider getContextProvider() {
        return fBreadcrumbContextProvider;
    }
    
    int getHeight() {
        return fViewer.getControl().getSize().y;
    }
    
    private TreePath getPathForSelection(ISelection selection) {
        if (selection instanceof ITreeSelection && !selection.isEmpty()) {
            return ((ITreeSelection)selection).getPaths()[0];
        }
        return null;
    }
    
    public Control createDropDownControl(Composite parent, final IBreadcrumbDropDownSite site, TreePath paramPath) {
        
        TreeViewerDropDown dropDownTreeViewer = new TreeViewerDropDown() {
            
            TreeModelViewer fDropDownViewer;
            
            protected TreeViewer createTreeViewer(Composite composite, int style, TreePath path) {
                fDropDownViewer = new TreeModelViewer(
                    composite, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.VIRTUAL | SWT.POP_UP, 
                    fTreeViewer.getPresentationContext());

                Object launchViewInput = fTreeViewer.getInput();
                fDropDownViewer.setInput(launchViewInput);

                ViewerFilter[] filters = fTreeViewer.getFilters();
                fDropDownViewer.setFilters(filters);
                
                ModelDelta delta = new ModelDelta(launchViewInput, IModelDelta.NO_CHANGE);
                fTreeViewer.saveElementState(TreePath.EMPTY, delta);
                fDropDownViewer.updateViewer(delta);
                
                if (path.getSegmentCount() != 0) {
                    fDropDownViewer.setSelection(new TreeSelection(path), true, true);
                }
                
                fDropDownViewer.addLabelUpdateListener(new ILabelUpdateListener() {
                    public void labelUpdateComplete(ILabelUpdate update) {}
                    public void labelUpdatesBegin() {}
                    public void labelUpdateStarted(ILabelUpdate update) {}
                    public void labelUpdatesComplete() {
                        new UIJob(fViewer.getControl().getDisplay(), "resize breadcrub dropdown") { //$NON-NLS-1$
                            { setSystem(true); }
                            public IStatus runInUIThread(IProgressMonitor monitor) {
                                site.updateSize();
                                return Status.OK_STATUS;
                            }
                        }.schedule();
                    }
                });

                return fDropDownViewer;
            }

            protected void openElement(ISelection selection) {
                if (selection != null && (selection instanceof ITreeSelection) && !selection.isEmpty()) {
                    ModelDelta delta = new ModelDelta(fDropDownViewer.getInput(), IModelDelta.NO_CHANGE); 
                    fDropDownViewer.saveElementState(TreePath.EMPTY, delta);
                    fTreeViewer.updateViewer(delta);
                    fTreeViewer.setSelection(selection, true, true);
                    fViewer.setSelection(StructuredSelection.EMPTY);
                    site.close();
                }
                    
                super.openElement(selection);
            }
        };
        

        return dropDownTreeViewer.createDropDown(parent, site, paramPath);
    }
}
