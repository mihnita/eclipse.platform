/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.mapping;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.mapping.IResourceDiff;
import org.eclipse.team.core.mapping.ISynchronizationContext;

/**
 * A remote resource mapping context that wraps a synchronization context.
 * This is used by the {@link SynchronizationContentProvider} to get the traversals
 * for resource mappings. Since it is used to provide content, it avoids long running
 * operations if possible.
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is a guarantee neither that this API will
 * work nor that it will remain the same. Please do not use this API without
 * consulting with the Platform/Team team.
 * </p>
 * 
 * @since 3.2
 */
public final class SynchronizationResourceMappingContext extends
		RemoteResourceMappingContext {

	private final ISynchronizationContext context;

	/**
	 * Create a resource mapping context for the given synchronization context
	 * @param context the synchronization context
	 */
	public SynchronizationResourceMappingContext(ISynchronizationContext context) {
		this.context = context;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.mapping.RemoteResourceMappingContext#isThreeWay()
	 */
	public boolean isThreeWay() {
		return context.getType() == ISynchronizationContext.THREE_WAY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.mapping.RemoteResourceMappingContext#hasRemoteChange(org.eclipse.core.resources.IResource, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public boolean hasRemoteChange(IResource resource, IProgressMonitor monitor) throws CoreException {
		IDiff diff = context.getDiffTree().getDiff(resource);
		if (diff instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) diff;
			IDiff remote = twd.getRemoteChange();
			return remote != null && remote.getKind() != IDiff.NO_CHANGE;
		}
		return diff != null && diff.getKind() != IDiff.NO_CHANGE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.mapping.RemoteResourceMappingContext#hasLocalChange(org.eclipse.core.resources.IResource, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public boolean hasLocalChange(IResource resource, IProgressMonitor monitor) throws CoreException {
		IDiff diff = context.getDiffTree().getDiff(resource);
		if (diff instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) diff;
			IDiff local = twd.getLocalChange();
			return local != null && local.getKind() != IDiff.NO_CHANGE;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.mapping.RemoteResourceMappingContext#fetchRemoteContents(org.eclipse.core.resources.IFile, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStorage fetchRemoteContents(IFile file, IProgressMonitor monitor) throws CoreException {
		IDiff diff = context.getDiffTree().getDiff(file);
		if (diff instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) diff;
			IDiff remote = twd.getRemoteChange();
			if (remote instanceof IResourceDiff) {
				IResourceDiff rd = (IResourceDiff) remote;
				return rd.getAfterState().getStorage(monitor);
			}
		} else if (diff instanceof IResourceDiff) {
			IResourceDiff rd = (IResourceDiff) diff;
			return rd.getAfterState().getStorage(monitor);
		}
		return file;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.mapping.RemoteResourceMappingContext#fetchBaseContents(org.eclipse.core.resources.IFile, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStorage fetchBaseContents(IFile file, IProgressMonitor monitor) throws CoreException {
		IDiff diff = context.getDiffTree().getDiff(file);
		if (diff instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) diff;
			IDiff remote = twd.getRemoteChange();
			if (remote instanceof IResourceDiff) {
				IResourceDiff rd = (IResourceDiff) remote;
				return rd.getBeforeState().getStorage(monitor);
			}
			IDiff local = twd.getLocalChange();
			if (local instanceof IResourceDiff) {
				IResourceDiff rd = (IResourceDiff) local;
				return rd.getBeforeState().getStorage(monitor);
			}
		}
		return null;
	}

	public IResource[] fetchMembers(IContainer container, IProgressMonitor monitor) throws CoreException {
		Set result = new HashSet();
		IResource[] children = container.members();
		for (int i = 0; i < children.length; i++) {
			IResource resource = children[i];
			result.add(resource);
		}
		IPath[] childPaths = context.getDiffTree().getChildren(container.getFullPath());
		for (int i = 0; i < childPaths.length; i++) {
			IPath path = childPaths[i];
			IDiff delta = context.getDiffTree().getDiff(path);
			IResource child;
			if (delta == null) {
				// the path has descendent deltas so it must be a folder
				if (path.segmentCount() == 1) {
					child = ((IWorkspaceRoot)container).getProject(path.lastSegment());
				} else {
					child = container.getFolder(new Path(path.lastSegment()));
				}
			} else {
				child = context.getDiffTree().getResource(delta);
			}
			result.add(child);
		}
		return (IResource[]) result.toArray(new IResource[result.size()]);
	}

	public void refresh(ResourceTraversal[] traversals, int flags, IProgressMonitor monitor) throws CoreException {
		//context.refresh(traversals, flags, monitor);
	}

	public ISynchronizationContext getSynchronizationContext() {
		return context;
	}

}
