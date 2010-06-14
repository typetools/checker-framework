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
 */
public class ClearMarkersAction implements IObjectActionDelegate {

    /** The current selection. */
    private IStructuredSelection currSelection;
    private final MarkerCleaner cleaner = new MarkerCleaner();

    @Override
    public final void setActivePart(final IAction action,
            final IWorkbenchPart targetPart) {
        // noop
    }

    @Override
    public final void selectionChanged(final IAction action,
            final ISelection selection) {
        if (selection instanceof IStructuredSelection)
            currSelection = (IStructuredSelection) selection;
        else
            currSelection = null;
    }

    /**
     * Clear the markers on each project in the given selection, displaying a
     * progress monitor.
     * 
     */
    @Override
    public final void run(final IAction action) {
        if (currSelection == null || currSelection.isEmpty())
            return;

        try {
            ProgressMonitorDialog progress = new ProgressMonitorDialog(
                    Activator.getShell());
            progress.run(true, true, cleaner);
        } catch (InvocationTargetException e) {
            Activator.logException(e,
                    "InvocationTargetException on clear markers");
        } catch (InterruptedException e) {
            Activator.logException(e, "InterruptedException on clear markers");
        }
    }

    private class MarkerCleaner implements IRunnableWithProgress {
        @Override
        public void run(IProgressMonitor pm) throws InvocationTargetException {
            try {
                @SuppressWarnings("unchecked")
                Iterator<IAdaptable> iter = currSelection.iterator();
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
