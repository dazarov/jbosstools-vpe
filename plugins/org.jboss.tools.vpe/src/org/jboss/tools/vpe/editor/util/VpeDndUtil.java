/*******************************************************************************
 * Copyright (c) 2007 Exadel, Inc. and Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Exadel, Inc. and Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.vpe.editor.util;

import static org.jboss.tools.vpe.xulrunner.util.XPCOM.queryInterface;

import java.util.Properties;

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.jboss.tools.common.model.XModelException;
import org.jboss.tools.common.model.ui.dnd.DnDUtil;
import org.jboss.tools.common.model.ui.editor.IModelObjectEditorInput;
import org.jboss.tools.jst.web.ui.internal.editor.jspeditor.dnd.JSPPaletteInsertHelper;
import org.jboss.tools.vpe.VpePlugin;
import org.mozilla.interfaces.nsIFile;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.interfaces.nsISupportsCString;
import org.mozilla.interfaces.nsISupportsString;
import org.mozilla.xpcom.XPCOMException;

public class VpeDndUtil {
    
	public static boolean isDropEnabled(IModelObjectEditorInput input) {
    	return DnDUtil.isPasteEnabled(input.getXModelObject());
    }

    public static void drop(IModelObjectEditorInput input,
	    ISourceViewer viewer, ISelectionProvider provider) {
	Properties properties = new Properties();
	properties.setProperty("isDrop", "true");  //$NON-NLS-1$//$NON-NLS-2$
	properties.setProperty("actionSourceGUIComponentID", "editor");  //$NON-NLS-1$//$NON-NLS-2$
	properties.setProperty("accepsAsString", "true");  //$NON-NLS-1$//$NON-NLS-2$
	properties.put("selectionProvider", provider); //$NON-NLS-1$
	properties.put("viewer", viewer); //$NON-NLS-1$

		try {
		    DnDUtil.paste(input.getXModelObject(), properties);
		    JSPPaletteInsertHelper.getInstance().insertIntoEditor(viewer, properties);
		} catch (XModelException ex) {
		    VpePlugin.getPluginLog().logError(ex);
		}
    }
    
	/**
	 * Determine is nsIFile instance.
	 * 
	 * @param support
	 * @return
	 */
	public static boolean isNsIFileInstance(nsISupports support) {
        boolean rst = true;
        
        try {
        	queryInterface(support, nsIFile.class);
        } catch (XPCOMException e) {
            rst = false;
        }      
        return rst;
    }
	/**
	 * Determine is csstring instance
	 * @param support
	 * @return
	 */
	public static boolean isNsICStringInstance(nsISupports support) {
        boolean rst = true;

        try {
        	queryInterface(support, nsISupportsCString.class);
        } catch (XPCOMException e) {
            rst = false;
        }
        return rst;
    }
    /**
     * Determine is string instance
     * @param support
     * @return
     */
	public static boolean isNsIStringInstance(nsISupports support) {
        boolean rst = true;

        try {
        	queryInterface(support, nsISupportsString.class);
        } catch (XPCOMException e) {
            rst = false;
        }
        return rst;
    }

}
