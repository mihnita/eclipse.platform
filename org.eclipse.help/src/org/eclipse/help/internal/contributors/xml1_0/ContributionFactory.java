package org.eclipse.help.internal.contributors.xml1_0;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import org.xml.sax.*;
import org.eclipse.help.internal.contributions1_0.*;
import org.eclipse.help.internal.contributors1_0.*;
import org.eclipse.help.internal.contributions.xml1_0.*;

/**
 * ContributionFactory
 */
public class ContributionFactory {
	protected static final ContributionFactory instance = new ContributionFactory();
	/**
	 * ContributionFactory constructor.
	 */
	public ContributionFactory() {
		super();
	}
	public Contribution createContribution(String name, Attributes atts) {
		Contribution e = null;
		if (name.equals(TopicContributor.TOPIC_ELEM))
			e = new HelpTopic(atts);
		else
			if (name.equals(ActionContributor.ACTIONS_ELEM))
				e = new HelpAction(atts);
			else
				if (name.equals(TopicContributor.TOPICS_ELEM))
					e = new HelpTopic(atts); // create a topic too
				else
					if (name.equals(ActionContributor.INSERT_ELEM))
						e = new HelpInsert(atts);
					else
						if (name.equals(ViewContributor.INFOSET_ELEM))
							e = new HelpInfoSet(atts);
						else
							if (name.equals(ViewContributor.VIEW_ELEM))
								e = new HelpInfoView(atts);
							else
								return null;

		return e;
	}
	public static ContributionFactory instance() {
		return instance;
	}
}
