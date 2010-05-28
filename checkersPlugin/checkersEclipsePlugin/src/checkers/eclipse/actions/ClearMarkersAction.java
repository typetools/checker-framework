package checkers.eclipse.actions;

import static checkers.eclipse.util.JavaUtils.*;

import java.lang.reflect.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.*;

import checkers.eclipse.*;
import checkers.eclipse.util.*;

/**
 * Remove all bug markers for the currently selected project.
 * 
 * XXX copied from FindBugs.
 */
public class ClearMarkersAction implements IObjectActionDelegate {

    /** The current selection. */
    private ISelection currSelection;

    @Override
    public final void setActivePart(final IAction action,
            final IWorkbenchPart targetPart) {
        // noop
    }

    @Override
    public final void selectionChanged(final IAction action,
            final ISelection selection) {
        this.currSelection = selection;
    }

    @Override
    public final void run(final IAction action) {
        if (currSelection instanceof IStructuredSelection)
            work((IStructuredSelection) currSelection);
    }

    /**
     * Clear the markers on each project in the given selection, displaying a
     * progress monitor.
     * 
     * @param selection
     */
    private void work(final IStructuredSelection selection) {
        if (selection.isEmpty())
            return;

        try {
            IRunnableWithProgress r = new MarkerCleaner(selection);
            ProgressMonitorDialog progress = new ProgressMonitorDialog(
                    Activator.getShell());
            progress.run(true, true, r);
        } catch (InvocationTargetException e) {
            Activator.logException(e,
                    "InvocationTargetException on clear markers");
        } catch (InterruptedException e) {
            Activator.logException(e, "InterruptedException on clear markers");
        }
    }

    private static class MarkerCleaner implements IRunnableWithProgress {
        private final IStructuredSelection selection;

        public MarkerCleaner(IStructuredSelection selection) {
            this.selection = selection;
        }

        @Override
        public void run(IProgressMonitor pm) throws InvocationTargetException {
            try {
                @SuppressWarnings("unchecked")
                Iterator<IAdaptable> iter = selection.iterator();
                for (IAdaptable adaptable : iterable(iter)) {
                    IResource resource = (IResource) adaptable
                            .getAdapter(IResource.class);

                    if (resource == null)
                        continue;

                    pm.subTask("Clearing JSR 308 markers from "
                            + resource.getName());
                    MarkerUtil.removeMarkers(resource);
                }
            } catch (CoreException ex) {
                Activator.logException(ex, "CoreException on clear markers");
                throw new InvocationTargetException(ex);
            }
        }
    }

}
