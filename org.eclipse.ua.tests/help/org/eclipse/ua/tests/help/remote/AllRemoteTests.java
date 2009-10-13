/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ua.tests.help.remote;

import junit.framework.Test;
import junit.framework.TestSuite;

/*
 * Tests help keyword index functionality.
 */
public class AllRemoteTests extends TestSuite {

	/*
	 * Returns the entire test suite.
	 */
	public static Test suite() {
		return new AllRemoteTests();
	}

	/*
	 * Constructs a new test suite.
	 */
	public AllRemoteTests() {
		addTestSuite(SearchServletTest.class);
		addTestSuite(RemotePreferenceTest.class);
		addTestSuite(SearchUsingRemoteHelp.class);		
	}
}
