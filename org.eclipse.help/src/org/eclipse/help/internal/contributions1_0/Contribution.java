package org.eclipse.help.internal.contributions1_0;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */



import java.util.*;

import org.eclipse.help.topics.*;


/**
 * Interface for help contributions (topics, actions,views, etc.)
 */
public interface Contribution /* 1.0 nav support */extends IDescriptor, ITopicNode/* eo 1.0 nav support */{
	// switches indicating how to insert
	public final static int FIRST = 1;
	public final static int LAST = -1;
	public final static int NEXT = 129;
	public final static int NORMAL = 0;
	public final static int PREV = 127;


	/**
	 */
	void accept(Visitor visitor);
	/**
	 */
	Iterator getChildren();
	/**
	 */
	List getChildrenList();
	/**
	 */
	String getID();
	/**
	 */
	String getLabel();
	/**
	 */
	Contribution getParent();
	/**
	 */
	String getRawLabel();
}
