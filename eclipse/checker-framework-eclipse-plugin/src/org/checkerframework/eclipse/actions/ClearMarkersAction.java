package org.checkerframework.eclipse.actions;

import static org.checkerframework.eclipse.util.JavaUtils.iterable;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import org.checkerframework.eclipse.CheckerPlugin;
import org.checkerframework.eclipse.util.MarkerUtil;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

/** Remove all bug markers for the currently selected project. */
public class ClearMarkersAction extends CheckerHandler {
    /** The current selection. */
    private IStructuredSelection currSelection;

    private final MarkerCleaner cleaner = new MarkerCleaner();

    /** Clear the markers on each project in the given selection, displaying a progress monitor. */
    public Object execute(ExecutionEvent event) {
        ISelection selection = getSelection(event);

        if (selection instanceof IStructuredSelection)
            currSelection = (IStructuredSelection) selection;
        else currSelection = null;

        if (currSelection == null || currSelection.isEmpty()) return null;

        try {
            ProgressMonitorDialog progress = new ProgressMonitorDialog(CheckerPlugin.getShell());
            progress.run(true, true, cleaner);
        } catch (InvocationTargetException e) {
            CheckerPlugin.logException(e, "InvocationTargetException on clear markers");
        } catch (InterruptedException e) {
            CheckerPlugin.logException(e, "InterruptedException on clear markers");
        }

        return null;
    }

    private class MarkerCleaner implements IRunnableWithProgress {
        @Override
        public void run(IProgressMonitor pm) throws InvocationTargetException {
            try {
                @SuppressWarnings("unchecked")
                Iterator<IAdaptable> iter = currSelection.iterator();
                for (IAdaptable adaptable : iterable(iter)) {
                    IResource resource = (IResource) adaptable.getAdapter(IResource.class);

                    if (resource == null) continue;

                    pm.subTask("Clearing Checker Framework markers from " + resource.getName());
                    MarkerUtil.removeMarkers(resource);
                }
            } catch (CoreException ex) {
                CheckerPlugin.logException(ex, "CoreException on clear markers");
                throw new InvocationTargetException(ex);
            }
        }
    }
}
