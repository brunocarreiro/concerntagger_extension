/* ConcernMapper - A concern modeling plug-in for Eclipse
 * Copyright (C) 2006  McGill University (http://www.cs.mcgill.ca/~martin/cm)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * $Revision: 1.4 $
 */

package edu.columbia.concerns.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import edu.columbia.concerns.ConcernTagger;
import edu.columbia.concerns.model.IConcernModelProvider;

/**
 * Clears the concern model.
 */
public class RemoveAllConcernsAction extends Action
{
	private IConcernModelProvider concernModelProvider;
	private IStatusLineManager statusLineManager;
	
	/**
	 * The constructor. Sets the text label and tooltip
	 */
	public RemoveAllConcernsAction(IConcernModelProvider concernModelProvider,
	                               IStatusLineManager statusLineManager)
	{
		this.concernModelProvider = concernModelProvider;
		this.statusLineManager = statusLineManager;
		
		setText(ConcernTagger
				.getResourceString("actions.RemoveAllConcernsAction.Label"));
		//setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(
		//		ConcernMapper.ID_PLUGIN, "icons/delete.png"));
		setImageDescriptor( PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor( ISharedImages.IMG_TOOL_DELETE)); 
		setToolTipText(ConcernTagger
				.getResourceString("actions.RemoveAllConcernsAction.ToolTip"));
	}

	/**
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	@Override
	public void run()
	{
		if (shouldProceed())
		{
			int numRemoved = concernModelProvider.getModel().removeAllConcerns();
			statusLineManager.setMessage(numRemoved + " concerns removed");
		}
	}

	private boolean shouldProceed()
	{
		return MessageDialog.openQuestion(
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
			ConcernTagger.getResourceString("actions.RemoveAllConcernsAction.QuestionDialogTitle"),
			ConcernTagger.getResourceString("actions.RemoveAllConcernsAction.WarningOverwrite"));
	}
}
