package org.eclipse.help.internal.contributors.xml1_0;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import org.eclipse.core.runtime.*;
import org.eclipse.help.internal.contributors1_0.*;

/**
 * Factory for view contributors
 */
public class XMLViewContributorCreator implements ContributorCreator {
	/**
	 * XMLTopicContributorCreator constructor comment.
	 */
	public XMLViewContributorCreator() {
		super();
	}
	/**
	 * create method comment.
	 */
	public Contributor create(
		IPluginDescriptor plugin,
		IConfigurationElement configuration) {
		return new XMLViewContributor(plugin, configuration);
	}
}
