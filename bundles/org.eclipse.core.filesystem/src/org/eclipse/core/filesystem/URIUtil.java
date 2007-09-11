/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.filesystem;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.runtime.*;

/**
 * This class provides utility methods for comparing, inspecting, and manipulating
 * URIs.  More specifically, this class is useful for dealing with URIs that represent 
 * file systems represented by the <tt>org.eclipse.core.filesystem.filesystems</tt> 
 * extension point. For such URIs the file system implementation can be consulted 
 * to interpret the URI in a way that is not possible at a generic level.
 * <p>
 * This class is not intended to be instantiated or subclassed.
 * </p>
 * 
 * @since org.eclipse.core.filesystem 1.0
 */
public class URIUtil {

	/**
	 * Tests two URIs for equality.  This method delegates equality testing
	 * to the registered file system for the URIs.  If either URI does not 
	 * have a registered file system, the default {@link URI#equals(Object)}
	 * method is used.
	 * 
	 * @param one The first URI to test for equality
	 * @param two The second URI to test for equality
	 * @return <code>true</code> if the first URI is equal to the second,
	 * as defined by the file systems for those URIs, and <code>false</code> otherwise.
	 */
	public static boolean equals(URI one, URI two) {
		try {
			return EFS.getStore(one).equals(EFS.getStore(two));
		} catch (CoreException e) {
			// fall back to default equality test
			return one.equals(two);
		}
	}

	/**
	 * Returns an {@link IPath} representing this {@link URI}
	 * in the local file system, or <code>null</code> if this URI does
	 * not represent a file in the local file system.
	 * 
	 * @param uri The URI to convert
	 * @return The path representing the provided URI, <code>null</code>
	 */
	public static IPath toPath(URI uri) {
		Assert.isNotNull(uri);
		if (EFS.SCHEME_FILE.equals(uri.getScheme()))
			return new Path(uri.getSchemeSpecificPart());
		//handle relative path
		if (uri.getScheme() == null)
			return new Path(uri.getPath());
		return null;
	}

	/**
	 * Converts an {@link IPath} representing a local file system path to a {@link URI}.
	 * 
	 * @param path The path to convert
	 * @return The URI representing the provided path
	 */
	public static URI toURI(IPath path) {
		if (path == null)
			return null;
		if (path.isAbsolute())
			return toURI(path.toFile().getAbsolutePath(), true);
		try {
			//try to preserve the path as a relative path
			return new URI(null, null, path.toString(), null);
		} catch (URISyntaxException e) {
			return toURI(path.toFile().getAbsolutePath(), true);
		}
	}

	/**
	 * Converts a String representing a local file system path to a {@link URI}.
	 * For example, this method can be used to create a URI from the output
	 * of {@link File#getAbsolutePath()}. The provided path string is always treated 
	 * as an absolute path.
	 * 
	 * @param pathString The absolute path string to convert
	 * @return The URI representing the provided path string
	 */
	public static URI toURI(String pathString) {
		return toURI(pathString, true);
	}

	/**
	 * Converts a String representing a local file system path to a {@link URI}.
	 * For example, this method can be used to create a URI from the output
	 * of {@link File#getAbsolutePath()}.
	 * <p>
	 * The <code>forceAbsolute</code> flag controls how this method handles
	 * relative paths.  If the value is <code>true</code>, then the input path
	 * is always treated as an absolute path, and the returned URI will be an 
	 * absolute URI.  If the value is <code>false</code>, then a relative path
	 * provided as input will result in a relative URI being returned.
	 * 
	 * @param pathString The path string to convert
	 * @param forceAbsolute if <code>true</code> the path is treated as an
	 * absolute path
	 * @return The URI representing the provided path string
	 * @since org.eclipse.core.filesystem 1.2
	 */
	public static URI toURI(String pathString, boolean forceAbsolute) {
		if (File.separatorChar != '/')
			pathString = pathString.replace(File.separatorChar, '/');
		final int length = pathString.length();
		StringBuffer pathBuf = new StringBuffer(length + 1);
		//mark if path is relative
		if (length > 0 && (pathString.charAt(0) != '/') && forceAbsolute) {
			pathBuf.append('/');
		}
		//additional double-slash for UNC paths to distinguish from host separator
		if (pathString.startsWith("//")) //$NON-NLS-1$
			pathBuf.append('/').append('/');
		pathBuf.append(pathString);
		try {
			String scheme = null;
			if (length > 0 && (pathBuf.charAt(0) == '/')) {
				scheme = EFS.SCHEME_FILE;
			}
			return new URI(scheme, null, pathBuf.toString(), null);
		} catch (URISyntaxException e) {
			//try java.io implementation
			return new File(pathString).toURI();
		}
	}

	/**
	 * Private constructor to prevent instantiation.
	 */
	private URIUtil() {
		super();
	}
}
