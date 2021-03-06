/* ConcernMapper - A concern modeling plug-in for Eclipse
 * Copyright (C) 2006  McGill University (http://www.cs.mcgill.ca/~martin/cm)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * $Revision: 1.7 $
 */

package edu.columbia.concerns.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IViewSite;

import edu.columbia.concerns.ConcernTagger;
import edu.columbia.concerns.model.IConcernModelProvider;
import edu.columbia.concerns.repository.ConcernDomain;

/**
 * An action to rename a concern to the model.
 */
public class RenameConcernDomainAction extends Action
{
	private IViewSite site;
	private IConcernModelProvider concernModelProvider;

	/**
	 * Creates the action.
	 * @param pConcern
	 *            The view from where the action is triggered
	 * @param pViewer
	 *            The viewer controlling this action.
	 */
	public RenameConcernDomainAction(	IConcernModelProvider concernModelProvider, 
	                                 	IViewSite site)
	{
		this.site = site;
		this.concernModelProvider = concernModelProvider;
		
		setText(ConcernTagger
				.getResourceString("actions.RenameConcernDomainAction.Label"));
		setToolTipText(ConcernTagger
				.getResourceString("actions.RenameConcernDomainAction.ToolTip"));
	}

	/**
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	@Override
	public void run()
	{
		final ConcernDomain concernDomain = concernModelProvider.getModel().getConcernDomain();
		
		InputDialog lDialog = new InputDialog(
				site.getShell(),
				ConcernTagger
						.getResourceString("actions.RenameConcernDomainAction.DialogTitle"),
				ConcernTagger
						.getResourceString("actions.RenameConcernDomainAction.DialogLabel"),
				concernDomain.getName(), 
				new IInputValidator()
					{
						public String isValid(String pName)
						{
							if (concernDomain.getName().equals(pName))
							{
								return ConcernTagger.getResourceString("SameName");
							}
							else
							{
								return ConcernDomain.isNameValid(pName);
							}
						}
					}
				);

		if (lDialog.open() == Window.OK)
		{
			concernDomain.rename(lDialog.getValue());
		}
	}
}
