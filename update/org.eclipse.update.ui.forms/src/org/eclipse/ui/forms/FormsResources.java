/*
 * Created on Nov 27, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.ui.forms;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;

/**
 * Utility methods to access shared form-specific resources.
 * <p>
 * All methods declared on this class are static. This
 * class cannot be instantiated.
 * </p>
 * <p>
 * </p>
 */
public class FormsResources {
	private static Cursor busyCursor;
	private static Cursor handCursor;
	private static Cursor textCursor;
	
	public static Cursor getBusyCursor() {
		if (busyCursor==null)
			busyCursor = new Cursor(Display.getCurrent(), SWT.CURSOR_WAIT);
		return busyCursor;
	}
	public static Cursor getHandCursor() {
		if (handCursor==null)
			handCursor = new Cursor(Display.getCurrent(), SWT.CURSOR_HAND);
		return handCursor;
	}
	public static Cursor getTextCursor() {
		if (textCursor==null)
			textCursor = new Cursor(Display.getCurrent(), SWT.CURSOR_IBEAM);
		return textCursor;
	}
	public static void shutdown() {
		if (busyCursor!=null)
			busyCursor.dispose();
		if (handCursor!=null)
			handCursor.dispose();
		if (textCursor!=null)
			textCursor.dispose();
		busyCursor=null;
		handCursor=null;
		textCursor=null;
	}
}