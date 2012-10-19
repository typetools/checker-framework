package checkers.eclipse.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import checkers.nullness.quals.Nullable;

public abstract class CheckerHandler extends AbstractHandler
{
    protected @Nullable
    IJavaElement element(ISelection selection)
    {
        if (selection instanceof IStructuredSelection)
        {
            IStructuredSelection structuredSelection = (IStructuredSelection) selection;
            if (structuredSelection != null && !structuredSelection.isEmpty())
            {
                Object elem = structuredSelection.getFirstElement();
                if (elem instanceof IJavaElement)
                {
                    return (IJavaElement) structuredSelection.getFirstElement();
                }
            }
        }

        return null;
    }

    /**
     * Retrieve the selection from the menu or otherwise when called from
     * elsewhere
     * 
     * @param event
     * @return the current selection
     */
    protected ISelection getSelection(ExecutionEvent event)
    {
        ISelection selection = HandlerUtil.getActiveMenuSelection(event);

        /* use the current selection when not called from popup menu */
        if (selection == null)
            selection = HandlerUtil.getCurrentSelection(event);

        return selection;
    }
}
