package edu.columbia.concerns.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;

import edu.columbia.concerns.ConcernTagger;
import edu.columbia.concerns.model.ConcernEvent;
import edu.columbia.concerns.model.ConcernModelFactory;
import edu.columbia.concerns.model.IConcernListener;
import edu.columbia.concerns.model.IConcernModelProvider;
import edu.columbia.concerns.repository.Component;
import edu.columbia.concerns.repository.Concern;
import edu.columbia.concerns.repository.EdgeKind;

public abstract class MultiElementAction 
	implements 
		IActionDelegate,			// For selection change notification
		IEditorActionDelegate,		// To capture java element selection in the editor 
		IMenuCreator,				// Allows us to create the drop down menu
		IConcernListener		// Refresh menu when concerns and assignments change
{
	private Menu menu;					// Drop down menu (we must dispose ourselves)
	protected IEditorPart aJavaEditor;	// For handling selection in the Editor
	protected List<IJavaElement> selectedJavaElements = // Currently selected items 
		new ArrayList<IJavaElement>(); 
	
	protected IConcernModelProvider concernModelProvider; // Concern model we are dealing with
	
	public MultiElementAction()
	{
		// We want to be notified when the active concern model changes
		ConcernModelFactory.singleton().addListener(this);

		concernModelProvider = ConcernModelFactory.singleton();

		// We want to be notified when any concerns or assignments are
		// changed in the active concern model
		concernModelProvider.getModel().addListener(this);
	}

	//-----------------------------------------------------
	// IActionDelegate implementation
	//-----------------------------------------------------
    
	// Never called because the action becomes a menu.
	public void run(IAction pAction)
	{ }

	// Keeps track of selection changes in Package Explorer, Editor,
	// etc.
	public void selectionChanged(IAction pAction, ISelection pSelection)
	{
		pAction.setMenuCreator(this);
		
		getSelectedJavaElements(aJavaEditor, pSelection, selectedJavaElements);

		// Force the menus to be redrawn since we enable/disable
		// concern menu items based on the selected element
		refresh(null);
	}

	//-----------------------------------------------------
	// IEditorActionDelegate implementation
	//-----------------------------------------------------
	
	public void setActiveEditor(IAction action, IEditorPart targetEditor)
	{
		// Keep track of the editor so we can capture selection changes
		aJavaEditor = targetEditor;
	}

	//-----------------------------------------------------
	// IMenuCreator implementation
	//-----------------------------------------------------
	
	public void dispose()
	{
		if (menu != null && !menu.isDisposed())
		{
			menu.dispose();
			menu = null;
		}
	}

	public Menu getMenu(Control parent)
	{
		return null;
	}
	
	public Menu getMenu(Menu parent)
	{
		dispose();

		menu = new Menu(parent);
		fillMenu(menu, concernModelProvider.getModel().getRoot().getChildren());
		return menu;
	}

	//-----------------------------------------------------
	// ConcernModelChangeListener implementation
	//-----------------------------------------------------

	/*
	 * Refresh menu when concerns and assignments change
	 */
	@Override
	public void modelChanged(ConcernEvent events)
	{
		if (events.isChangedDomainName())
			return;
		
		if (events.isChangedActiveConcernModel())
		{
			concernModelProvider.getModel().removeListener(this);
			concernModelProvider = ConcernModelFactory.singleton();

			// We want to be notified when any concerns or assignments are
			// changed in the active concern model
			concernModelProvider.getModel().addListener(this);
		}

		boolean hasAssignOrUnassign = false;
		
		for(ConcernEvent event : events)
		{
			if (event.isAssign() || event.isUnassign())
			{
				hasAssignOrUnassign = true;
				break;
			}
		}

		if (hasAssignOrUnassign)
			Display.getDefault().asyncExec(new UpdateDropDownMenusRunner(events));
	}
	
	//-----------------------------------------------------
	// HELPER METHODS
	//-----------------------------------------------------

	private void refresh(ConcernEvent events)
	{
		if (menu != null)
		{
			fillMenu(menu, concernModelProvider.getModel().getRoot().getChildren());
		}
	}
	
	public static boolean getSelectedJavaElements(	IWorkbenchPart workbenchPart,
	                                              	ISelection selection,
	                                              	Collection<IJavaElement> selectedJavaElements)
	{
		selectedJavaElements.clear();
		
		if (selection == null)
			return false;
		
		IStructuredSelection structuredSelection = getStructuredSelection(
				selection, workbenchPart);
		if (structuredSelection == null)
			return false;

		boolean isEditor = workbenchPart instanceof JavaEditor;
		
		Iterator lI = structuredSelection.iterator();
		while (lI.hasNext())
		{
			Object selectionObj = lI.next();
			if (!(selectionObj instanceof IJavaElement))
			{
				selectedJavaElements.clear();
				return true; // User selected something that isn't a java element
			}

			IJavaElement element = (IJavaElement) selectionObj;
			if (!Component.isJavaElementAssignable(element))
			{
				selectedJavaElements.clear();
				return false; // We only support methods, fields, and types
			}
			
			selectedJavaElements.add(element);
		}
		
		if (isEditor && selectedJavaElements.isEmpty())
			return true;	// Ignore this selection
		else
			return false;	// Don't ignore the selection (may be empty)
	}
	
	private static IStructuredSelection getStructuredSelection(	ISelection selection,
	                                                            IWorkbenchPart workbenchPart)
	{
		if (selection == null)
			return null;
		
		IStructuredSelection structuredSelection = null;

		if (selection instanceof IStructuredSelection)
		{
			structuredSelection = (IStructuredSelection) selection;
		}
		else if (workbenchPart != null)
		{
			try
			{
				structuredSelection = SelectionConverter.getStructuredSelection(workbenchPart);
			}
			catch (JavaModelException e)
			{
				return null;
			}
		}

		return structuredSelection;
	}

	// Populate the dynamic menu
	abstract protected void fillMenu(Menu parent, List<Concern> concerns);

	protected static boolean isAssigned(Concern concern, 
	                                    List<IJavaElement> javaElements,
	                                    EdgeKind concernComponentRelation)
	{
		// See if any of the selected elements are already
		// assigned to the concern
		for(IJavaElement javaElement : javaElements)
		{
			if (concern.isAssigned(javaElement,
					concernComponentRelation))
			{
				return true;
			}
		}
		
		return false;
	}
	
	protected static String getConcernNameWithMnemonic(	Concern concern, 
														Set<Character> mnemonicsAlreadyUsed)
	{
		String concernName = concern.getDisplayName();
		assert concernName != null && !concernName.isEmpty();

		if (concernName.indexOf('&') >= 0)
			return concernName;
		
		char[] buf = concernName.toCharArray(); 

		char[] newBuf = new char[buf.length + 1]; // Add one for the ampersand

		for(int i = 0, newIndex = 0; i < buf.length; ++i, ++newIndex)
		{
			char c = buf[i];
			
			if ((newIndex == i) &&
				Character.isLetter(c) &&
				mnemonicsAlreadyUsed.add(c)) // Returns true if mnemonic doesn't exist
			{
				newBuf[newIndex++] = '&';
			}
			
			newBuf[newIndex] = c;
		}
		
		return new String(newBuf);
	}
	
	protected static String getNewConcernMenuItemText()
	{
		return ConcernTagger.getResourceString("actions.EditorAssignAction.NewConcern");
	}
	
	private final class UpdateDropDownMenusRunner implements Runnable
	{
		ConcernEvent events;
		
		public UpdateDropDownMenusRunner(ConcernEvent events)
		{
			this.events = events;
		}
		
		@Override
		public void run()
		{
			refresh(events);
		}
	}
}
