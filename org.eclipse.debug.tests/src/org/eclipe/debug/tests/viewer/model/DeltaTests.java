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
package org.eclipe.debug.tests.viewer.model;

import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.eclipe.debug.tests.viewer.model.TestModel.TestElement;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.debug.internal.ui.viewers.model.ITreeModelContentProviderTarget;
import org.eclipse.debug.internal.ui.viewers.model.ITreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Tests to verify that the viewer property retrieves and processes the 
 * model deltas generated by the test model. 
 */
abstract public class DeltaTests extends TestCase implements ITestModelUpdatesListenerConstants {
    Display fDisplay;
    Shell fShell;
    ITreeModelViewer fViewer;
    TestModelUpdatesListener fListener;
    
    public DeltaTests(String name) {
        super(name);
    }

    /**
     * @throws java.lang.Exception
     */
    protected void setUp() throws Exception {
        fDisplay = PlatformUI.getWorkbench().getDisplay();
        fShell = new Shell(fDisplay/*, SWT.ON_TOP | SWT.SHELL_TRIM*/);
        fShell.setMaximized(true);
        fShell.setLayout(new FillLayout());

        fViewer = createViewer(fDisplay, fShell);
        
        fListener = new TestModelUpdatesListener(fViewer, false, false);

        fShell.open ();
    }

    abstract protected ITreeModelContentProviderTarget createViewer(Display display, Shell shell);
    
    /**
     * @throws java.lang.Exception
     */
    protected void tearDown() throws Exception {
        fListener.dispose();
        fViewer.getPresentationContext().dispose();
        
        // Close the shell and exit.
        fShell.close();
        while (!fShell.isDisposed()) if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
    }

    protected void runTest() throws Throwable {
        try {
            super.runTest();
        } catch (Throwable t) {
            throw new ExecutionException("Test failed: " + t.getMessage() + "\n fListener = " + fListener.toString(), t);
        }
    }
    
    public void testUpdateLabel() {
        //TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
        
        TestModel model = TestModel.simpleSingleLevel();
        fViewer.setAutoExpandLevel(-1);

        // Create the listener
        fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false); 

        // Set the input into the view and update the view.
        fViewer.setInput(model.getRootElement());
        while (!fListener.isFinished()) if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY);
        
        // Update the model
        TestElement element = model.getRootElement().getChildren()[0];
        TreePath elementPath = new TreePath(new Object[] { element });
        ModelDelta delta = model.appendElementLabel(elementPath, "-modified");
        
        fListener.reset(elementPath, element, -1, true, false); 
        model.postDelta(delta);
        while (!fListener.isFinished(LABEL_COMPLETE | MODEL_CHANGED_COMPLETE)) 
            if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY);
    }

    public void testRefreshStruct() {
        //TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
        
        TestModel model = TestModel.simpleSingleLevel();
        fViewer.setAutoExpandLevel(-1);

        // Create the listener
        fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false); 

        // Set the input into the view and update the view.
        fViewer.setInput(model.getRootElement());
        while (!fListener.isFinished()) if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY);
        
        // Update the model
        TestElement element = model.getRootElement().getChildren()[0];
        TreePath elementPath = new TreePath(new Object[] { element });
        TestElement[] newChildren = new TestElement[] {
            new TestElement(model, "1.1 - new", new TestElement[0]),
            new TestElement(model, "1.2 - new", new TestElement[0]),
            new TestElement(model, "1.3 - new", new TestElement[0]),
        };
        ModelDelta delta = model.setElementChildren(elementPath, newChildren);
        
        fListener.reset(elementPath, element, -1, true, false); 
        model.postDelta(delta);
        while (!fListener.isFinished()) if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY);
    }

    public void testRefreshStruct2() {
        //TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
        
        TestModel model = TestModel.simpleMultiLevel();
        fViewer.setAutoExpandLevel(-1);

        // Create the listener
        fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false); 

        // Set the input into the view and update the view.
        fViewer.setInput(model.getRootElement());
        while (!fListener.isFinished()) if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY);

        String prefix = "new - ";
        model.setElementChildren(TreePath.EMPTY, new TestElement[] {
            new TestElement(model, prefix + "1", new TestElement[0]),
            new TestElement(model, prefix + "2", true, false, new TestElement[] {
                new TestElement(model, prefix + "2.1", true, true, new TestElement[0]),
                new TestElement(model, prefix + "2.2", false, true, new TestElement[0]),
                new TestElement(model, prefix + "2.3", true, false, new TestElement[0]),
            }),
            new TestElement(model, prefix + "3", new TestElement[] {
                new TestElement(model, prefix + "3.1", new TestElement[] {
                    new TestElement(model, prefix + "3.1.1", new TestElement[0]),
                    new TestElement(model, prefix + "3.1.2", new TestElement[0]),
                    new TestElement(model, prefix + "3.1.3", new TestElement[0]),
                }),
                new TestElement(model, prefix + "3.2", new TestElement[] {
                    new TestElement(model, prefix + "3.2.1", new TestElement[0]),
                    new TestElement(model, prefix + "3.2.2", new TestElement[0]),
                    new TestElement(model, prefix + "3.2.3", new TestElement[0]),
                }),
                new TestElement(model, prefix + "3.3", new TestElement[] {
                    new TestElement(model, prefix + "3.3.1", new TestElement[0]),
                    new TestElement(model, prefix + "3.3.2", new TestElement[0]),
                    new TestElement(model, prefix + "3.3.3", new TestElement[0]),
                }),
            })
        });
        
        TestElement element = model.getRootElement();
        fListener.reset(TreePath.EMPTY, element, -1, false, false);
        
        model.postDelta(new ModelDelta(element, IModelDelta.CONTENT));
        while (!fListener.isFinished(ALL_UPDATES_COMPLETE | MODEL_CHANGED_COMPLETE)) 
            if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY);
    }

    public void testInsert() {
        //TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
        
        TestModel model = TestModel.simpleSingleLevel();
        fViewer.setAutoExpandLevel(-1);

        // Create the listener
        fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false); 

        // Set the input into the view and update the view.
        fViewer.setInput(model.getRootElement());
        
        while (!fListener.isFinished()) if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY);
        
        // Update the model
        TestElement element = new TestElement(model, "7", new TestElement[0]);
        TreePath elementPath = new TreePath(new Object[] { element });
        ModelDelta delta = model.insertElementChild(TreePath.EMPTY, 6, element);
        
        // Insert causes the update of element's data, label and children.
        // TODO: update of element's data after insert seems redundant
        // but it's probably not a big inefficiency
        fListener.reset();
        fListener.addChildreUpdate(TreePath.EMPTY, 6);
        fListener.addHasChildrenUpdate(elementPath);
        fListener.addLabelUpdate(elementPath);
        // TODO: redundant label updates on insert!
        fListener.setFailOnRedundantUpdates(false);
        model.postDelta(delta);
        while (!fListener.isFinished(ALL_UPDATES_COMPLETE | MODEL_CHANGED_COMPLETE)) 
            if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY);
    }
    
    /**
     * This test checks that insert and select delta flags are processed in correct order:
     * insert then select.
     */
    public void testInsertAndSelect() {
        //TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
        
        TestModel model = TestModel.simpleSingleLevel();
        fViewer.setAutoExpandLevel(-1);

        // Create the listener
        fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false);

	    // Set the input into the view and update the view.
	    fViewer.setInput(model.getRootElement());
	      
	    while (!fListener.isFinished())
	        if (!fDisplay.readAndDispatch ()) fDisplay.sleep();
	
	    model.validateData(fViewer, TreePath.EMPTY);        
        
        // Update the model
        // Insert two new elements at once
        TestElement element0 = new TestElement(model, "00", new TestElement[] {});
        TestElement element1 = new TestElement(model, "01", new TestElement[] {});
        TreePath elementPath0 = new TreePath(new Object[] { element0 });
        TreePath elementPath1 = new TreePath(new Object[] { element1 });
        ModelDelta rootDelta = model.insertElementChild(TreePath.EMPTY, 0, element0);
        rootDelta = model.insertElementChild(rootDelta, TreePath.EMPTY, 1, element1);
        
        // Set the select flag on the first added node.
        ModelDelta delta0 = rootDelta.getChildDelta(element0);
        delta0.setFlags(delta0.getFlags() | IModelDelta.SELECT);

        fListener.reset();
        fListener.addHasChildrenUpdate(elementPath0);
        fListener.addHasChildrenUpdate(elementPath1);
        fListener.addLabelUpdate(elementPath0);
        fListener.addLabelUpdate(elementPath1);
        
        // TODO: list full set of expected updates.
        fListener.setFailOnRedundantUpdates(false);

        model.postDelta(rootDelta);
        while (!fListener.isFinished()) 
            if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        
        model.validateData(fViewer, TreePath.EMPTY);
    }

    /**
     * This test checks that insert and remove deltas are processed in correct order:
     * remove deltas are processed first then insert deltas.  
     */
    public void testInsertAndRemove() {
        //TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
        
        TestModel model = TestModel.simpleSingleLevel();
        fViewer.setAutoExpandLevel(-1);
        fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false);
        fViewer.setInput(model.getRootElement());
          
        while (!fListener.isFinished())
            if (!fDisplay.readAndDispatch ()) fDisplay.sleep();
    
        model.validateData(fViewer, TreePath.EMPTY);        
        
        // Update the model
        // Remove one element then insert a new one
        IModelDelta removeDelta = model.removeElementChild(TreePath.EMPTY, 3).getChildDeltas()[0];
        
        // Insert new elements at once
        TestElement element = new TestElement(model, "00", new TestElement[] {});
        TreePath elementPath = new TreePath(new Object[] { element });
        IModelDelta insertDelta = model.insertElementChild(TreePath.EMPTY, 1, element).getChildDeltas()[0];

        // Create a combined delta where the insert child delta is first and the remove child delta is second.
        ModelDelta combinedDelta = new ModelDelta(model.getRootElement(), IModelDelta.NO_CHANGE, 0, model.getRootElement().getChildren().length);
        combinedDelta.addNode(insertDelta.getElement(), insertDelta.getIndex(), insertDelta.getFlags(), insertDelta.getChildCount());
        combinedDelta.addNode(removeDelta.getElement(), removeDelta.getIndex(), removeDelta.getFlags(), removeDelta.getChildCount());
        
        // Set the select flag on the first added node.
        fListener.reset();
        fListener.addHasChildrenUpdate(elementPath);
        fListener.addLabelUpdate(elementPath);
        
        // TODO: list full set of expected updates.
        fListener.setFailOnRedundantUpdates(false);

        model.postDelta(combinedDelta);
        while (!fListener.isFinished(ALL_UPDATES_COMPLETE | MODEL_CHANGED_COMPLETE)) 
            if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        
        model.validateData(fViewer, TreePath.EMPTY);
    }

    
    public void testAddElement() {
        //TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
        
        TestModel model = TestModel.simpleSingleLevel();
        fViewer.setAutoExpandLevel(-1);

        // Create the listener
        fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false); 

        // Set the input into the view and update the view.
        fViewer.setInput(model.getRootElement());
        while (!fListener.isFinished()) if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY);
        
        // Update the model
        TestElement element = new TestElement(model, "7", new TestElement[0]);
        TreePath elementPath = new TreePath(new Object[] { element });
        ModelDelta delta = model.addElementChild(TreePath.EMPTY, null, 6, element);
        
        // Add causes the update of parent child count and element's children.
        fListener.reset(elementPath, element, -1, true, false); 
        fListener.addChildreUpdate(TreePath.EMPTY, 6);
        // TODO: redundant updates on add!
        fListener.setFailOnRedundantUpdates(false);
        model.postDelta(delta);
        while (!fListener.isFinished(ALL_UPDATES_COMPLETE | MODEL_CHANGED_COMPLETE)) 
            if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY);
    }

    // This test currently fails.  When (if) bug 311442 gets address we should re-enable it. 
    public void _x_testAddUnexpandedElement() {
        //TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
        
        TestModel model = TestModel.simpleMultiLevel();

        // Turn off auto-expansion
        fViewer.setAutoExpandLevel(0);

        // Create the listener
        fListener.reset(TreePath.EMPTY, model.getRootElement(), 1, true, false); 

        // Set the input into the view and update the view.
        fViewer.setInput(model.getRootElement());
        while (!fListener.isFinished()) if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();

        // Update the model
        TreePath parentPath = model.findElement("1");
        ModelDelta rootDelta = model.addElementChild(parentPath, null, 0, new TestElement(model, "1.1", new TestElement[0]));
        model.addElementChild(parentPath, rootDelta, 1, new TestElement(model, "1.2", new TestElement[0]));
        model.addElementChild(parentPath, rootDelta, 2, new TestElement(model, "1.3", new TestElement[0]));
        model.addElementChild(parentPath, rootDelta, 3, new TestElement(model, "1.4", new TestElement[0]));
        
        // Add causes the update of parent child count and element's children.
        fListener.reset();
        fListener.setFailOnRedundantUpdates(false);
        model.postDelta(rootDelta);
        while (!fListener.isFinished(MODEL_CHANGED_COMPLETE | CONTENT_UPDATES_COMPLETE)) 
            if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();

        // Update the elements that were added.
        fListener.reset();
        fListener.addUpdates((ITreeModelContentProviderTarget)fViewer, TreePath.EMPTY, model.getRootElement(), -1, ALL_UPDATES_COMPLETE);
        rootDelta = new ModelDelta(model.getRootElement(), IModelDelta.CONTENT);
        model.getElementDelta(rootDelta, model.findElement("1.1"), true).setFlags(IModelDelta.CONTENT);
        model.getElementDelta(rootDelta, model.findElement("1.2"), true).setFlags(IModelDelta.CONTENT);
        model.getElementDelta(rootDelta, model.findElement("1.3"), true).setFlags(IModelDelta.CONTENT);
        model.getElementDelta(rootDelta, model.findElement("1.4"), true).setFlags(IModelDelta.CONTENT);
        
        model.postDelta(rootDelta);
        while (!fListener.isFinished(MODEL_CHANGED_COMPLETE | ALL_UPDATES_COMPLETE)) 
            if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        
        fListener.reset(parentPath, model.getElement(parentPath), 1, false, true);
        ((ITreeModelContentProviderTarget)fViewer).expandToLevel(parentPath, 1);

        while (fListener.isFinished(CONTENT_UPDATES_STARTED) && !fListener.isFinished(CONTENT_UPDATES_COMPLETE)) 
            if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();

        model.validateData(fViewer, parentPath);
    }

    public void _x_testRefreshUnexpandedElementsChildren() {
        //TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
        
        TestModel model = TestModel.simpleMultiLevel();

        // Turn off auto-expansion
        fViewer.setAutoExpandLevel(0);

        // Create the listener
        fListener.reset(TreePath.EMPTY, model.getRootElement(), 1, true, false); 

        // Set the input into the view and update the view.
        fViewer.setInput(model.getRootElement());
        while (!fListener.isFinished()) if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();

        // Expand elment "2"
        TreePath parentPath = model.findElement("2");
        fListener.reset(parentPath, model.getElement(parentPath), 1, false, true);
        ((ITreeModelContentProviderTarget)fViewer).expandToLevel(parentPath, 1);

        while (fListener.isFinished(CONTENT_UPDATES_STARTED) && !fListener.isFinished(CONTENT_UPDATES_COMPLETE)) 
            if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();

        // Collapse back element "2"
        ((ITreeModelContentProviderTarget)fViewer).setExpandedState(parentPath, false);
        
        // Update the children of element "2".
        fListener.reset();
        fListener.addUpdates((ITreeModelContentProviderTarget)fViewer, TreePath.EMPTY, model.getRootElement(), -1, ALL_UPDATES_COMPLETE);
        ModelDelta rootDelta = new ModelDelta(model.getRootElement(), IModelDelta.CONTENT);
        model.getElementDelta(rootDelta, model.findElement("2.1"), true).setFlags(IModelDelta.CONTENT);
        model.getElementDelta(rootDelta, model.findElement("2.2"), true).setFlags(IModelDelta.CONTENT);
        model.getElementDelta(rootDelta, model.findElement("2.3"), true).setFlags(IModelDelta.CONTENT);
        
        model.postDelta(rootDelta);
        while (!fListener.isFinished(MODEL_CHANGED_COMPLETE | ALL_UPDATES_COMPLETE)) 
            if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();

        // Expand back element "2"
        fListener.reset(parentPath, model.getElement(parentPath), 1, false, true);
        ((ITreeModelContentProviderTarget)fViewer).expandToLevel(parentPath, 1);

        while (fListener.isFinished(CONTENT_UPDATES_STARTED) && !fListener.isFinished(CONTENT_UPDATES_COMPLETE)) 
            if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();

        model.validateData(fViewer, parentPath, true);
    }

    
    public void testRemove() {
        //TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
        
        TestModel model = TestModel.simpleSingleLevel();
        fViewer.setAutoExpandLevel(-1);

        // Create the listener
        fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, true, false); 

        // Set the input into the view and update the view.
        fViewer.setInput(model.getRootElement());
        while (!fListener.isFinished()) if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY);
        
        // Update the model
        ModelDelta delta = model.removeElementChild(TreePath.EMPTY, 5);
        
        // Remove delta should generate no new updates, but we still need to wait for the event to
        // be processed.
        fListener.reset(); 
        model.postDelta(delta);
        while (!fListener.isFinished(MODEL_CHANGED_COMPLETE)) 
            if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY);
    }
    
    public void testExpandAndSelect() {
        TestModel model = TestModel.simpleMultiLevel();
        
        // Create the listener
        fListener.reset(TreePath.EMPTY, model.getRootElement(), 1, true, false);

        // Set the input into the view and update the view.
        fViewer.setInput(model.getRootElement());
        while (!fListener.isFinished()) if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY, true);

        // Create the delta
        fListener.reset();
        // TODO Investigate: there seem to be unnecessary updates being issued 
        // by the viewer.  These include the updates that are commented out:  
        // For now disable checking for extra updates.
        fListener.setFailOnRedundantUpdates(false);
        TestElement element = model.getRootElement();
        TreePath path_root = TreePath.EMPTY;
        ModelDelta delta= new ModelDelta(model.getRootElement(), -1, IModelDelta.EXPAND, element.getChildren().length);
        ModelDelta deltaRoot = delta;
        element = element.getChildren()[2];
        TreePath path_root_3 = path_root.createChildPath(element);
        delta = delta.addNode(element, 2, IModelDelta.EXPAND, element.fChildren.length);
        fListener.addChildreUpdate(path_root_3, 0);
        TreePath path_root_3_1 = path_root_3.createChildPath(element.getChildren()[0]);
        fListener.addHasChildrenUpdate(path_root_3_1);
        fListener.addLabelUpdate(path_root_3_1);
        TreePath path_root_3_3 = path_root_3.createChildPath(element.getChildren()[2]);
        fListener.addHasChildrenUpdate(path_root_3_3);
        fListener.addLabelUpdate(path_root_3_3);
        //TODO unnecessary update: fListener.addChildreUpdate(path1, 1); 
        fListener.addChildreUpdate(path_root_3, 2);
        element = element.getChildren()[1];
        TreePath path_root_3_2 = path_root_3.createChildPath(element);
        delta = delta.addNode(element, 1, IModelDelta.EXPAND, element.fChildren.length);
        fListener.addLabelUpdate(path_root_3_2);
        TreePath path_root_3_2_1 = path_root_3_2.createChildPath(element.getChildren()[0]);
        fListener.addHasChildrenUpdate(path_root_3_2_1);
        fListener.addLabelUpdate(path_root_3_2_1);
        TreePath path_root_3_2_3 = path_root_3_2.createChildPath(element.getChildren()[2]);
        fListener.addHasChildrenUpdate(path_root_3_2_3);
        fListener.addLabelUpdate(path_root_3_2_3);
        // TODO unnecessary update: fListener.addChildreCountUpdate(path2);
        fListener.addChildreUpdate(path_root_3_2, 0);
        // TODO unnecessary update: fListener.addChildreUpdate(path2, 1); 
        fListener.addChildreUpdate(path_root_3_2, 2);
        element = element.getChildren()[1];
        TreePath path_root_3_2_2 = path_root_3_2.createChildPath(element);
        delta = delta.addNode(element, 1, IModelDelta.SELECT, element.fChildren.length);
        fListener.addLabelUpdate(path_root_3_2_2);
        fListener.addHasChildrenUpdate(path_root_3_2_2);

        // Validate the expansion state BEFORE posting the delta.
        
        ITreeModelContentProviderTarget contentProviderViewer = (ITreeModelContentProviderTarget)fViewer; 
        Assert.assertFalse(contentProviderViewer.getExpandedState(path_root_3));
        Assert.assertFalse(contentProviderViewer.getExpandedState(path_root_3_2));
        Assert.assertFalse(contentProviderViewer.getExpandedState(path_root_3_2_2));
        
        model.postDelta(deltaRoot);
        while (!fListener.isFinished(ALL_UPDATES_COMPLETE | MODEL_CHANGED_COMPLETE)) 
            if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY, true);

        // Validate the expansion state AFTER posting the delta.
        Assert.assertTrue(contentProviderViewer.getExpandedState(path_root_3));
        Assert.assertTrue(contentProviderViewer.getExpandedState(path_root_3_2));
        Assert.assertFalse(contentProviderViewer.getExpandedState(path_root_3_2_2));
        
        // Verify selection
        ISelection selection = fViewer.getSelection();
        if (selection instanceof ITreeSelection) {
            List selectionPathsList = Arrays.asList( ((ITreeSelection)selection).getPaths() );
            Assert.assertTrue(selectionPathsList.contains(path_root_3_2_2));
        } else {
            Assert.fail("Not a tree selection");
        }
    }

    /**
     * This test verifies that expand and select updates are being ignored.
     */
    public void testExpandAndSelect_simple() {
        TestModel model = TestModel.simpleMultiLevel();
        
        // Create the listener
        fListener.reset(TreePath.EMPTY, model.getRootElement(), 1, true, false);

        // Set the input into the view and update the view.
        fViewer.setInput(model.getRootElement());
        while (!fListener.isFinished()) if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY, true);

        // Create the delta
        fListener.reset();
        // TODO Investigate: there seem to be unnecessary updates being issued 
        // by the viewer.  These include the updates that are commented out:  
        // For now disable checking for extra updates.
        fListener.setFailOnRedundantUpdates(false);
        TestElement element = model.getRootElement();
        TreePath path_root = TreePath.EMPTY;
        ModelDelta delta= new ModelDelta(model.getRootElement(), -1, IModelDelta.EXPAND, element.getChildren().length);
        ModelDelta deltaRoot = delta;
        element = element.getChildren()[2];
        TreePath path_root_3 = path_root.createChildPath(element);
        delta.addNode(element, 2, IModelDelta.SELECT | IModelDelta.EXPAND, element.fChildren.length);

        // Validate the expansion state BEFORE posting the delta.
        
        ITreeModelContentProviderTarget contentProviderViewer = (ITreeModelContentProviderTarget)fViewer; 
        Assert.assertFalse(contentProviderViewer.getExpandedState(path_root_3));
        
        model.postDelta(deltaRoot);
        while (true) {
            if (fListener.isFinished(MODEL_CHANGED_COMPLETE)) {
                if (fListener.isFinished(CONTENT_UPDATES_COMPLETE | LABEL_UPDATES_COMPLETE) ) {
                    break;
                }
            }
            if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        }
        model.validateData(fViewer, TreePath.EMPTY, true);

        // Validate the expansion state AFTER posting the delta.
        Assert.assertTrue(contentProviderViewer.getExpandedState(path_root_3));
        
        // Verify selection
        ISelection selection = fViewer.getSelection();
        if (selection instanceof ITreeSelection) {
            List selectionPathsList = Arrays.asList( ((ITreeSelection)selection).getPaths() );
            Assert.assertTrue(selectionPathsList.contains(path_root_3));
        } else {
            Assert.fail("Not a tree selection");
        }
    }

    public void testCompositeModelRefreshStruct() {
        //TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
        
        TestModel model = TestModel.compositeMultiLevel();
        fViewer.setAutoExpandLevel(-1);

        // Create the listener
        // TODO: redundant updates on install deltas 
        fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, false, false);

        // Set the input into the view and update the view.
        fViewer.setInput(model.getRootElement());
        while (!fListener.isFinished()) if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY, true);

        // Update the model
        TreePath m4_2_1Path = model.findElement("m4.2.1");
        TestElement m4_2_1 = model.getElement(m4_2_1Path);
        TestModel m4 = m4_2_1.getModel();
        TestElement[] newChildren = new TestElement[] {
            new TestElement(m4, "4.2.1.new-1", new TestElement[0]),
            new TestElement(m4, "4.2.1.new-2", new TestElement[0]),
            new TestElement(m4, "4.2.1.new-3", new TestElement[0]),
        };

        ModelDelta delta = m4.setElementChildren(m4_2_1Path, newChildren);
        
        fListener.reset(m4_2_1Path, m4_2_1, -1, true, false); 
        model.postDelta(delta);
        while (!fListener.isFinished()) if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY);
    }

    public void testCompositeModelAddElement() {
        TestModel model = TestModel.compositeMultiLevel();
        fViewer.setAutoExpandLevel(-1);

        // Create the listener
        // TODO: redundant updates on install deltas 
        fListener.reset(TreePath.EMPTY, model.getRootElement(), -1, false, false);

        // Set the input into the view and update the view.
        fViewer.setInput(model.getRootElement());
        while (!fListener.isFinished()) if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY, true);
        
        TreePath m3_1Path = model.findElement("m3.1");
        TestElement m3_1 = model.getElement(m3_1Path);
        TestModel m3 = m3_1.getModel();
        TestElement m3_1_new = new TestElement(m3, "m3.1-new", new TestElement[0]);
        TreePath m3_1_newPath = m3_1Path.createChildPath(m3_1_new);
        ModelDelta delta = m3.addElementChild(m3_1Path, null, 0, m3_1_new);
        
        fListener.reset(m3_1_newPath, m3_1_new, -1, true, false); 
        fListener.addChildreUpdate(m3_1Path, 0);
        fListener.setFailOnRedundantUpdates(false);
        
        m3.postDelta(delta);
        while (!fListener.isFinished(ALL_UPDATES_COMPLETE | MODEL_CHANGED_COMPLETE)) 
            if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        
        model.validateData(fViewer, TreePath.EMPTY);
    }
    
    public void testBug292322() {
        //TreeModelViewerAutopopulateAgent autopopulateAgent = new TreeModelViewerAutopopulateAgent(fViewer);
        TestModel model = TestModel.simpleMultiLevel();
        fListener.reset(TreePath.EMPTY, model.getRootElement(), 1, true, false); 

        // Set the input into the view and update the view.
        fViewer.setInput(model.getRootElement());
        while (!fListener.isFinished()) if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();
        model.validateData(fViewer, TreePath.EMPTY, true);
        
        // Update the model: remove one child of an un-expanded element, then
        // make sure that the number of children is correct.
        TreePath parentPath = model.findElement("2");
        TestElement parentElement = model.getElement(parentPath);
        ModelDelta delta = model.removeElementChild(parentPath, 0);
        
        // Update the viewer
        fListener.reset(parentPath, parentElement, 0, false, false);
        //fListener.addChildreCountUpdate(parentPath);
        model.postDelta(delta);
        while (!fListener.isFinished(MODEL_CHANGED_COMPLETE | CONTENT_COMPLETE)) 
            if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();

        // Validate the viewer data.
        model.validateData(fViewer, TreePath.EMPTY, true);

        // Update the model: remove the remaining children and make sure that 
        // the element children are updated to false.
        model.removeElementChild(parentPath, 0);
        
        // Update the viewer
        fListener.reset(parentPath, parentElement, 0, false, false);
        model.postDelta(delta);
        while (!fListener.isFinished(MODEL_CHANGED_COMPLETE | CONTENT_COMPLETE)) 
            if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();

        // Validate the viewer data.
        model.validateData(fViewer, TreePath.EMPTY, true);

        // Update the model: remove the remaining children and make sure that 
        // the element children are updated to false.
        model.removeElementChild(parentPath, 0);
        
        // Update the viewer
        fListener.reset(parentPath, parentElement, 0, false, false);
        model.postDelta(delta);
        while (!fListener.isFinished(MODEL_CHANGED_COMPLETE | CONTENT_COMPLETE)) 
            if (!fDisplay.readAndDispatch ()) fDisplay.sleep ();

        // Validate the viewer data.
        model.validateData(fViewer, TreePath.EMPTY, true);
    }

}
