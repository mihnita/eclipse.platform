/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.datatransfer;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.taskdefs.Javac;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.ant.internal.ui.model.AntElementNode;
import org.eclipse.ant.internal.ui.model.AntModelContentProvider;
import org.eclipse.ant.internal.ui.model.AntProjectNode;
import org.eclipse.ant.internal.ui.model.AntTargetNode;
import org.eclipse.ant.internal.ui.model.AntTaskNode;
import org.eclipse.ant.internal.ui.model.IAntModel;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.IImportStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;

public class AntNewJavaProjectPage extends WizardPage {
	
	private static class ImportOverwriteQuery implements IOverwriteQuery {
		public String queryOverwrite(String file) {
			return ALL;
		}	
	}	

	private Text fProjectNameField;
	private Text fLocationPathField;
	private Button fBrowseButton;
	
	private IAntModel fAntModel;

	private ModifyListener fLocationModifyListener = new ModifyListener() {
		public void modifyText(ModifyEvent e) {
		    //no lexical or position, has task info
			fAntModel= AntUtil.getAntModel(getProjectLocationFieldValue(), false, false, true);
            AntProjectNode projectNode= fAntModel == null ? null : fAntModel.getProjectNode();
			if (fAntModel != null && projectNode != null) {
			    setProjectName(); // page will be validated on setting the project name
                List javacNodes= new ArrayList();
                getJavacNodes(javacNodes, projectNode);
                fTableViewer.setInput(javacNodes.toArray());
                if (!javacNodes.isEmpty()) {
                    fTableViewer.setSelection(new StructuredSelection(javacNodes.get(0)));
                }
			} else {
                fTableViewer.setInput(new Object[] {});
			}
            setPageComplete(validatePage());
		}
	};
	
	private ModifyListener fNameModifyListener = new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			setPageComplete(validatePage());
		}
	};

	private static final int SIZING_TEXT_FIELD_WIDTH = 250;
    private TableViewer fTableViewer;
	
	public AntNewJavaProjectPage() {
		super("newPage"); //$NON-NLS-1$
		setPageComplete(false);
		setTitle(DataTransferMessages.AntNewJavaProjectPage_9);
		setDescription(DataTransferMessages.AntNewJavaProjectPage_10);

	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		
		initializeDialogUnits(parent);
		Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight= IDialogConstants.VERTICAL_MARGIN;
        layout.marginWidth= IDialogConstants.HORIZONTAL_MARGIN;
        layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        layout.numColumns= 3;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setFont(parent.getFont());
        
		// TODO: help context
		//WorkbenchHelp.setHelp(composite, IIDEHelpContextIds.NEW_PROJECT_WIZARD_PAGE);

		createProjectNameGroup(composite);
		createProjectLocationGroup(composite);
        createTargetsTable(composite);
		validatePage();
		// Show description on opening
		setErrorMessage(null);
		setMessage(null);
		setControl(composite);
	}

	/**
	 * Creates the project location specification controls.
	 *
	 * @param parent the parent composite
	 */
	private final void createProjectLocationGroup(Composite parent) {
		// new project label
		Label projectContentsLabel = new Label(parent, SWT.NONE);
		projectContentsLabel.setText(DataTransferMessages.AntNewJavaProjectPage_11);
		projectContentsLabel.setFont(parent.getFont());

		createUserSpecifiedProjectLocationGroup(parent);
	}
	/**
	 * Creates the project name specification controls.
	 *
	 * @param parent the parent composite
	 */
	private final void createProjectNameGroup(Composite parent) {
		
		Font dialogFont = parent.getFont();

		// new project label
		Label projectLabel = new Label(parent, SWT.NONE);
		projectLabel.setText(DataTransferMessages.AntNewJavaProjectPage_12);
		projectLabel.setFont(dialogFont);
        GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        projectLabel.setLayoutData(gd);

		// new project name entry field
		fProjectNameField = new Text(parent, SWT.BORDER);
        gd= new GridData();
        gd.horizontalAlignment= GridData.FILL;
        gd.grabExcessHorizontalSpace= false;
        gd.horizontalSpan= 2;
		fProjectNameField.setLayoutData(gd);
		fProjectNameField.setFont(dialogFont);
		
		fProjectNameField.addModifyListener(fNameModifyListener);
	}
	/**
	 * Creates the project location specification controls.
	 *
	 * @param projectGroup the parent composite
	 * @param boolean - the initial enabled state of the widgets created
	 */
	private void createUserSpecifiedProjectLocationGroup(Composite projectGroup) {
		
		Font dialogFont = projectGroup.getFont();

		// project location entry field
		fLocationPathField = new Text(projectGroup, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = SIZING_TEXT_FIELD_WIDTH;
		fLocationPathField.setLayoutData(data);
		fLocationPathField.setFont(dialogFont);

		// browse button
		fBrowseButton = new Button(projectGroup, SWT.PUSH);
		fBrowseButton.setText(DataTransferMessages.AntNewJavaProjectPage_13);
		fBrowseButton.setFont(dialogFont);
		setButtonLayoutData(fBrowseButton);
		
		fBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				handleBrowseButtonPressed();
			}
		});

		fLocationPathField.addModifyListener(fLocationModifyListener);
	}

	/**
	 * Returns the current project name
	 *
	 * @return the project name
	 */
	private String getProjectName(AntProjectNode projectNode) {
		String projectName= projectNode.getLabel();
		if (projectName == null) {
			projectName= DataTransferMessages.AntNewJavaProjectPage_14;
		}
		return projectName;
	}
	/**
	 * Returns the value of the project name field
	 * with leading and trailing spaces removed.
	 * 
	 * @return the project name in the field
	 */
	private String getProjectNameFieldValue() {
		if (fProjectNameField == null) {
			return ""; //$NON-NLS-1$
		} 
		return fProjectNameField.getText().trim();
	}
	/**
	 * Returns the value of the project location field
	 * with leading and trailing spaces removed.
	 * 
	 * @return the project location directory in the field
	 */
	private String getProjectLocationFieldValue() {
		return fLocationPathField.getText().trim();
	}
	
	/**
	 * Determine the buildfile the user wishes to operate from
	 */
	private void handleBrowseButtonPressed() {
		
		String lastUsedPath= null;
		//lastUsedPath= dialogSettings.get(IAntUIConstants.DIALOGSTORE_LASTEXTFILE);
		if (lastUsedPath == null) {
			lastUsedPath= ""; //$NON-NLS-1$
		}
		FileDialog dialog = new FileDialog(getShell(), SWT.SINGLE);
		dialog.setFilterExtensions(new String[] { "*.xml" }); //$NON-NLS-1$;
		dialog.setFilterPath(lastUsedPath);

		String result = dialog.open();
		if (result == null) {
			return;
		}
		IPath filterPath= new Path(dialog.getFilterPath());
		String buildFileName= dialog.getFileName();
		IPath path= filterPath.append(buildFileName).makeAbsolute();	
		
		fLocationPathField.setText(path.toOSString());
	}

	/**
	 * Returns whether this page's controls currently all contain valid 
	 * values.
	 *
	 * @return <code>true</code> if all controls are valid, and
	 *   <code>false</code> if at least one is invalid
	 */
	private boolean validatePage() {

		String locationFieldContents = getProjectLocationFieldValue();

		if (locationFieldContents.equals("")) { //$NON-NLS-1$
			setErrorMessage(null);
			setMessage(DataTransferMessages.AntNewJavaProjectPage_15);
			return false;
		}

		IPath path = new Path(""); //$NON-NLS-1$
		if (!path.isValidPath(locationFieldContents)) {
			setErrorMessage(DataTransferMessages.AntNewJavaProjectPage_16);
			return false;
		}

		if (fAntModel == null) {
			if (getBuildFile(locationFieldContents) == null) {
				setErrorMessage(DataTransferMessages.AntNewJavaProjectPage_0);
				return false;
			} 
			setErrorMessage(DataTransferMessages.AntNewJavaProjectPage_17);
			return false;
		}
		
		if (fAntModel.getProjectNode() == null) {
		    setErrorMessage(DataTransferMessages.AntNewJavaProjectPage_2);
		    return false;
		}
		
		if (getProjectNameFieldValue().length() == 0) {
			setErrorMessage(DataTransferMessages.AntNewJavaProjectPage_18);
			return false;
		} 
		IProject existingProject= ResourcesPlugin.getWorkspace().getRoot().getProject(getProjectNameFieldValue());
		if (existingProject.exists()) {
			setErrorMessage(DataTransferMessages.AntNewJavaProjectPage_19);
			return false;
		}
        
		
		if (fTableViewer.getTable().getItemCount() == 0) {
		    setErrorMessage(DataTransferMessages.AntNewJavaProjectPage_1);
		    setPageComplete(false);
		    return false;
		}
       
		setErrorMessage(null);
		setMessage(null);
		return true;
	}

	/**
	 * Set the project name using either the name of the
	 * parent of the file or the name entry in the xml for 
	 * the file
	 */
	private void setProjectName() {
		AntProjectNode node= fAntModel.getProjectNode();
		String projectName= getProjectName(node);
		
		fProjectNameField.setText(projectName);
	}

	/**
	 * Return a .xml file from the specified location.
	 * If there isn't one return null.
	 */
	private File getBuildFile(String locationFieldContents) {
		File buildFile = new File(locationFieldContents);
		if (!buildFile.isFile() || !buildFile.exists()) { 
			return null;
		}

		return buildFile;
	}

	/**
	 * Creates a new project resource based on the Ant buildfile.
	 * The classpath is configured based on the classpath of the javac declaration in the buildfile.
	 *
	 * @return the created project resource, or <code>null</code> if the project
	 *    was not created
	 */
	protected IJavaProject createProject() {
		final IJavaProject[] result= new IJavaProject[1];
		final String projectName= getProjectNameFieldValue();
		final File buildFile= getBuildFile(getProjectLocationFieldValue());
		final List selectedJavacs= ((IStructuredSelection)fTableViewer.getSelection()).toList();
		WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
			protected void execute(IProgressMonitor monitor) throws CoreException {
				List javacTasks= resolveJavacTasks(selectedJavacs);
				ProjectCreator creator= new ProjectCreator();
				Iterator iter= javacTasks.iterator();
				while (iter.hasNext()) {
					Javac javacTask = (Javac) iter.next();
					IJavaProject javaProject= creator.createJavaProjectFromJavacNode(projectName, javacTask, monitor);
					importBuildFile(monitor, javaProject.getPath(), buildFile);
					result[0]= javaProject;
				}
			}
		};
		
		//run the new project creation operation
		try {
			getContainer().run(true, true, op);
		} catch (InterruptedException e) {
			return null;
		} catch (InvocationTargetException e) {
			// ie.- one of the steps resulted in a core exception	
			Throwable t = e.getTargetException();
			IStatus status= null;
			if (t instanceof CoreException) {	
				status= ((CoreException) t).getStatus();
			} else {
			    status= new Status(IStatus.ERROR, AntUIPlugin.PI_ANTUI, IStatus.OK, "Error occurred. Check log for details ", t); //$NON-NLS-1$
			    AntUIPlugin.log(t);
			}
			ErrorDialog.openError(getShell(), DataTransferMessages.AntNewJavaProjectPage_21,
					null, status);
		}
		
		return result[0];
	}
	
	protected void importBuildFile(IProgressMonitor monitor, IPath destPath, File buildFile) {
		IImportStructureProvider structureProvider = FileSystemStructureProvider.INSTANCE;
		List files = new ArrayList(1);
		
		files.add(buildFile);
		File rootDir= buildFile.getParentFile();
		try {
			ImportOperation op= new ImportOperation(destPath, rootDir, structureProvider, new ImportOverwriteQuery(), files);
			op.setCreateContainerStructure(false);
			op.run(monitor);
		} catch (InterruptedException e) {
			// should not happen
		} catch (InvocationTargetException e) {
			Throwable t = e.getTargetException();
			if (t instanceof CoreException) {	
				ErrorDialog.openError(getShell(), DataTransferMessages.AntNewJavaProjectPage_22,
				null, ((CoreException) t).getStatus());
			}
		}
	}

	private List resolveJavacTasks(List javacNodes) {
		List resolvedJavacTasks= new ArrayList(javacNodes.size());
		Iterator nodes= javacNodes.iterator();
		while (nodes.hasNext()) {
			AntTaskNode taskNode = (AntTaskNode) nodes.next();
			Task javacTask= taskNode.getTask();
			if (javacTask instanceof UnknownElement) {
				if (((UnknownElement)javacTask).getRealThing() == null) {
					javacTask.maybeConfigure();
				}
				
				resolvedJavacTasks.add(((UnknownElement)javacTask).getRealThing());
			} else {
				resolvedJavacTasks.add(javacTask);
			}	
		}
		return resolvedJavacTasks;
	}
	
	private void getJavacNodes(List javacNodes, AntElementNode parent) {
		if (!parent.hasChildren()) {
			return;
		}
		List children= parent.getChildNodes();
		for (Iterator iter = children.iterator(); iter.hasNext();) {
			AntElementNode node = (AntElementNode) iter.next();
			if (node instanceof AntTargetNode) {
				getJavacNodes(javacNodes, node);
			} else if (node instanceof AntTaskNode) {
				AntTaskNode task= (AntTaskNode)node;
				if ("javac".equals(task.getName())) { //$NON-NLS-1$
					javacNodes.add(task);
				}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			fLocationPathField.setFocus();
		}
	}
    
 	/**
     * Creates the table which displays the available targets
     * @param parent the parent composite
     */
    private void createTargetsTable(Composite parent) {
        Font font= parent.getFont();
        Label label = new Label(parent, SWT.NONE);
        label.setFont(font);
        label.setText(DataTransferMessages.AntNewJavaProjectPage_3);
        GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        gd.horizontalSpan= 3;
        label.setLayoutData(gd);
        
        Table table= new Table(parent, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION | SWT.RESIZE);
        
        GridData data= new GridData(GridData.FILL_BOTH);
        int availableRows= availableRows(parent);
        data.heightHint = table.getItemHeight() * (availableRows / 20);
        data.widthHint= 250;
        data.horizontalSpan= 3;
        table.setLayoutData(data);
        table.setFont(font);
        
        fTableViewer = new TableViewer(table);
        fTableViewer.setLabelProvider(new JavacTableLabelProvider());
        fTableViewer.setContentProvider(new AntModelContentProvider());
    }
    
    /**
     * Return the number of rows available in the current display using the
     * current font.
     * @param parent The Composite whose Font will be queried.
     * @return int The result of the display size divided by the font size.
     */
    private int availableRows(Composite parent) {

        int fontHeight = (parent.getFont().getFontData())[0].getHeight();
        int displayHeight = parent.getDisplay().getClientArea().height;

        return displayHeight / fontHeight;
    }
}