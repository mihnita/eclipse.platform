package org.eclipse.help.internal.contributors1_0;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.lang.*;

/**
 * Topic contributor.
 */
public interface TopicContributor extends Contributor {
	public static final String TOPICS_ELEM = "topics";
	public static final String TOPICS_NAME_ATTR = NAME_ATTR;
	public static final String TOPICS_ID_ATTR = ID_ATTR;

	public static final String TOPIC_ELEM = "topic";
	public static final String TOPIC_ID_ATTR = ID_ATTR;
	public static final String TOPIC_LABEL_ATTR = "label";
	public static final String TOPIC_HREF_ATTR = "href";

}
